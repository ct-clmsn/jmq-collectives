<!-- Copyright (c) 2021 Christopher Taylor                                          -->
<!--                                                                                -->
<!--   Distributed under the Boost Software License, Version 1.0. (See accompanying -->
<!--   file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)        -->

# [jmq-collectives](https://github.com/ct-clmsn/jmq-collectives)

== Description ==

This library implements Robert Van de Geijn's collective communication
algorithms using 0MQ (ZeroMQ) in Java. The algorithms are typically
implemented in MPI on HPC systems to support SPMD (Single Program
Many Data) applications. The algorithms treat each thread or process
as a node in a biomial tree.

This library allows users to apply the algorithms over 0MQ's
inproc (shared memory) backend, ipc (unix pipes) backend, and between
compute hosts over the tcp backend.

== Implementation Notes ==

This implementation uses java.util.streams.* features. Users will
need a version of the jdk that supports this functionality.

The inproc backend is provided for inter-thread and inter-process
(processes on the same compute host) communication.

The ipc backend is provided for inter-process communcations
(processes on the same compute host).

The tpc backend is provided for inter-process communications
over a network of compute hosts.

This library uses serde for data serialization (marhsalling) and
deserialization (unmarshalling).

The ZMQ_ROUTER socket types used for each 0MQ backend. This means
communication initializaton is N^2, where N is the number of
threads or processes participating in the communication. Since
the algorithms operate over a binomial tree, algorithmic performance
is log2(N).

tcp barriers use the reduction and broadcast collectives to identify
that each thread/process has reached the barrier and the root
thread/process uses the broadcast to indicate to the child
threads/processes the barrier constraint has been reached.

== Scaling Limitations ==

Since 0MQ manages communcations over file descriptors and the
GNU/Linux operating system constrains processes to ~2063 file
descriptors, users are restricted to around 1024 threads of
parallelism over the inproc backend. This scaling constraint
is due to the requirement that each thread participating in
the communication requires 2 file desciptors to send and recieve
data.

== License ==

Boost 1.0

== Author ==

Christopher Taylor

== Dependencies ==

(maven)[https://maven.apache.org/index.html]
(jeromq)[https://github.com/zeromq/jeromq]
(java)[https://openjdk.java.net/]
