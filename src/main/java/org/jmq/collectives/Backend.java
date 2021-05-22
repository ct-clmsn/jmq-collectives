//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.io.IOException;
import java.lang.ClassNotFoundException;

public interface Backend {
    public void initialize(final Params p);
    public void finalize();
    public long rank();
    public long n_ranks();
    public <Data extends java.io.Serializable> void send(final long rank, Data data) throws IOException;
    public <Data extends java.io.Serializable> Data recv(final long rank) throws IOException, ClassNotFoundException;
}
