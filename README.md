<!-- Copyright (c) 2021 Christopher Taylor                                          -->
<!--                                                                                -->
<!--   Distributed under the Boost Software License, Version 1.0. (See accompanying -->
<!--   file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)        -->

# [jmq-collectives](https://github.com/ct-clmsn/jmq-collectives)

This library implements a [SPMD](https://en.m.wikipedia.org/wiki/SPMD) (single program
multiple data) model and collective communication algorithms (Robert van de Geijn's
Binomial Tree) in Java using [JeroMQ](https://github.com/zeromq/jeromq). The library provides log2(N)
algorithmic performance for each collective operation over N compute hosts.

Collective communication algorithms are used in HPC (high performance computing) / Supercomputing
libraries and runtime systems such as [MPI](https://www.open-mpi.org) and [OpenSHMEM](http://openshmem.org).

Documentation for this library can be found on it's [wiki](https://github.com/ct-clmsn/jmq-collectives/wiki).
Wikipedia has a nice summary about collectives and SPMD programming [here](https://en.wikipedia.org/wiki/Collective_operation).

### Algorithms Implemented

* [Broadcast](https://en.wikipedia.org/wiki/Broadcast_(parallel_pattern))
* [Reduction](https://en.wikipedia.org/wiki/Broadcast_(parallel_pattern))
* [Scatter](https://en.wikipedia.org/wiki/Collective_operation#Scatter_[9])
* [Gather](https://en.wikipedia.org/wiki/Collective_operation#Gather_[8])
* [Barrier](https://en.wikipedia.org/wiki/Barrier_(computer_science))

### Configuring Distributed Program Execution

This library requires the use of environment variables
to configure distributed runs of SPMD applications.

Users are required to supply each of the following environment
variables to correctly run programs:

* JMQ_COLLECTIVES_NRANKS
* JMQ_COLLECTIVES_RANK
* JMQ_COLLECTIVES_IPADDRESSES

JMQ_COLLECTIVES_NRANKS - unsigned integer value indicating
how many processes (instances or copies of the program)
are running.

JMQ_COLLECTIVES_RANK - unsigned integer value indicating
the process instance this program represents. This is
analogous to a user provided thread id. The value must
be 0 or less than JMQ_COLLECTIVES_NRANKS.

JMQ_COLLECTIVES_IPADDRESSES - should contain a ',' delimited
list of ip addresses and ports. The list length should be
equal to the integer value of JMQ_COLLECTIVES_NRANKS. An
example for a 2 rank application name `app` is below:

```
# $DEVPATH is the path to jmq-collectives cloned repository

CLASSPATH=.:$CLASSPATH:$DEVPATH/jmq-collectives/target/classes/ JMQ_COLLECTIVES_NRANKS=2 JMQ_COLLECTIVES_RANK=0 JMQ_COLLECTIVES_IPADDRESSES=127.0.0.1:5555,127.0.0.1:5556 java -cp .:./jeromq-0.5.3-SNAPSHOT.jar:./jmq-collectives-1.0-SNAPSHOT.jar AppTest

CLASSPATH=.:$CLASSPATH:$DEVPATH/jmq-collectives/target/classes/ JMQ_COLLECTIVES_NRANKS=2 JMQ_COLLECTIVES_RANK=1 JMQ_COLLECTIVES_IPADDRESSES=127.0.0.1:5555,127.0.0.1:5556 java -cp .:./jeromq-0.5.3-SNAPSHOT.jar:./jmq-collectives-1.0-SNAPSHOT.jar AppTest
```

In this example, Rank 0 maps to 127.0.0.1:5555 and Rank 1
maps to 127.0.0.1:5556.

HPC batch scheduling systems like [Slurm](https://en.m.wikipedia.org/wiki/Slurm_Workload_Manager),
[TORQUE](https://en.m.wikipedia.org/wiki/TORQUE), [PBS](https://en.wikipedia.org/wiki/Portable_Batch_System),
etc. provide mechanisms to automatically define these
environment variables when jobs are submitted.

### Implementation Notes


#### Who is this library for?

Several environments the author has worked in lack MPI (OpenMPI,
MPICH, etc) installations and/or installing an MPI implementation
is not feasible for a variety of reasons (admin rules, institutional
inertia, compilers are ancient, etc).

If you are a person that works in a 'JVM Shop' (Java, Scala, etc),
needs SPMD programming, and all the options you want or need
(OpenMPI, MPICH, etc) are not available then this library is for
you.

#### What can I do with this library?

Do you work in the large scale data analysis or machine learning
problem space? Do you work on scientific computing problems? Do
you work with RDDs, Arrow, or database technologies (SQL) and want
to scale the size of the problems you'd like to solve? If you are
a cloud developer working with data stored on the Hadoop File System
(HDFS), this library combines nicely with that environment. You'll
be able to implement SPMD parallel programs, for a cluster, without
having to contort your algorithm into the Hadoop Map/Reduce programming
model.

#### What is SPMD?

SPMD (single-program many data) is a parallel programming style that
requires users to conceptualize a network of computers as a large
single machine. Multicore processors are a reduced version of this
concept. Each core on the processor in your machine talks to other
cores over a network in your processor through memory access instructions.

The SPMD programming style re-enforces the notion that processors
communicate over a network instead of through the internal
interconnect. In academic terms, the machine model or
abstraction for this enviornment is called PRAM (parallel random
access machine). SPMD style can be used when writing multithreaded
programs for this library SPMD is being used to program clusters
of machines.

It should be noted that Spark and Hadoop offer an SPMD programming
experience. The difference between this library and Spark and Hadoop
is that this library provides direct machine-to-machine communication
through message passing. The Spark and Hadoop model performs a significant
amount of communication through the file system and through files.
Spark and Hadoop also provide datatypes (files and RDDs) that play the
role of guide rails to help users write SPMD programs in the Spark and
Hadoop style. This library provides no guide rails other than a set of
collective communication primitives users can leverage to communicate
distributed data around a cluser running their SPMD application in
parallel.

#### How many processs/nodes should I deploy?

Users should make sure to deploy distributed jobs with a power of 2,
or log2(N), instances of an application developed with this library.

#### Why use 0MQ tcp protocol?

Currently a TCP/IP backend is implemented. TCP is a chatty protocol
(lots of network traffic is generated) and will have an adverse impact
on performance. That said, TCP is highly available and reliable and
JeroMQ provides support for this network protocol.

#### How scalable is it?

In the interest of providing a scalable solution, each time a communication
operation is invoked, a couple of things happen on the sender and receiver
side. For the sender: a socket is created, a connection is made, data is
transferred, a connection is closed, and the socket is closed. For the
receiver: a socket is created, a port is bound to the socket, data is
received, the socket unbinds, the socket is closed. This implementation
can be considered heavy handed due to all the operating system calls and
interactions it requires.

The reasoning for this particular implementation decision has to deal with
scalability concerns. If a sufficiently large enough number of machines are
applied to execute an application, then program initialization will take a
non-trivial amount of time - all the machines will need to create N**2
(where N is the total number of machines) sockets, socket connections, and
socket handshakes. A separate thread/process for communication would be
required along with fairly complicated queue management logic (imagine a queue
built on top of 0MQ).

Additionally, there is an overhead cost at the operating system level. Each
socket consumes a file descriptor. Operating system instances are configured
with a hard upper bound on the number of file descriptors that can be provided
to all applications running in the operating system. The popularity of using
file descriptors to manage synchronization (ie: using file descriptors as
semaphores for asynchronous function completion, etc) has created several
instances of "file descriptor leaks" or an over consumption of file descriptors
leading to program and operating system crashes.

The trade off is presented in this implementation. This library consumes 1
socket every time a communication occurs at the expense and cost of connection
initialization overhead. Program initialization is faster, the solution is
significantly more scalable (ie: no need to over-exhaust operating system
resources like file descriptors; note this is a scalability versus communication
performance trade-off!), and it creates an incentive to minimize the number of
communication events (communication means waiting longer for a solution and more
opportunities for program failure).

#### Limitations? Future Support/Features?

* This implementation uses java.util.streams.* and Java's lambda features.
Users will need a version of the jdk that supports this functionality.

* The collectives should not be called from within threads. This is a design
limitation of this library and a design limitation of JeroMQ. Collectives can
and should be called from main. (Do not fork a bunch of threads and
call collective operations!)

* This implementation is a *pure Java* implementation of SPMD collectives.
The intent is to avoid the overhead of calling out of the JVM through JNA
or JNI.

### License

Boost 1.0

### Author

Christopher Taylor

### Dependencies

* [maven](https://maven.apache.org/index.html)
* [jeromq](https://github.com/zeromq/jeromq)
* [java](https://openjdk.java.net/)
