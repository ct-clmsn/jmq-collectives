<!-- Copyright (c) 2021 Christopher Taylor                                          -->
<!--                                                                                -->
<!--   Distributed under the Boost Software License, Version 1.0. (See accompanying -->
<!--   file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)        -->

# [jmq-collectives](https://github.com/ct-clmsn/jmq-collectives)

This library implements a [SPMD](https://en.m.wikipedia.org/wiki/SPMD) (single program
multiple data) model and collective communication algorithms (Robert van de Geijn's
Binomial Tree) in Rust using [JMQ](https://github.com/zeromq/jeromq). The library provides log2(N)
algorithmic performance for each collective operation over N compute hosts.

Collective communication algorithms are used in HPC (high performance computing) / Supercomputing
libraries and runtime systems such as [MPI](https://www.open-mpi.org) and [OpenSHMEM](http://openshmem.org).

Documentation for this library can be found on it's [wiki](https://github.com/ct-clmsn/jmq-collectives/wiki).

### Algorithms Implemented

* Broadcast
* Reduction
* Scatter
* Gather
* Barrier

### Configuring Distributed Program Execution

This library requires the use of environment variables
to configure distributed runs of SPMD applications.
Each of the following environment variables needs to be
supplied to correctly run programs:

* ZMQ_COLLECTIVES_NRANKS
* ZMQ_COLLECTIVES_RANK
* ZMQ_COLLECTIVES_ADDRESSES

ZMQ_COLLECTIVES_NRANKS - unsigned integer value indicating
how many processes (instances or copies of the program)
are running.

ZMQ_COLLECTIVES_RANK - unsigned integer value indicating
the process instance this program represents. This is
analogous to a user provided thread id. The value must
be 0 or less than ZMQ_COLLECTIVES_NRANKS.

ZMQ_COLLECTIVES_ADDRESSES - should contain a ',' delimited
list of ip addresses and ports. The list length should be
equal to the integer value of ZMQ_COLLECTIVES_NRANKS. An
example for a 2 rank application name `app` is below:

```
ZMQ_COLLECTIVES_NRANKS=2 ZMQ_COLLECTIVES_RANK=0 ZMQ_COLLECTIVES_ADDRESSES=127.0.0.1:5555,127.0.0.1:5556 ./app

ZMQ_COLLECTIVES_NRANKS=2 ZMQ_COLLECTIVES_RANK=1 ZMQ_COLLECTIVES_ADDRESSES=127.0.0.1:5555,127.0.0.1:5556 ./app
```

In this example, Rank 0 maps to 127.0.0.1:5555 and Rank 1
maps to 127.0.0.1:5556.

HPC batch scheduling systems like [Slurm](https://en.m.wikipedia.org/wiki/Slurm_Workload_Manager),
[TORQUE](https://en.m.wikipedia.org/wiki/TORQUE), [PBS](https://en.wikipedia.org/wiki/Portable_Batch_System),
etc. provide mechanisms to automatically define these
environment variables when jobs are submitted.

### Implementation Notes

This implementation uses java.util.streams.* features. Users will
need a version of the jdk that supports this functionality.

== License ==

Boost 1.0

== Author ==

Christopher Taylor

== Dependencies ==

(maven)[https://maven.apache.org/index.html]
(jeromq)[https://github.com/zeromq/jeromq]
(java)[https://openjdk.java.net/]
