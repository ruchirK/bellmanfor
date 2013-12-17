import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.*;
import java.net.*;


public class RoutingTable {
       
       private List<Neighbor> distanceVector = Collections.synchronizedList(new ArrayList<Neighbor>());
       
       private Set<String> knownVertices = Collections.synchronizedSet(new HashSet<String>());
       private int MAXSIZE = 512;
       
       private byte[] vectorData = new byte[MAXSIZE];
       private ByteBuffer currentVector = ByteBuffer.wrap(vectorData);
       private int currentActive = 0; 
       private InetAddress myAddr;
       private int myPort;
       private long timeout;
       
       public RoutingTable(int port, long time) throws UnknownHostException {
              this.myAddr = InetAddress.getLocalHost();
              this.myPort = port;
              this.timeout = time;
       }
       
       public void addNeighbor(String ipAddr, String portNum, String destCost) throws UnknownHostException, SocketException {
               //used to read from commandLine
               Neighbor myNewNeighbor = new Neighbor(ipAddr,portNum, destCost);
               distanceVector.add(myNewNeighbor);
               //knownVertices.add(ipAddr+portNum);
               currentActive++;
               
       }
       
       public void addNewNeighbor(InetAddress ipAddr, int portNum, double destCost) throws UnknownHostException, SocketException {
               //used to read from commandLine
               Neighbor myNewNeighbor = new Neighbor(ipAddr,portNum, destCost);
               distanceVector.add(myNewNeighbor);
               //knownVertices.add(ipAddr+portNum);
               currentActive++;
               
       }
       
       public void addNewLink(InetAddress destAddress, InetAddress linkAddr, int portNum, double linkCost){
           //used to add a new connection when seen from a route update message
           Neighbor myNewLink = new Neighbor(destAddress,linkAddr, portNum,linkCost);
           distanceVector.add(myNewLink);
           currentActive++;
      }
        
       
       public void printRoutingTable() {
               //underlying logic of show-route
               long currTime = System.currentTimeMillis();
               System.out.format("<%d>Distance vector is: ", currTime);
               for (Neighbor n : distanceVector) {
                  //if (n.isActive()){
                      n.printNeighbor();
                  //}
               }
       }
       
       public void generateDistanceVector(int updateType) throws SocketException {
               //generates byte representation of current distance vector data to be sent to neighbors
               currentVector.clear();
               currentVector.put((byte) updateType);
               currentVector.putInt(myPort);
               //generates byte array to be sent to neighbors
               currentVector.putInt(currentActive);
               for (Neighbor n : distanceVector) {
                  if (!n.isNeighbor() || n.isActive()){
                      currentVector.put(n.getDestByteArray());
                      currentVector.putInt(n.getDestPort());
                      currentVector.putDouble(n.getLinkCost());
                  }
               }
               
       }
       
       public void sendToAllNeighbors() throws IOException{
           //underlying logic of route update and linkup
           for (Neighbor n : distanceVector) {
                  if (n.isNeighbor() && n.isActive()){
                      n.sendToNeighbor(vectorData);
                  }
           }
       }
       
       public void sendRouteUpdate() throws IOException {
           this.generateDistanceVector(1);
           this.sendToAllNeighbors();
       }
       
       public void initialLinkUp() throws IOException, SocketException {
           this.generateDistanceVector(3);
           this.sendToAllNeighbors();
       }
       
       public void handleReceivedRouteUpdate(byte[] recvData, InetAddress senderAddress) throws UnknownHostException {
              //assumes that a distance vector and parses it updating its own data using Bellman Ford
              ByteBuffer recvBuffer = ByteBuffer.wrap(recvData);
              Byte check = recvBuffer.get();
              if (check != ((byte) 1) || check != ((byte) 3)){
                  return;
              }
              int senderPort = recvBuffer.getInt();
              //first determine who sent it and their link cost to you
              double senderLinkCost = 0;
              for (Neighbor n : distanceVector) {
                  if (n.equalsNeighbor(senderAddress, senderPort)) {
                      //update n's last heard from value
                      n.setActive(true);
                      n.setTime();
                      senderLinkCost = n.getLinkCost();
                  }
              }
              if (senderLinkCost == 0 ){
                 //we couldn't find matching sender 
                 //error case
                 return;
              }
              byte[] linkAddr = new byte[4];
              InetAddress linkAddress;
              int linkPort = 0;
              boolean foundLink = false;
              double linkCost;
              int numLinks = recvBuffer.getInt();
              for(int i = 0; i< numLinks; i++) {
                  foundLink = false;
                  recvBuffer.get(linkAddr, 0, 4); 
                  linkAddress = InetAddress.getByAddress(linkAddr);
                  linkPort = recvBuffer.getInt();
                  linkCost = recvBuffer.getDouble();
                  //use hashset to check contains first instead
                  for (Neighbor n : distanceVector) {
                      if (n.equalsNeighbor(linkAddress, linkPort)) {
                          foundLink = true;
                          if(/*n.isActive() && */ n.getLinkCost() > (senderLinkCost + linkCost)){
                              n.setLinkCost(senderLinkCost+linkCost);
                          }
                      }
                  }
                  
                  if(!foundLink && !isMyAddress(linkAddress, linkPort)) {
                      //encountered a new node
                      this.addNewLink(linkAddress, senderAddress, linkPort, (senderLinkCost + linkCost));
                  }
              }
              
       }
       
       public int modifyLink(InetAddress destAddr, int destPort, boolean value) {
           //logical link down, applies to user inputted link downs, received link down messages, and timeouts
           for (Neighbor n : distanceVector) {
                  if (n.isNeighbor() && n.equalsNeighbor(destAddr, destPort) ) {
                        
                        n.setActive(value);
                  }
           }
           if(value){
               currentActive++;
               return currentActive;
           }
           
           currentActive--;
           return currentActive;
       }
       
       public void userLinkCommand(String destAddress, String destPortNum, boolean value) throws IOException, UnknownHostException {
           //handle user input case of linkdown
           InetAddress destAddr = InetAddress.getByName(destAddress);
           int destPort = Integer.parseInt(destPortNum);
           int currActive = this.currentActive;
           int result = this.modifyLink(destAddr, destPort, value);
           
           if(currActive != (result + 1) && !value) {
               System.out.format("Sorry could not remove %s:%s not a direct neighbor", destAddress, destPortNum);
               return;           
            }
            if(currActive != (result - 1) && value) {
               System.out.format("Sorry could not add %s:%s not previously a direct neighbor", destAddress, destPortNum);
               return;           
            }
            
            //generate link down/up message, send it to neighbor
            currentVector.clear();
            
            if(!value){
                currentVector.put((byte) 2);
            }
            else { 
                currentVector.put((byte) 4);
            }
            currentVector.putInt(myPort);
            currentVector.putInt(myPort);
            currentVector.putInt(-1);
            for (Neighbor n : distanceVector) {
                  if (n.isNeighbor() && n.equalsNeighbor(destAddr, destPort) ) {
                     n.sendToNeighbor(vectorData);
                     return;
                  }
            }
       }
       
       
        
       public void handleReceivedLinkMessage(byte[] recvData, InetAddress senderAddress) throws UnknownHostException{    
           //check that the received array is a valid linkup/linkdown 
           ByteBuffer recvBuffer = ByteBuffer.wrap(recvData);
           boolean value = false;
           if (recvBuffer.get() == (byte) 4){
                  value = true;
           }
           int senderPort = recvBuffer.getInt();
           if(senderPort != recvBuffer.getInt()){
               return;
           }
           if (recvBuffer.getInt() == -1) {
               modifyLink(senderAddress, senderPort, value);
           }
      }
      
      public void handleReceivedInitLinkUp(byte[] recvData, InetAddress senderAddress) throws UnknownHostException, SocketException { 
           ByteBuffer recvBuffer = ByteBuffer.wrap(recvData);
           if (recvBuffer.get() != (byte) 3){
                  return;
           }
           int senderPort = recvBuffer.getInt();
           for (Neighbor n : distanceVector) {
                  if (n.equalsNeighbor(senderAddress, senderPort)) {
                      //you already have this node stored 
                      handleReceivedRouteUpdate(recvData, senderAddress);
                      return;
                  }
           }
                      
           //first determine who sent it and their link cost to you
           double senderLinkCost = 0; 
           byte[] linkAddr = new byte[4];
           InetAddress linkAddress;
           int linkPort = 0;
           //boolean foundLink = false;
           int numLinks = recvBuffer.getInt();
           for(int i = 0; i< numLinks; i++) {
                  recvBuffer.get(linkAddr, 0, 4); 
                  linkAddress = InetAddress.getByAddress(linkAddr);
                  linkPort = recvBuffer.getInt();
                  senderLinkCost = recvBuffer.getDouble();
                  if(isMyAddress(linkAddress, linkPort)) {
                      this.addNewNeighbor(senderAddress, senderPort, senderLinkCost);
                      recvBuffer.rewind();
                      handleReceivedRouteUpdate(recvData, senderAddress);
                      return;
                  }
           }
      }
      
      
      public void setMyAddress(InetAddress address){
          this.myAddr = address;
      }
      
      public boolean isMyAddress(InetAddress address, int port){
             if (this.myAddr.equals(address) && this.myPort == port){
                 return true;
             }
             
             return false;
      }
      
      public void handleReceivedInputs(byte[] data, InetAddress sender) throws UnknownHostException, SocketException {
         Byte indexByte = data[0];
         if(indexByte == (byte) 1) {
             this.handleReceivedRouteUpdate(data, sender);
             return;
         }
         if(indexByte == (byte) 3) {
             this.handleReceivedInitLinkUp(data, sender);
             return;
         }
         if(indexByte == (byte) 2 || indexByte == (byte) 4){ 
             handleReceivedLinkMessage(data, sender);
         }
      }                         
         
      public void scanForNeighborTimeout(long time){
          for (Neighbor n : distanceVector) {
                  if (n.isNeighbor() && n.isActive()){
                      n.handleTimeout(timeout, time);
                  }
          }
      }    
              
                   
                  
                      
                      

}       
       
