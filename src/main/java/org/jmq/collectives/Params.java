//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.lang.String;
import java.util.Vector;
import java.util.Map;

public class BasicParams {

    private long nranks_;
    private long rank_;
    private Vector<String> addresses_;

    public Params() {
        final Map<String, String> env = System.getenv();

        if(env.containsKey("JMQ_COLLECTIVES_NRANKS")) {
            this.nranks_ = Long.parseLong(env.get("JMQ_COLLECTIVES_NRANKS"));
        }
        else {
            this.nranks_ = 0;
        }

        if(env.containsKey("JMQ_COLLECTIVES_RANK")) {
            this.rank_ = Long.parseLong(env.get("JMQ_COLLECTIVES_RANK"));
        }
        else {
            this.rank_ = 0;
        }

        this.addresses_ = new Vector<String>();
        if(env.containsKey("JMQ_COLLECTIVES_IPADDRESSES")) {
            for(String addr : env.get("JMQ_COLLECTIVES_IPADDRESSES").split(",")) {
                this.addresses_.addElement(addr);
            }
        }
    }

    public long n_ranks() { return this.nranks_; }

    public long rank() { return this.rank_; }
    
    public Vector<String> addresses() { return this.addresses_; }
}

public class Params extends BasicParams {

    private long liveness;
    private long interval;
    private long interval_init;
    private long interval_max;

    public Params() {
        super();

        final Map<String, String> env = System.getenv();

        if(env.containsKey("JMQ_COLLECTIVES_LIVENESS")) {
            this.liveness = Long.parseLong(env.get("JMQ_COLLECTIVES_LIVENESS"));
        }
        else {
            this.liveness = 0;
        }

        if(env.containsKey("JMQ_COLLECTIVES_INTERVAL")) {
            this.interval = Long.parseLong(env.get("JMQ_COLLECTIVES_INTERVAL"));
        }
        else {
            this.interval = 0;
        }

        if(env.containsKey("JMQ_COLLECTIVES_INTERVAL_MAX")) {
            this.interval_max = Long.parseLong(env.get("JMQ_COLLECTIVES_INTERVAL_MAX"));
        }
        else {
            this.interval_max = 0;
        }

        if(env.containsKey("JMQ_COLLECTIVES_INTERVAL_INIT")) {
            this.interval_init = Long.parseLong(env.get("JMQ_COLLECTIVES_INTERVAL_INIT"));
        }
        else {
            this.interval_init = 0;
        }
    }

    public long getLiveness() { return this.liveness; }

    public long getInterval() { return this.interval; }

    public long getIntervalMax() { return this.interval_max; }

    public long getIntervalInit() { return this.interval_init; }
}
