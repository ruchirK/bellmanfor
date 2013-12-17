Networks Assignment 3 
Ruchir Khaitan 

In this assignment I used 3 classes:

bfclient - manages the three threads and listens to users, periodically checks for timeouts, and and and listens to to incoming packets with an listening socket 

RoutingTable - represents the routing table, generates and updates the distance vector as a byte array, updates it based on incoming messages, and scans an arraylist of class neighbors for timeouts, status etc 

Inter node message format

1 byte represents type of message

1 - route update
2 - user linkdown 
3 - initial linkup
4 - user linkup 

then sender port encoded as int, 

then in cases 1 and 3 (routeupdates) I use a list of destination ips, destination ports, and destination costs, encoded as InetAddress, int, double, and stored in a byte array to be sent as a packet

the maxsize of the byte array is 512 bytes as I don't want UDP packets to get too large. 

My Neighbor class contains a representation for all nodes in dv, and for those that are direct neighbors - it stores a socket for for each direct neighbor
to send directly to it.

Also, my code has at least one potential bug where to make sure that it doesnt add its own address to the distance vectors and so it compares listed portnum and ip against its stored portnum and InetAddress.getLocalHost() which doesn't work in all environments. 

Finally, I wasn't able to test many complex cases because of threading limitations, buy it works in basic use scenarios.
