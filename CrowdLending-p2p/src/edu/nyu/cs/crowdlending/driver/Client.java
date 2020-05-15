package edu.nyu.cs.crowdlending.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
	private String name;
    private InetAddress ip;
    private int balance;
    private Map<String, Integer> moneyBorrowed;
    private Map<String,Integer> moneyLent;
    InetAddress serverIp;
    int serverPort;
    
    ServerSocket sc;
    
    public Client(String name, InetAddress ipAddress, int port, int bal, InetAddress serverIp, int serverPort) throws IOException {
    	sc = new ServerSocket(port);
    	this.serverPort = serverPort; 
    	this.name = name;
    	ip = ipAddress;
    	balance = bal;
    	moneyBorrowed = Collections.synchronizedMap(new HashMap<>());
    	moneyLent = Collections.synchronizedMap(new HashMap<>());
    }
    
    public String getName() {
    	return name;
    }
    
    public InetAddress getIp() {
    	return ip;
    }
    
    public void getEligibleLenders() {
    	
    }
    
    //acting as a client
    public void getMoney(String ipToConnect, int portToConn, int moneyReq) throws IOException {
    	Socket sc = new Socket(ipToConnect, portToConn);
    	
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        
        out.println("BORROW "+moneyReq);
        String response = in.nextLine();
        System.out.println(response);
        
    }
    
    //acting as a Server
    public void sendMoney(String clientName) throws IOException {
    	//accepts the connection from borrower peer
    	Socket client = sc.accept();
    	
    	//<--Might have to edit---->
    	//read the input from client 
    	Scanner input = new Scanner(client.getInputStream());
    	String msg = input.nextLine();
    	String[] parsed = Message.parseMessage(msg);
    	
    	//update balance
    	int reqMoney =  Integer.parseInt(parsed[parsed.length-1]);
    	if(reqMoney<balance)
    		balance = balance-reqMoney;
    	
    	//send AWK message
        PrintWriter output = new PrintWriter(client.getOutputStream(), true);
        output.println("AWK MONEY SENT "+clientName);
        
        //close connections
        input.close();
        output.close();
        client.close();
    }
    
    public void stopConnections() {
    	
    }
}
