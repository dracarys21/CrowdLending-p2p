package edu.nyu.cs.crowdlending.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
	private String accountNumber;
    private String ip;
    
    private int balance;
    private Map<String, Integer> moneyBorrowed;
    private Map<String,Integer> moneyLent;
    String serverIp;
    int serverPort;
    int port;
    
    ServerSocket serviceSocket;
    
    public Client(String accNo, String ipAddress, int port, int bal, String serverIp, int serverPort) throws IOException {
    	
    	this.port = port; 
    	serviceSocket = new ServerSocket(port);
       	balance = bal;
    	
    	this.accountNumber = accNo;
    	ip = ipAddress;
    	
    	this.serverIp = serverIp;
    	this.serverPort = serverPort; 
    	
    	moneyBorrowed = Collections.synchronizedMap(new HashMap<>());
    	moneyLent = Collections.synchronizedMap(new HashMap<>());
    }
    
    public String getName() {
    	return accountNumber;
    }
    
    public String getIp() {
    	return ip;
    }
    
    //register client to server
    public void register() throws UnknownHostException, IOException {
    	
    	Socket sc = new Socket(serverIp, serverPort);
    	
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        
        String registerServ = ip+":"+port;
        //send client's wish to get money
        out.println("REGISTER "+registerServ);
        System.out.println(in.nextLine());
        
        sc.close();
        out.close();
        in.close();
    }
    
  //ask server for lenders	
    public String[] getEligibleLendersFromServer(int moneyReq) throws IOException {
    	Socket sc = new Socket(serverIp, serverPort);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        
        out.println("BORROW "+moneyReq);
        
        String[] ipsToConnect = in.nextLine().split(",");
        
        return ipsToConnect;
    }
    
    //acting as a client
    public synchronized void requestMoney(String[] ipsToConnect, int moneyReq) throws IOException {
    	//connect to server to get list of lenders
    	Socket sc = null;
    	Scanner in = null ;
    	PrintWriter out = null;
    	String ownIpPort = ip+":"+port;
    	
        for(String ipsAndPort : ipsToConnect) {
        	 //get port
        	String ipAndPort[] = ipsAndPort.split(":");
        	
        	 sc = new Socket(ipAndPort[0], Integer.parseInt(ipAndPort[1]));	
        	out = new PrintWriter(sc.getOutputStream(), true);
        	in = new Scanner(sc.getInputStream());
        	 
        	 out.println("REQUEST "+ownIpPort+" "+moneyReq);
        	 String response = in.nextLine(); 

        	 if(response.equals("AWK AVAILABLE")) {
        		 balance+=moneyReq;
        		 moneyBorrowed.put(ipsAndPort, moneyReq);
        		 System.out.println("MONEY RECEIVED BY "+accountNumber);
        		 break;
        	 }
        }        
      //close connections
        if(in!=null) in.close();
        if(out!=null) out.close();
        sc.close();
    }
    
    //acting as a Server
    public  synchronized void lendMoney(String clientName) throws IOException {
    	//accepts the connection from borrower peer
    	Socket client = serviceSocket.accept();
    	
    	//read the input from client 
    	Scanner input = new Scanner(client.getInputStream());
    	PrintWriter output = new PrintWriter(client.getOutputStream(), true);

    	String msg = input.nextLine();
    	String[] parsed = Message.parseMessage(msg);
    	
    	//update balance
    	int reqMoney =  Integer.parseInt(parsed[parsed.length-1]);
    	
    	if(reqMoney<=balance) {
    		output.println("AWK AVAILABLE");
    		balance = balance-reqMoney;
    		moneyLent.put(parsed[2], reqMoney);
    	}
    	else {
    	 //send message if money not available to send
    		output.println("AWK NOT AVAILABLE");
    	
    	//close connections	
    		input.close();
            output.close();
            client.close();
            
    		return;
    	}
    	
    	//send AWK message
        output.println("AWK MONEY SENT "+clientName);
       
        //update server
        updateServer();
        
        //close connections
        input.close();
        output.close();
        client.close();
    }
    
    //acts as a client
    public synchronized void updateServer() throws UnknownHostException, IOException {
    	Socket sc = new Socket(serverIp, serverPort);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        
        String registerServ = ip+":"+port;
        //send client's wish to get money
        out.println("UPDATE "+registerServ+" "+balance);
        System.out.println(in.nextLine());
    }
    
    //acts as a client 
    public synchronized void returnMoney(String ipWithPort) throws IOException {
    	Socket sc = new Socket(serverIp, serverPort);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        
        int moneyRet = moneyBorrowed.get(ipWithPort);
         moneyBorrowed.remove(ipWithPort);
         
        balance = balance-moneyRet;
        out.println("RETURN "+ipWithPort+" "+moneyRet);
        
        System.out.println(in.nextLine());
    }
    
    //acts as a server
    public synchronized void receieveBorrowedMoney() throws IOException {
    	Socket client = serviceSocket.accept();
    	Scanner input = new Scanner(client.getInputStream());
    	PrintWriter output = new PrintWriter(client.getOutputStream(), true);
    	
    	String msg = input.nextLine();
    	
    	String[] splits = msg.split(" ");
    	int moneyReturned = Integer.parseInt(splits[2]);
    	moneyLent.put(splits[1],moneyLent.get(splits[1])-moneyReturned);
    	balance = balance - moneyReturned;
    	
    	output.println("MONEY RECIVED BY "+accountNumber);
    }
    
}
