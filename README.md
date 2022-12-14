# OTP-streaming
Over the Top multimedia streaming service implemented in Java.

Run commands

For the node application
* su - core -c "cd onode/ && mvn clean package"

For the client application
* su - core -c "export DISPLAY=:0.0 && cd client/ && mvn clean package -DmyAddress=localhost -DnodeAddress=localhost"

* su - core -c "export DISPLAY=:0.0 && cd client/ && mvn clean package -DmyAddress=10.0.17.20 -DnodeAddress=10.0.17.1"

## Minimal Scenario

server: 
* su - core -c "export DISPLAY=:0.0 && cd server/ && mvn clean package -DserverID=S1 -DnodeAddress=localhost -DconfigPath=target/classes/minimal.json"

## Complete Scenario

server:
* su - core -c "export DISPLAY=:0.0 && cd server/ && mvn clean package -DserverID=S1 -DnodeAddress=localhost -DconfigPath=target/classes/complete.json"

* su - core -c "export DISPLAY=:0.0 && cd server/ && mvn clean package -DserverID=S2 -DnodeAddress=10.0.3.1"

Note: 
* Use -DmyAddress=localhost and -DnodeAddress=localhost  if the client is being executed in the same container as a node. Specifically use the string "localhost"!
