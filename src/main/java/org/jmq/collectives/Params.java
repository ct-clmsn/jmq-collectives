//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.lang.String;
import java.util.Vector;
import java.util.Map;

public class Params {

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
