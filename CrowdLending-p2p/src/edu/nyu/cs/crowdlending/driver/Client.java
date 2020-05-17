package edu.nyu.cs.crowdlending.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class Client {
	private String accountNumber;
    private String ip;
    
    private int balance;
    private Map<String, Integer> moneyBorrowed;
    private Map<String,Integer> moneyLent;
    String serverIp;
    int serverPort;
    int port;
    
    //ServerSocket serviceSocket;
    
    public Client(String accNo, int bal, String serverIp, int port) throws IOException {
    	//this.port = port; 
    	//serviceSocket = new ServerSocket(port);
       	balance = bal;
    	this.accountNumber = accNo;
    	//ip = ipAddress;
    	this.serverIp = "127.0.0.1";
    	serverPort = 59898;
    	this.port = port; 
    	moneyBorrowed = Collections.synchronizedMap(new HashMap<>());
    	moneyLent = Collections.synchronizedMap(new HashMap<>());
    	new Thread(new Runnable() {
		   public void run() {
		       try {
				startClientServer(port);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
		}).start();
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
        sc.close();
        in.close();
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
    public  synchronized Runnable lendMoney(String clientIPAndPort, int reqMoney, Scanner input, PrintWriter output) throws UnknownHostException, IOException {
    	if(reqMoney<=balance) {
    		output.println("AWK AVAILABLE");
    		balance = balance-reqMoney;
    		moneyLent.put(clientIPAndPort, reqMoney);
    		//update server
            updateServer();
    	}
    	else {
    	 //send message if money not available to send
    		output.println("AWK NOT AVAILABLE");
    	}
        //close connections
        input.close();
        output.close();
		return null;
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
        sc.close();
        in.close();
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
        sc.close();
        in.close();
    }
    
    //acts as a server
    public synchronized Runnable receieveBorrowedMoney(String clientIPAndPort, int moneyReturned, PrintWriter output) {
    	moneyLent.put(clientIPAndPort, moneyLent.get(clientIPAndPort) - moneyReturned);
    	balance = balance + moneyReturned;
    	output.println("MONEY RECEIVED FROM " + accountNumber);
		return null;
    }
    
    public void startClientServer(int port) throws Exception {
        try (var listener = new ServerSocket(port)) {	//args[0] = port of peer when it acts as a server
            System.out.println("The peer is listening for borrow requests...");
            var pool = Executors.newFixedThreadPool(10);
            while (true) {
            	Socket socket = listener.accept();
            	var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                String line = in.nextLine();
                String[] arr = line.split(" ");
                String ipAndPort = arr[1];
            	int amount = Integer.parseInt(arr[2]);
                if(arr[0].equals("REQUEST")) {
                	pool.execute(lendMoney(ipAndPort, amount, in, out));
                }
                else if(arr[0].equals("RETURN")) {
                	pool.execute(receieveBorrowedMoney(ipAndPort, amount, out));
                }
            }
        }
    }
}
