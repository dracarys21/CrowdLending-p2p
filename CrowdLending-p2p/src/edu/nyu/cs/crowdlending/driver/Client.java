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
       	balance = bal;
    	this.accountNumber = accNo;
    	ip = "127.0.0.1";
    	this.serverIp = "127.0.0.1";
    	serverPort = 59898;
    	this.port = port; 
    	moneyBorrowed = Collections.synchronizedMap(new HashMap<>());
    	moneyLent = Collections.synchronizedMap(new HashMap<>());
    	new Thread(new Runnable() {
		   public void run() {
			   try (var listener = new ServerSocket(port)) {
				   System.out.println("The peer is listening for borrow requests...");
		            var pool = Executors.newFixedThreadPool(20);
		            while (true) {
		                pool.execute(new ClientServer(listener.accept()));
		            }
		        } catch (IOException e) {
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
        String registerServ = ip + ":"+port + " " + balance;
        //send client's wish to get money
        out.println("REGISTER "+registerServ);
        System.out.println(in.nextLine());
        in.close();
        sc.close();
    }
    
    //ask server for lenders	
    public String[] getEligibleLendersFromServer(int moneyReq) throws IOException {
    	Socket sc = new Socket(serverIp, serverPort);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        out.println("BORROW " + ip + ":" + port + " " + moneyReq);
        String[] ipsToConnect = in.nextLine().split(",");
        in.close();
        sc.close();
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
        		 balance += moneyReq;
        		 if(moneyBorrowed.containsKey(ipsAndPort)) {
        			 moneyBorrowed.put(ipsAndPort, moneyBorrowed.get(ipsAndPort) + moneyReq);
        		 }
        		 else {
        			 moneyBorrowed.put(ipsAndPort, moneyReq);
        		 }
        		 System.out.println("MONEY RECEIVED FROM " + ipsAndPort);
        		 System.out.println("Current balance: " + balance);
        		 updateServer();
        		 break;
        	 }
        }        
    }
    
    //acting as a Server
    public  synchronized Runnable lendMoney(String clientIPAndPort, int reqMoney, Scanner input, PrintWriter output) throws UnknownHostException, IOException {
    	if(reqMoney<=balance) {
    		output.println("AWK AVAILABLE");
    		balance = balance - reqMoney;
    		moneyLent.put(clientIPAndPort, reqMoney);
    		System.out.println("Money lent to " + clientIPAndPort);
    		System.out.println("Current balance is " + balance);
    		//update server
            updateServer();
    	}
    	else {
    	 //send message if money not available to send
    		output.println("AWK NOT AVAILABLE");
    	}
		return null;
    }
    
    //acts as a client
    public synchronized void updateServer() throws UnknownHostException, IOException {
    	Socket sc = new Socket(serverIp, serverPort);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        String registerServ = ip + ":" + port;
        out.println("UPDATE " + registerServ + " " + balance);
        System.out.println(in.nextLine());
        in.close();
        sc.close();
    }
    
    //acts as a client 
    public synchronized void returnMoney(String ip, int port) throws IOException {
    	Socket sc = new Socket(ip, port);
    	PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
        Scanner in = new Scanner(sc.getInputStream());
        String ipWithPort = ip + ":" + port;
        int moneyRet = moneyBorrowed.get(ipWithPort);
        moneyBorrowed.remove(ipWithPort);
        balance = balance - moneyRet;
        out.println("RETURN " + this.ip + ":" + this.port + " " + moneyRet);
        System.out.println("Returned " + moneyRet + " to " + ipWithPort);
    }
    
    //acts as a server
    public synchronized Runnable receiveReturnedMoney(String clientIPAndPort, int moneyReturned, PrintWriter output) throws UnknownHostException, IOException {
    	moneyLent.put(clientIPAndPort, moneyLent.get(clientIPAndPort) - moneyReturned);
    	balance = balance + moneyReturned;
    	output.println("MONEY RECEIVED FROM " + clientIPAndPort);
    	System.out.println("Current balance is: " + balance);
    	updateServer();
		return null;
    }
    
    private class ClientServer implements Runnable{
    	private Socket socket;
    	ClientServer(Socket socket) {
            this.socket = socket;
        }
        
		@Override
		public void run() {
			System.out.println("Connected: " + socket);
			try {
	            while (true) {
	            	var in = new Scanner(socket.getInputStream());
	                var out = new PrintWriter(socket.getOutputStream(), true);
	                String line = in.nextLine();
	                String[] arr = line.split(" ");
	                String ipAndPort = arr[1];
	            	int amount = Integer.parseInt(arr[2]);
	                if(arr[0].equals("REQUEST")) {
	                	lendMoney(ipAndPort, amount, in, out);
	                }
	                else if(arr[0].equals("RETURN")) {
	                	receiveReturnedMoney(ipAndPort, amount, out);
	                }
	            }
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    	
    }
}
