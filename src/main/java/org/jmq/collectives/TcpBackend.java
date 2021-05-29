//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.lang.String;
import java.lang.Math;
import java.util.Vector;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import org.zeromq.ZMonitor;
import org.zeromq.ZMonitor.Event;
import org.zeromq.ZMonitor.ZEvent;

public class Message {
    private boolean is_heartbeat;
    private long rank;
    private byte [] bytes;

    public Message(final byte [] buf, final boolean is_heartbeat, final long rank) {
        this.isHeartbeat = is_heartbeat;
        this.rank = rank;
        this.bytes = buf;
    }

    public boolean isHeartbeat() { return this.is_heartbeat; }
    public long getRank() { return this.rank; }
    public byte [] bytes() { return this.bytes; }
}

public class HeartbeatException extends Exception {
    HeartbeatException(final String rank) {
       super("HeartbeatException\tRank lost:" + rank);
    }

    HeartbeatException(final long rank) {
       super("HeartbeatException\tRank lost:" + String.valueOf(rank));
    }
}

public class Heartbeat implements Runnable {
    private long rank;
    private long nranks;
    private long liveness;
    private long interval;
    private long interval_init;
    private long interval_max;
    private boolean halt;
    private ZMQ.Socket req;
    private ZMQ.Socket rep;
    private ZMQ.Socket data_req;
    private ZMQ.Socket data_rep;
    private ZMQ.Poller poller;
    private String uuid; 

    public Heartbeat(ZMQ.Context ctx, final String data_uuid_str, final Params p) {
        synchronized(this.halt) {
            this.halt = false;
        }

        this.rank = p.rank();
        this.nranks = p.n_ranks();
        this.liveness = p.getLiveness();
        this.interval = p.Interval()
        this.interval_init = p.getIntervalInit();
        this.interval_max = p.getIntervalMax()
        this.rep = ctx.createSocket(SocketType.ROUTER);
        this.req = ctx.createSocket(SocketType.ROUTER);
        this.data_req = ctx.createSocket(SocketType.PUSH);
        this.data_rep = ctx.createSocket(SocketType.PULL);
	this.poller = ctx.poller();
        this.uuid = data_uuid_str;
    }

    public void initialize(final Params p) {
        final Vector<String> addresses = p.addresses();
        assert addresses.size() > this.rank_;

        // data from main thread
        //
        this.data_rep.bind("inproc://" + this.uuid + "hb");

        // data to main thread
        //
        this.data_req.connect("inproc://" + this.uuid + "tcpbackend");

        final String bind_address_str = addresses.get((int)this.rank_);
        this.rep.setIdentity(Integer.toUnsignedString((int)this.rank_).getBytes());
        this.rep.setProbeRouter(true);

        // data from remote processes
        //
        this.rep.bind("tcp://" + bind_address_str);

        // data to remote processes
        this.req.setIdentity(Integer.toUnsignedString((int)this.rank_).getBytes());
        this.req.setProbeRouter(true);
       
        // N^2 connections to each processes
        //
        // handles ROUTER socket handshake
        //
        for(long orank = 0; orank < (long)addresses.size(); ++orank) {
            if(orank == this.rank_) {
                    for(long irank = 0; irank < (long)addresses.size(); ++irank) {
                        if(irank != this.rank_) {
                            this.req.connect( "tcp://" + addresses.get((int)irank) );

                            // clears the server's ZMQ_ROUTER_ID data from ZMQ_PROBE
                            //
                            this.req.recv(); 
                            this.req.recv();
                        }
                    }
            }
            else {
                this.rep.recv();
                this.rep.recv();
            }
        }

        // registers data from remote proceses with poller
        //
        this.poller.register(this.rep, ZMQ.Poller.POLLIN);

        // registers data from main process with poller
        //
        this.poller.register(this.data_rep, ZMQ.Poller.POLLIN);
    }

    protected void finalize throws Throwable() {
        this.rep.close();
        this.data_rep.close();
        this.req.close();
        this.data_req.close();
    }

    public synchronized void setHalt(boolean value) {
        this.halt = value;
    }

    public synchronized boolean getHalt() { return this.halt; }

    public run() throws HeartbeatException {
        final long heartbeat_liveness = this.liveness;
        final long interval_init = this.interval_init;
        long [] liveness = new long[this.nranks];
        long [] interval = new long [this.nranks];
        long [] heartbeat_at = long [this.nranks];
        byte [] hb_buff = new byte[1];

        java.utils.Arrays.fill(liveness, this.liveness);
        java.utils.Arrays.fill(interval_init, this.interval_init);
        java.utils.Arrays.fill(heartbeat_at, 0);

        int rank_ptr = 0;
        rank_ptr = (rank_ptr + 1) % this.nranks;

        if(rank_ptr == this.rank) {
            rank_ptr = (rank_ptr + 1) % this.nranks;
        }
    
        heartbeat_at[rank_ptr] = System.currentTimeMillis() + this.interval;

        while(true) {
            final boolean is_halt = this.getHalt();
            if(is_halt) {
                break;
            }

            this.poller.poll(this.interval * 1000);

            // internal (this.data_rep)
            //
            if(this.poller.pollin(1)) {
                String xmtrnkstr = this.data_rep.recvStr(0);
                byte[] data = this.data_rep.recv(0);

                Message msg(data, false, Integer.valueOf(xmtrnkstr).longValue());
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
                ObjectOutputStream out = new ObjectOutputStream(buffer);
                out.writeObject(msg);
                out.close();
                byte [] databuf = buffer.toByteArray();

                this.req.send(xmtrnkstr, ZMQ.SNDMORE);
                this.req.send(databuf, 0);
            }

            // external (this.rep)
            //
            if(this.poller.pollin(0)) {
                String rnkstr = this.rep.recvStr(0);
                byte[] data = this.rep.recv(0);
                final int rcvrank = (int)Integer.valueOf(rnkstr).longValue();

                ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(buffer);
                Message msg = (Message)ois.readObject();
                ois.close();
                buffer.close();

               if(msg.isHeartbeat()) {
                   liveness[rcvrank] = heartbeat_liveness;
                   interval[rcvrank] = interval_init;
               }
               else {
                   byte[] databuf = msg.bytes();
                   this.data_req.send(String.valueOf(msg.getRank()), ZMQ.SNDMORE);
                   this.data_req.send(databuf, 0);
               }
            }
            else if(--liveness[rank_ptr] == 0) {
                if(interval[rank_ptr] < interval_max) {
                       interval[rank_ptr] *= 2;
                }

                // throw exception!
                //
               
                throw new HeartbeatException((long)rank_ptr);
                //liveness[rank_ptr] = heartbeat_liveness;
            }

            // xmt heartbeat
            //
            if(System.currentTimeMillis() > heartbeat_at[rank_ptr]) {
                heartbeat_at[rank_ptr] = System.currentTimeMillis() + heartbeat_interval;
                Message msg(hb_buff, true, this.rank);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
                ObjectOutputStream out = new ObjectOutputStream(buffer);
                out.writeObject(msg);
                out.close();
                byte [] databuf = buffer.toByteArray();

                this.req.send(String.valueOf(rank_ptr), ZMQ.SNDMORE);
                this.req.send(databuf, 0);
            }

            // increment rank_ptr
            //
            rank_ptr = (rank_ptr + 1) % this.nranks;

            // if rank_ptr is 'myself' increment again
            //
            if(rank_ptr == this.rank) {
                rank_ptr = (rank_ptr + 1) % this.nranks;
            }
        }
    }
}

public class TcpBackend implements Backend, Collectives {
    private long nranks_;
    private long rank_;
    private int uuid;
    private ZContext ctx;
    private ZMQ.Socket rep;
    private ZMQ.Socket req;
    private Heartbeat hb;
    private Thread hb_thread;

    public TcpBackend(final Params p) {
        this.nranks_ = p.n_ranks();
        this.rank_ = p.rank();
        this.ctx = new ZContext();
        this.rep = this.ctx.createSocket(SocketType.PULL);
        this.req = this.ctx.createSocket(SocketType.PUSH);
        this.uuid = java.util.UUID.randomUUID().toString().hashCode();
        this.hb = null;
        this.hb_thread = null;
    }

    public void initialize(final Params p) {
        this.rep.bind("ipc://" + rep_uuid + "tcpbackend");
        final String rep_uuid = String.valueOf(this.uuid);
        this.hb = new Heartbeat(this.ctx, rep_uuid, p);
        this.hb.initialize(p);
        this.req.connect("ipc://" + rep_uuid + "hb");
        this.hb_thread = new Thread(this.hb).start();
    }

    protected void finalize throws Throwable() {
        this.hb.setHalt(true);
        this.hb_thread.join();
        this.req.close();
        this.rep.close();
        this.ctx.close();
    }

    public long n_ranks() { return this.nranks_; }
    public long rank() { return this.rank_; }

    public <Data extends java.io.Serializable> void send(final long rnk, Data data) throws IOException, ClassNotFoundException {
        assert rnk > this.nranks_;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
        ObjectOutputStream out = new ObjectOutputStream(buffer);
        out.writeObject(data);
        out.close();
        byte [] databuf = buffer.toByteArray();

        this.req.send(String.valueOf(rnk), ZMQ.SNDMORE);
        this.req.send(databuf, 0);
    }

    public <Data extends java.io.Serializable> Data recv(final long rnk) throws IOException, ClassNotFoundException {
        assert rnk > this.nranks_;

        String rnkstr = this.rep.recvStr(0);
        byte[] data = this.rep.recv(0);

        ByteArrayInputStream buffer = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(buffer);
        Data ret = (Data)ois.readObject();
        ois.close();
        buffer.close();

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

        Data local = null;

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        boolean not_sent = true; 
        long mask = 0x1;

        local = data.reduce(init, fn);

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
        java.util.function.BinaryOperator<Integer> ibo = (x1, x2) -> x1 + x2;
        this.reduce(Integer.valueOf(0), ibo, va.stream());
    }

    public <Data extends java.io.Serializable> java.util.stream.Stream<Data> scatter(java.util.Iterator<Data> data, final long data_size) throws IOException, ClassNotFoundException {

        java.util.stream.Stream<Data> out = null;
        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        final long block_size = data_size / this.nranks_;
        long k = this.nranks_ / 2;
        boolean not_received = true; 


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

        java.util.stream.Stream<Data> out = null;

        final long depth = (long)Math.ceil(Math.log(this.nranks_) / Math.log(2));
        long mask = 0x1;

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
