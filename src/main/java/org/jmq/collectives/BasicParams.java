//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.lang.String;
import java.util.Vector;
import java.util.Map;

public class BasicParams extends Params {

    private long liveness;
    private long interval;
    private long interval_init;
    private long interval_max;

    public BasicParams() {
        super();

        final Map<String, String> env = System.getenv();

        if(env.containsKey("JMQ_COLLECTIVES_LIVENESS")) {
            this.liveness = Long.parseLong(env.get("JMQ_COLLECTIVES_LIVENESS"));
        }
        else {
            this.liveness = 3;
        }

        if(env.containsKey("JMQ_COLLECTIVES_INTERVAL")) {
            this.interval = Long.parseLong(env.get("JMQ_COLLECTIVES_INTERVAL"));
        }
        else {
            this.interval = 1000;
        }

        if(env.containsKey("JMQ_COLLECTIVES_INTERVAL_INIT")) {
            this.interval_init = Long.parseLong(env.get("JMQ_COLLECTIVES_INTERVAL_INIT"));
        }
        else {
            this.interval_init = 32000;
        }
    }

    public long getLiveness() { return this.liveness; }

    public long getInterval() { return this.interval; }

    public long getIntervalInit() { return this.interval_init; }
}
