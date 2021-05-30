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
import org.zeromq.ZPoller;
import org.zeromq.ZMonitor.Event;
import org.zeromq.ZMonitor.ZEvent;

public class Heartbeat implements Runnable {
    private long rank;
    private long nranks;
    private long liveness;
    private long interval;
    private long interval_init;
    private boolean halt;
    private ZMQ.Socket req;
    private ZMQ.Socket rep;
    private ZMQ.Socket data_req;
    private ZMQ.Socket data_rep;
    private ZPoller poller;
    private String uuid; 

    public Heartbeat(ZContext ctx, final String data_uuid_str, final Params p) {
        synchronized(this) {
            this.halt = false;
        }

        this.rank = p.rank();
        this.nranks = p.n_ranks();
        this.liveness = p.getLiveness();
        this.interval = p.getInterval();
        this.interval_init = p.getIntervalInit();

        this.rep = ctx.createSocket(SocketType.ROUTER);
        this.rep.setIdentity(Integer.toUnsignedString((int)this.rank).getBytes());
        this.rep.setProbeRouter(true);

        this.req = ctx.createSocket(SocketType.ROUTER);
        this.req.setIdentity(Integer.toUnsignedString((int)this.rank).getBytes());
        this.req.setProbeRouter(true);
        this.req.setLinger(1000);

        this.data_rep = ctx.createSocket(SocketType.PAIR);
        this.data_req = ctx.createSocket(SocketType.PAIR);
    	this.poller = new ZPoller(ctx);
        this.uuid = data_uuid_str;
    }

    public void initialize(final Params p) {
        final Vector<String> addresses = p.addresses();
        assert addresses.size() > this.rank;

        // data from main thread
        //
        this.data_rep.bind("inproc://" + this.uuid + "hb");

        // data to main thread
        //
        this.data_req.connect("inproc://" + this.uuid + "tcpbackend");

        final String bind_address_str = addresses.get((int)this.rank);
        this.rep.bind("tcp://" + bind_address_str);
      
        // N^2 connections to each processes
        //
        // handles ROUTER socket handshake
        //
        for(long orank = 0; orank < (long)addresses.size(); ++orank) {
            if(orank == this.rank) {
                    for(long irank = 0; irank < (long)addresses.size(); ++irank) {
                        if(irank != this.rank) {
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
        this.poller.register(this.rep, ZPoller.IN);

        // registers data from main process with poller
        //
        this.poller.register(this.data_rep, ZPoller.IN);
    }

    protected void finalize() throws Throwable {
        this.data_req.close();
        this.data_rep.close();

        this.req.close();
        this.rep.close();
    }

    public synchronized void setHalt(boolean value) {
        this.halt = value;
    }

    public synchronized boolean getHalt() { return this.halt; }

    public void run() {
        try {
            runImpl();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void runImpl() throws HeartbeatException {
        final long heartbeat_liveness = this.liveness;
        final long interval_init = this.interval_init;
        long [] liveness = new long[(int)this.nranks];
        long [] interval = new long[(int)this.nranks];
        long [] heartbeat_at = new long[(int)this.nranks];
        byte [] hb_buff = new byte[1];

        java.util.Arrays.fill(liveness, this.liveness);
        java.util.Arrays.fill(interval, this.interval_init);
        java.util.Arrays.fill(heartbeat_at, 0);

        int rank_ptr = 0;
        rank_ptr = (rank_ptr + 1) % (int)this.nranks;

        if(rank_ptr == this.rank) {
            rank_ptr = (rank_ptr + 1) % (int)this.nranks;
        }
    
        heartbeat_at[rank_ptr] = System.currentTimeMillis() + this.interval;

        boolean is_halt = this.getHalt();

        while(true) {
            if(is_halt) {
                break;
            }

            final int num_events = this.poller.poll(this.interval * 1000);

            if(num_events > 0) {
                // internal (this.data_rep)
                //
                if(this.poller.isReadable(this.data_rep)) {
                    String xmtrnkstr = this.data_rep.recvStr(0);
                    byte[] data = this.data_rep.recv(0);
                    if(xmtrnkstr.equals("-1")) { break; }

                    Message msg = new Message(data, false, Integer.valueOf(xmtrnkstr).longValue());
                    try {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
                        ObjectOutputStream out = new ObjectOutputStream(buffer);
                        out.writeObject(msg);
                        out.close();
                        byte [] databuf = buffer.toByteArray();

                        this.req.send(xmtrnkstr, ZMQ.SNDMORE);
                        this.req.send(databuf, 0);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                // external (this.rep)
                //
                if(this.poller.isReadable(this.rep)) {
                    for(int re = 0; re < num_events; ++re) { 
                        String rnkstr = this.rep.recvStr(0);
                        byte[] data = this.rep.recv(0);
                        final int rcvrank = (int)Integer.valueOf(rnkstr).longValue();

                        try {
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
                                liveness[rcvrank] = heartbeat_liveness;
                                interval[rcvrank] = interval_init;
                            }
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if(--liveness[rank_ptr] == 0) {
                    // throw exception!
                    //
               
                    throw new HeartbeatException((long)rank_ptr);
                }
            }
            else {
           
                // xmt heartbeat
                //
                if( System.currentTimeMillis() > heartbeat_at[rank_ptr] ) {
                    heartbeat_at[(int)rank_ptr] = System.currentTimeMillis() + interval[(int)rank_ptr];
                    Message msg = new Message(hb_buff, true, this.rank);

                    try {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
                        ObjectOutputStream out = new ObjectOutputStream(buffer);
                        out.writeObject(msg);
                        out.close();
                        byte [] databuf = buffer.toByteArray();

                        this.req.send(String.valueOf(rank_ptr), ZMQ.SNDMORE);
                        this.req.send(databuf, 0);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                if(--liveness[rank_ptr] == 0) {
                    // throw exception!
                    //
               
                    throw new HeartbeatException((long)rank_ptr);
                }
            }

            // increment rank_ptr
            //
            rank_ptr = (rank_ptr + 1) % (int)this.nranks;

            // if rank_ptr is 'myself' increment again
            //
            if(rank_ptr == this.rank) {
                rank_ptr = (rank_ptr + 1) % (int)this.nranks;
            }

            is_halt = this.getHalt();
        }
    }
}
