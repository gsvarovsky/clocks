[![Build Status](https://travis-ci.org/gsvarovsky/clocks.svg?branch=master)](https://travis-ci.org/gsvarovsky/clocks)
![stability-wip](https://img.shields.io/badge/stability-work_in_progress-lightgrey.svg)

# clocks
_ordered message delivery_

Foundational classes for establishing ordered delivery of messages to concurrent processes, with Vector Clock and Tree Clock implementations and process examples.

**[Feedback](https://github.com/gsvarovsky/clocks/issues) and contributions welcome!**

## usage
The main building block is a [MessageService](src/main/java/org/m_ld/clocks/MessageService.java), which is intended to be interposed between an application process (such as an Actor or Verticle) and the message transport (such as a realtime channel, websocket, or queue consumer).

The `MessageService` maintains an internal _clock_ state to determine the correct delivery order for incoming messages. It relies on the process to call its `send` and `receive` methods when sending or receiving a message from the transport; in doing so, the clock is suitably updated, and the message payload can be wrapped up or unwrapped and delivered, respectively.

An [example process](src/main/java/org/example/CausalCrdtProcess.java) is provided, as a generic Java class for the update of a CRDT requiring causal delivery, and further subclassed in a very naive fashion to implement an [OR-Set](src/main/java/org/example/OrSetProcess.java) (Observed-Removed-Set).

Two `MessageService` implementations are provided: a [Vector Clock service](src/main/java/org/m_ld/clocks/vector/VectorClockMessageService.java) and a [Tree Clock service](src/main/java/org/m_ld/clocks/tree/TreeClockMessageService.java), which is more efficient for dynamic systems.

## biblio
### vector clocks
* Concurrent and Distributed Systems _Process groups and message ordering_, University of Cambridge Computer Laboratory Course material 2009–10, https://www.cl.cam.ac.uk/teaching/0910/ConcDistS/10b-ProcGp-order.pdf

### interval tree clocks
* Paulo Sergio Almeida, Carlos Baquero, Victor Fonte, _Interval Tree Clocks_, http://gsd.di.uminho.pt/members/cbm/ps/itc2008.pdf

### other implementations
* GitHub, _Voldemort is a distributed key-value storage system_, https://github.com/voldemort/voldemort/blob/master/src/java/voldemort/versioning/VectorClock.java
* GitHub, _A ShiViz-compatible logging library for Java_, https://github.com/DistributedClocks/JVector/blob/master/jvec/org/github/com/jvec/vclock/VClock.java
* GitHub, _Java implementation of a multicast-chat_, https://github.com/matteotiziano/multicast-chat/blob/master/src/model/VectorClock.java
* GitHub, _An Implementation of Interval Tree Clock_, https://github.com/sinabz/itc4j
* GitHub, _A Logical Clock for Static and Dynamic Systems_, https://github.com/ricardobcl/Interval-Tree-Clocks
