<!-- Copyright (c) 2021 Christopher Taylor                                          -->
<!--                                                                                -->
<!--   Distributed under the Boost Software License, Version 1.0. (See accompanying -->
<!--   file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)        -->

To compile this library type 'mvn package' from project root directory.

To compile the demo program 'AppTest' follow these instructions:

* cp $HOME/.m2/repository/org/zeromq/jeromq/0.5.3-SNAPSHOT/jeromq-0.5.3-SNAPSHOT.jar into src/test/java/org/jmq/collectives/
* cp target/jmq-collectives-1.0-SNAPSHOT.jar into src/test/java/org/jmq/collectives/
* cd src/test/java/org/jmq/collectives/
* CLASSPATH=$HOME/git/jmq-collectives/target/ javac -classpath /home/ct/git/jmq-collectives/target/jmq-collectives-1.0-SNAPSHOT.jar AppTest.java                                                                     
To run the demo program 'App Test'

* Run the following commands separate terminals:
```
CLASSPATH=.:$CLASSPATH:$HOME/git/jmq-collectives/target/classes/ JMQ_COLLECTIVES_NRANKS=2 JMQ_COLLECTIVES_RANK=0 JMQ_COLLECTIVES_IPADDRESSES=127.0.0.1:5555,127.0.0.1:5556 java -cp .:./jeromq-0.5.3-SNAPSHOT.jar:./jmq-collectives-1.0-SNAPSHOT.jar AppTest 

CLASSPATH=.:$CLASSPATH:$HOME/git/jmq-collectives/target/classes/ JMQ_COLLECTIVES_NRANKS=2 JMQ_COLLECTIVES_RANK=1 JMQ_COLLECTIVES_IPADDRESSES=127.0.0.1:5555,127.0.0.1:5556 java -cp .:./jeromq-0.5.3-SNAPSHOT.jar:./jmq-collectives-1.0-SNAPSHOT.jar AppTest 
```
