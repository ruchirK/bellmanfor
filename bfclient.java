import java.net.*;
import java.io.*;


public class bfclient {

    private static RoutingTable myTable;
    private static int localPort;
    private static int timeout;
    private static boolean isRunning = true;
    private static int MAXSIZE = 512; 
    private static DatagramSocket listenSocket;
    
    public static void main(String[] args) { 
        readCommandLine(args);
        listenToSocket();
        manageTimeouts();
        listenToUser();
    
    
    }
    
    public static void readCommandLine(String[] args) {
    //used at startup, reads commands and neighbor data into routing table
        if(args.length < 3 || ((args.length-2)%3 != 0)) {
            System.out.println("Usage: java bfclient <localport> <timeout> <neighbor1 ip> <neighbor1 port> <neighbor1 link cost>");
            System.exit(1);
        }
        localPort = Integer.parseInt(args[0]);
        timeout = Integer.parseInt(args[1]) * 1000;
        try {
            myTable = new RoutingTable(localPort, timeout);
        
        
        
            //int num_neighbors = Integer.parseInt(args[2]);
            //if((args.length -3) != num_neighbors*3 ) {
              //  System.exit(1);
            //} 
            int argIndex = 2;
            for(int i = 0; i<(args.length-2)/3; i++){
                myTable.addNeighbor(args[argIndex], args[argIndex+1], args[argIndex + 2]);
                argIndex += 3;
             }
        }
        catch (Exception e) {
            System.out.println("Error allocating RT");
            e.printStackTrace();
            System.exit(1);
        }
        //initialize sending socket
        try {
            listenSocket = new DatagramSocket(localPort);
        }
        catch (SocketException e){
                        System.out.println("SocketError couldn't bind to port "+e );
                        System.exit(1);
        }
    }
    
    //listen for updates from other sockets - modifying your dv as a result
    //only listening
    
    
    //send out your dv on a scheduled basis so that your neighbors know you exist
    //only sending 
    
    //listen for user commands on command line, and execute as such
    //only sending
    
    public static void handleUserCommands(String input){
       try {
           //Handles logic behind reading user argumeants
           String[] parts = input.split(" |\\:");
           if(parts[0].equalsIgnoreCase("SHOWRT")) {
               myTable.printRoutingTable();
               return;
           }
           if(parts[0].equalsIgnoreCase("CLOSE")){
               //do close
               isRunning = false;
               System.exit(1);
               return;
           }
           if(parts.length != 3) {
               //invalid format
               return;
           }
           if(parts[0].equalsIgnoreCase("LINKDOWN")){
               myTable.userLinkCommand(parts[1], parts[2], false);
               return;
           }
           if(parts[0].equalsIgnoreCase("LINKUP")){
               myTable.userLinkCommand(parts[1], parts[2], true);
               return;
           }
       }
       catch (Exception e) {
            System.out.println("Error allocating handling user commanding");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void listenToUser() {
        BufferedReader input = new BufferedReader (new InputStreamReader(System.in));
        //affirm execution
        isRunning = true; 
        
        //print command list to user
        
        while(isRunning) {
            try {
                 handleUserCommands(input.readLine());
            } 
            catch (Exception e){
                System.out.println("Error occured whilst waiting on your directive master "+e);
            }
        }
        
        //stopped executing
        System.out.println("Closing time - you don't have to go home but you can't stay here");
        try {
             input.close();
        }
        catch (Exception e){
             System.out.println("Time for you to go out to the places you will be from " +e);
        }
        System.exit(0);
    }
    
    public static void listenToSocket() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                while(isRunning) {
                    byte[] data = new byte[MAXSIZE];
                    InetAddress senderAddress = null;
                    int inputSize = 0;
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    //System.out.println("Waiting in receive loop");
                    try {
                        listenSocket.receive(packet);
                        senderAddress = packet.getAddress();
                        inputSize = packet.getLength();
                        //System.out.println("Received from "+ packet.getSocketAddress());
                   }
                   catch (IOException e) {
                        System.out.println("IO Error "+e);
                   }
                   
                   byte[] recvData = new byte[inputSize];
                   System.arraycopy(packet.getData(), 0, recvData, 0, inputSize);
                   //System.out.println("Processing input now");
                   try {
                       myTable.handleReceivedInputs(recvData, senderAddress);
                   }
                   catch (Exception e) {
                      System.out.println("Error handling received inputs");
                      e.printStackTrace();
                      System.exit(1);
                   }    
                }
            }
        });
        t.start();
    }
                    
    public static void manageTimeouts(){
        Thread t = new Thread(new Runnable() {
            public void run() {
                long time = System.currentTimeMillis();
                long check = System.currentTimeMillis();
                long i = 0;
                try{
                   myTable.initialLinkUp();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                while (isRunning) {
                    if((check - time) > (timeout - 200) ){
                       try{
                           myTable.sendRouteUpdate();
                       }
                       catch (Exception e) {
                           System.out.println("Error sending route update");
                           e.printStackTrace();
                           System.exit(1);
                        }
                        myTable.scanForNeighborTimeout(check);
                        time = System.currentTimeMillis();
                        i = 0;
                    }
                    check = System.currentTimeMillis();
                    i++;
                } 
            }
        });
        t.start();
    }
    
    
        
} 

                        
                    
    
         
    
    
           
           
    
    
    
                      
    
    
