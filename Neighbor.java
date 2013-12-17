import java.io.*;
import java.net.*;

public class Neighbor {

    private DatagramSocket sendSocket = null; 
    private int destPort = 0;
    private InetAddress destIP = null;
    private boolean active = false;  //indicates direct link 
    private double linkCost = 10000;
    private double cost = 1000000;
    private boolean directNeighbor = false;
    private InetAddress firstHop = null;
    private int firstHopPort = 0;
    private long lastChecked;


    //Adds direct neighbor from command line 
    public Neighbor (String ipAddr, String portNum, String destCost) throws UnknownHostException, SocketException{
        this.destPort = Integer.parseInt(portNum);
        this.linkCost = Double.parseDouble(destCost);
        this.destIP = InetAddress.getByName(ipAddr);
        this.directNeighbor = true; 
        this.firstHop = destIP;
        this.firstHopPort = destPort;
        this.active = true;
        this.sendSocket = new DatagramSocket();
        this.lastChecked = System.currentTimeMillis();
        
    }
    public Neighbor (InetAddress ipAddr, int portNum, double destCost) throws UnknownHostException, SocketException{
        this.destPort = portNum;
        this.linkCost = destCost;
        this.destIP = ipAddr;
        this.directNeighbor = true; 
        this.firstHop = destIP;
        this.firstHopPort = destPort;
        this.active = true;
        this.sendSocket = new DatagramSocket();
        this.lastChecked = System.currentTimeMillis();
        
    }
    
    public Neighbor (String ipAddr, String portNum, InetAddress firstLink, int linkPort) throws UnknownHostException {
        this.destPort = Integer.parseInt(portNum);
        //this.destCost = Double.parseDouble(destCost);
        this.destIP = InetAddress.getByName(ipAddr); 
        this.firstHop = firstLink;
        this.firstHopPort = linkPort;
        
        //sendSocket = new DatagramSocket();
        
    }
    public Neighbor(InetAddress destAddress, InetAddress linkAddr, int portNum, double mycost){
        this.destPort = portNum;
        this.destIP = destAddress;
        this.firstHop = linkAddr;
        this.firstHopPort = portNum;
        this.cost = mycost;
    }
    
    public void sendToNeighbor (byte[] distVector) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(distVector, distVector.length, destIP, destPort);
            sendSocket.send(sendPacket);
            
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean isNeighbor() {
        return directNeighbor;
    }
    
    public void setLinkCost (double newCost){
       if (active){
           this.linkCost = newCost;
           return;
       }
       this.cost = newCost;
    }
    
    public void printNeighbor() {
        if(active){
        System.out.format("Destination = %s:%d, Cost = %f, Link = (%s:%d)%n", destIP.getHostAddress(), destPort, linkCost, firstHop.getHostAddress(), firstHopPort);
        return;
        }
        
        System.out.format("Destination = %s:%d, Cost = %f, Link = (%s:%d) timed out%n", destIP.getHostAddress(), destPort, cost, firstHop.getHostAddress(), firstHopPort);
        
    }
        
    public byte[] getDestByteArray() {
            return destIP.getAddress();
    }
    
    public int getDestPort(){
            return destPort;
    }
    
    public double getLinkCost() {
        if(active) {
            return linkCost;
        }
        return cost;
    }
    
    public boolean equalsNeighbor(InetAddress addr, int port){
        if(this.destPort == port && this.destIP.equals(addr)) {
            return true;
        }
        
        return false;
        
    }
    
    public void setActive(boolean val) {
        this.active = val;
    }
    
    public void handleTimeout(long timeout, long currTime) {
        long duration = currTime - lastChecked;
        if(duration > (3 * timeout)) {
            this.setActive(false);
	    System.out.format("The node %d%n", destPort);
        }
        
    }
    
    public void setTime() {
        this.lastChecked = System.currentTimeMillis();
    } 
    
    
    
}


    
    
            
            
        


