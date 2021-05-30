//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

public class HeartbeatException extends Exception {
    HeartbeatException(final String rank) {
       super("HeartbeatException\tRank lost:" + rank);
    }

    HeartbeatException(final long rank) {
       super("HeartbeatException\tRank lost:" + String.valueOf(rank));
    }
}
