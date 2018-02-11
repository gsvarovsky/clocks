[![Build Status](https://travis-ci.org/gsvarovsky/clocks.svg?branch=master)](https://travis-ci.org/gsvarovsky/clocks)

# clocks
_ordered message delivery_

Foundational classes for establishing ordered delivery of messages to concurrent processes, with Vector Clock and Interval Tree Clock implementations and process examples.

**[Feedback](https://github.com/gsvarovsky/clocks/issues) and contributions welcome!**

## biblio
### vector clocks
* Concurrent and Distributed Systems _Process groups and message ordering_, University of Cambridge Computer Laboratory Course material 2009–10, https://www.cl.cam.ac.uk/teaching/0910/ConcDistS/10b-ProcGp-order.pdf

### interval tree clocks
* Paulo Sergio Almeida, Carlos Baquero, Victor Fonte, _Interval Tree Clocks_, http://gsd.di.uminho.pt/members/cbm/ps/itc2008.pdf

### other implementations
* GitHub, _Voldemort is a distributed key-value storage system_, https://github.com/voldemort/voldemort/blob/master/src/java/voldemort/versioning/VectorClock.java
* GitHub, _A ShiViz-compatible logging library for Java_, https://github.com/DistributedClocks/JVector/blob/master/jvec/org/github/com/jvec/vclock/VClock.java
* GitHub, _Java implementation of a multicast-chat_, https://github.com/matteotiziano/multicast-chat/blob/master/src/model/VectorClock.java