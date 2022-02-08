//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.lang.Thread;
import java.lang.String;
import java.lang.Math;
import java.util.Vector;
import java.util.Random;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;

public class BasicTcpBackend implements Backend, Collectives {
    private long nranks_;
    private long rank_;
    private Vector<String> addresses;

    public BasicTcpBackend(final Params p) {
        assert p.addresses().size() != p.n_ranks();
        this.nranks_ = p.n_ranks();
        this.rank_ = p.rank();
        this.addresses = p.addresses();
    }

    public void initialize(final Params p) {}
    public void finalize() {}

    public long n_ranks() { return this.nranks_; }
    public long rank() { return this.rank_; }

    public <Data extends java.io.Serializable> void send(final long rnk, Data data) throws IOException, ClassNotFoundException {
        assert rnk > this.nranks_;

        try( ZContext ctx = new ZContext() ) {

            ZMQ.Socket sock = ctx.createSocket(SocketType.PAIR);
            sock.setLinger(-1); // message queue must flush before socket is closed and context is closed/destroyedd

            final String addr = "tcp://" + this.addresses.get((int)rnk);

            {
                boolean connected = false;
                while(!connected) {
                    connected = sock.connect(addr);
                }
            }

            {
                boolean not_sent = false;
        
                while(!not_sent) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
                    ObjectOutputStream out = new ObjectOutputStream(buffer);
                    out.writeObject(data);
                    out.close();
                    byte [] databuf = buffer.toByteArray();
                    not_sent = sock.send(databuf, 0);
                }
            }

            sock.close();
        }
    }

    public <Data extends java.io.Serializable> Data recv(final long rnk) throws IOException, ClassNotFoundException {
        assert rnk > this.nranks_;

        Data ret = null;

        try( ZContext ctx = new ZContext() ) {
        
            final String addr = "tcp://" + this.addresses.get((int)this.rank_);
            ZMQ.Socket sock = ctx.createSocket(SocketType.PAIR);
            sock.setImmediate(true); // immediately accept messages

            {
                boolean bound = false;
                while(!bound) {
                    bound = sock.bind(addr);
                }
            }

            byte[] data = sock.recv(0);
            ByteArrayInputStream buffer = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(buffer);
            ret = (Data)ois.readObject();
            ois.close();
            buffer.close();
            sock.close();

        }

        return ret; 
    }
    
    public <Data extends java.io.Serializable> Data broadcast(Data data) throws IOException, ClassNotFoundException {

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        long k = this.nranks_ / 2;
        boolean not_recv = true; 

        for(long _d = 0; _d < depth; ++_d) {
            long twok = 2 * k;
            if ((this.rank_ % twok) == 0) {
                this.send(this.rank_+k, data);
            }
            else if( not_recv && ((this.rank_ % twok) == k) ) {
                data = this.recv(this.rank_-k);
                not_recv = false;
            }

            k >>= 1;
        }

        return data;
    }

    public <Data extends java.io.Serializable> Data reduce(final Data init, java.util.function.BinaryOperator<Data> fn, java.util.stream.Stream<Data> data) throws IOException, ClassNotFoundException {

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        boolean not_sent = true; 
        long mask = 0x1;

        Data local = data.reduce(init, fn);

        for(long _d = 0; _d < depth; ++_d) {
            if( (mask & this.rank_) == 0 ) {
                if( (mask | this.rank_) < this.nranks_ && not_sent ) {
                    Data res = this.recv(this.rank_);
                    local = fn.apply(local, res);
                }
            }
            else if(not_sent) {
                final long parent = this.rank_ & ((mask>0) ? 0 : 1);
                this.send(parent, local);
                not_sent = false;
            }

            mask <<= 1;
        }

        return local;
    }

    public void barrier() throws IOException, ClassNotFoundException {
        int v = (this.rank_ == 0) ? 1 : 0;
        this.broadcast(v);

        Vector<Integer> va = new Vector<Integer>();
        va.addElement(1);
        va.addElement(1);
        java.util.function.BinaryOperator<Integer> ibo = (x1, x2) -> x1 + x2;
        this.reduce(Integer.valueOf(0), ibo, va.stream());
    }

    public <Data extends java.io.Serializable> java.util.stream.Stream<Data> scatter(java.util.Iterator<Data> data, final long data_size) throws IOException, ClassNotFoundException {

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        final long block_size = data_size / this.nranks_;
        long k = this.nranks_ / 2;
        boolean not_received = true; 

        java.util.stream.Stream<Data> out = null;

        if( this.rank_ < 1 ) {
            final long beg = ((this.rank_ + k) % this.nranks_) * block_size;
            java.util.Spliterator<Data> sitr = java.util.Spliterators.spliteratorUnknownSize(data, 0);
            java.util.stream.Stream<Data> datastrm = java.util.stream.StreamSupport.stream(sitr, false);
            out = datastrm.limit(beg);
        }

        Vector<java.util.stream.Stream<Data>> streams = new Vector<java.util.stream.Stream<Data>>();

        for(long _d = 0; _d < depth; ++_d) {
            long twok = 2 * k;
            if( (this.rank_ % twok) == 0 ) {
                final long beg = ((this.rank_ + k) % this.nranks_) * block_size;
                final long end = (this.nranks_ - (this.rank_ % this.nranks_)) * block_size;

                java.util.Vector<Data> subdata = new java.util.Vector<Data>();
                for(long sp = beg; sp < end; ++sp) {
                   subdata.addElement( data.next() ); 
                }
                
                this.send(this.rank_+k, subdata);
            }
            else if( not_received && ((this.rank_ % twok) == k)) {
                java.util.Vector<Data> res = this.recv(this.rank_-k);
                streams.addElement(res.stream());
                not_received = false;
            }
        }

        if( this.rank_ > 0) {
            out = streams.stream().reduce(java.util.stream.Stream::concat).orElseGet(java.util.stream.Stream::empty);
        }

        return out;
    }

    public <Data extends java.io.Serializable> java.util.stream.Stream<Data> gather(java.util.Iterator<Data> data, final long data_size) throws IOException, ClassNotFoundException {

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        long mask = 0x1;

        java.util.stream.Stream<Data> out = null;
        Vector<java.util.stream.Stream<Data>> streams = new Vector<java.util.stream.Stream<Data>>();

        java.util.Vector<Data> subdata = new java.util.Vector<Data>();
        while(data.hasNext()) {
            subdata.addElement( data.next() ); 
        }

        if(this.rank_ > 0) {
            java.util.Spliterator<Data> sitr = java.util.Spliterators.spliteratorUnknownSize(subdata.iterator(), 0);
            out = java.util.stream.StreamSupport.stream(sitr, false);
        }
        else {
            java.util.Spliterator<Data> sitr = java.util.Spliterators.spliteratorUnknownSize(subdata.iterator(), 0);
            java.util.stream.Stream<Data> datastrm = java.util.stream.StreamSupport.stream(sitr, false);
            streams.addElement(datastrm);
        }

        for(long _d = 0; _d < depth; ++_d) {
            if ((mask & this.rank_) == 0) {
                final long child = this.rank_ | mask;
                if( child < this.nranks_ ) {
                    java.util.Vector<Data> res = this.recv(child);
                    streams.addElement(res.stream());
                }
            }
            else {
                final long parent = this.rank_ & ((mask>0) ? 0 : 1);
                this.send(parent, subdata);
            }

            mask <<= 1;
        }

        if( this.rank() < 1 ) {
            out = streams.stream().reduce(java.util.stream.Stream::concat).orElseGet(java.util.stream.Stream::empty);
        }

        return out;
    }
}
