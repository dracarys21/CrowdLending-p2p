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
		String[] arr = splitInputString(input);
		String amountString = arr[2];
		int amountRequested = Integer.parseInt(amountString);
		List<String> eligibleLenders = new ArrayList<>();
		for(Entry<String, Integer> e: directory.entrySet()) {
			String ipAndPort = e.getKey();
			int balance = e.getValue();
			if(balance >= amountRequested) {
				eligibleLenders.add(ipAndPort);
			}
		}
		out.println(String.join(",", eligibleLenders));
	}
	
	private static void registerNewPeer(String input, PrintWriter out) {
		String[] arr = splitInputString(input);
		String ipAndPort = arr[1];
		int balance = Integer.parseInt(arr[2]);
		directory.put(ipAndPort, balance);
		out.println("AWK REGISTERD " + ipAndPort);
	}
	
	private static void updateBalance(String input, PrintWriter out) {
		String[] arr = splitInputString(input);
		String ipAndPort = arr[1];
		int updatedBalance = Integer.parseInt(arr[2]);
		directory.put(ipAndPort, updatedBalance);
		out.println("AWK UPDATED " + ipAndPort + " " + updatedBalance);
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
