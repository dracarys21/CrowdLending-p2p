package edu.nyu.cs.crowdlending.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class Server {
	
	private static Map<String, Integer> directory = Collections.synchronizedMap(new HashMap<>());
	
	public static void main(String[] args) {
        try (var listener = new ServerSocket(59898)) {
            System.out.println("The banking server is running...");
            var pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new Lender(listener.accept()));
            }
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private static String[] splitInputString(String input) {
		String[] arr = input.split(" ");
		return arr;
	}
	
	private static void queryDirectory(String input, PrintWriter out) {
		System.out.println("String is " + input);
		String[] arr = splitInputString(input);
		String borrower = arr[1];
		String amountString = arr[2];
		int amountRequested = Integer.parseInt(amountString);
		System.out.println("Directory is " + directory);
		List<String> eligibleLenders = new ArrayList<>();
		for(Map.Entry<String, Integer> e: directory.entrySet()) {
			String ipAndPort = e.getKey();
			int balance = e.getValue();
			if(balance >= amountRequested && !ipAndPort.equals(borrower)) {
				eligibleLenders.add(ipAndPort);
			}
		}
		System.out.println("Here are the eligible lenders: " + String.join(",", eligibleLenders));
		out.println(String.join(",", eligibleLenders));
	}
	
	private static void registerNewPeer(String input, PrintWriter out) {
		String[] arr = splitInputString(input);
		String ipAndPort = arr[1];
		int balance = Integer.parseInt(arr[2]);
		if(!directory.containsKey(ipAndPort)) {
			directory.put(ipAndPort, balance);
			out.println("AWK REGISTERED " + ipAndPort);
		}
		else
			out.println("AWK NOT REGISTERD");
	}
	
	private static void updateBalance(String input, PrintWriter out) {
		String[] arr = splitInputString(input);
		String ipAndPort = arr[1];
		int updatedBalance = Integer.parseInt(arr[2]);
		directory.put(ipAndPort, updatedBalance);
		out.println("AWK UPDATED " + ipAndPort + " " + updatedBalance);
	}

	public static void removePeer(String line, PrintWriter out) {
		String[] arr = splitInputString(line);
		String ipAndPort = arr[1];
		if(directory.containsKey(ipAndPort)) {
			directory.remove(ipAndPort);
			out.println("AWK PEER "+ipAndPort+" REMOVED");
		}
		else
			out.println("AWK PEER "+ipAndPort+" NOT PRESENT IN NETWORK");
	}
    private static class Lender implements Runnable {
        private Socket socket;

        Lender(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket);
            try {
                var in = new Scanner(socket.getInputStream());
                var out = new PrintWriter(socket.getOutputStream(), true);
                
                String line = in.nextLine();
                //1. Parse input
                if(line.contains("REGISTER")) {
                	Server.registerNewPeer(line, out);
                }
                else if(line.contains("BORROW")) {
                	Server.queryDirectory(line, out);
                }
                else if(line.contains("UPDATE")) {
                	Server.updateBalance(line, out);
                }
                else if(line.contains("LEAVE")) {
                	Server.removePeer(line,out);
                }
                in.close();
            }
            catch (Exception e) {
                System.out.println("Error:" + socket);
            }
        }
    }

}
