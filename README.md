# OTP-streaming
Over the Top multimedia streaming service implemented in Java.

Run commands

server: su - core -c "export DISPLAY=:0.0 && cd server/ && mvn clean package -DserverID=1 -DnodeAddress=10.0.20.10 -DbuildOverlay=true"

onode: su - core -c "cd onode/ && mvn clean package"

client: su - core -c "export DISPLAY=:0.0 && cd client/ && mvn clean package -DmyAddress=0.0.0.0 -DnodeAddress=0.0.0.0"

Note: 
* Use -DmyAddress=localhost and -DnodeAddress=localhost  if the client is being executed in the same container as a node. Specifically use the string "localhost"!
