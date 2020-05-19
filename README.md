# CrowdLending-p2p
Crowd Lending Banking System with p2p framework

## To run the application:

* First spawn the server, and subsequently the peer nodes in the network.
* To start the server:
```
javac edu/nyu/cs/crowdlending/server/Server.java
java edu/nyu/cs/crowdlending/server/Server
```
* To start a peer node:
```
javac edu/nyu/cs/crowdlending/driver/Simulator.java
java edu/nyu/cs/crowdlending/server/Simulator
```
The peer nodes require 3 arguments to start: Account number, balance amount and port number
