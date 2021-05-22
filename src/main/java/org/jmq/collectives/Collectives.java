//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.io.IOException;
import java.lang.ClassNotFoundException;

public interface Collectives {
    public <Data extends java.io.Serializable> Data broadcast(Data data) throws IOException, ClassNotFoundException;
    public <Data extends java.io.Serializable> Data reduce(final Data init, java.util.function.BinaryOperator<Data> fn, java.util.stream.Stream<Data> data) throws IOException, ClassNotFoundException;
    public void barrier() throws IOException, ClassNotFoundException;
    public <Data extends java.io.Serializable> java.util.stream.Stream<Data> scatter(java.util.Iterator<Data> data, final long data_size) throws IOException, ClassNotFoundException;
    public <Data extends java.io.Serializable> java.util.stream.Stream<Data> gather(java.util.Iterator<Data> data, final long data_size) throws IOException, ClassNotFoundException;
}
