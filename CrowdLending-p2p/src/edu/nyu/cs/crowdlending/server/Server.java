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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class Server {
	
	private static Map<String, Integer> directory = Collections.synchronizedMap(new HashMap<>());
	
	public static void main(String[] args) throws Exception {
        try (var listener = new ServerSocket(59898)) {
            System.out.println("The banking server is running...");
            var pool = Executors.newFixedThreadPool(20);
            while (true) {
                pool.execute(new Lender(listener.accept()));
            }
        }
    }
	
	private static String[] splitInputString(String input) {
		String[] arr = input.split(" ");
		return arr;
	}
	
	private static String queryDirectory(String input) {
		String[] arr = splitInputString(input);
		String amountString = arr[2];
		int amountRequested = Integer.parseInt(amountString);
		List<String> eligibleLenders = new ArrayList<>();
		for(Entry<String, Integer> e: directory.entrySet()) {
			String ip = e.getKey();
			int balance = e.getValue();
			if(balance >= amountRequested) {
				eligibleLenders.add(ip);
			}
		}
		return String.join(",", eligibleLenders);
	}
	
	private static void registerNewPeer(String input) {
		String[] arr = splitInputString(input);
		String ip = arr[1];
		int balance = Integer.parseInt(arr[3]);
		directory.put(ip, balance);
	}
	
	private static void updateBalance(String input) {
		String[] arr = splitInputString(input);
		String ip = arr[1];
		int updatedBalance = Integer.parseInt(arr[2]);
		directory.put(ip, updatedBalance);
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
                	Server.registerNewPeer(line);
                }
                else if(line.contains("BORROW")) {
                	String eligibleLenders = Server.queryDirectory(line);
                	out.println(eligibleLenders);
                }
                else if(line.contains("UPDATE")) {
                	Server.updateBalance(line);
                }
                in.close();
            }
            catch (Exception e) {
                System.out.println("Error:" + socket);
            }
            finally {
                try {
                    socket.close();
                }
                catch (IOException e) {
                	
                }
                System.out.println("Closed: " + socket);
            }
        }
    }
}
