/**
 * 
 */
package edu.nyu.cs.crowdlending.driver;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author varada
 *
 */
public class Simulator {

	/**
	 * args[0] :  Account No
	 * args[1]: balance
	 * args[2]: port no
	 * @param args
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {		
		if(args.length!=3) {
			System.out.println("Arguments entered less than 3...Exiting the Window");
			System.exit(0);
		}
		Client client =  new Client(args[0], Integer.parseInt(args[1]), "127.0.0.1", Integer.parseInt(args[2]));
		client.register();
		
		Scanner sc = new Scanner(System.in);
		//register the client to server		
		while(true) {
			/*
			 * 1) borrow money
			 * 2)return money
			 * 3)close me!!
			 */
			System.out.println("Enter 1 to borrow, 2 to return, 3 deposit money, 4 withdraw money");
			int option = sc.nextInt();
			if(option==1) {
				System.out.println("Enter amount to borrow: ");
				int moneyRequested = sc.nextInt();
				String[] ipsToConnect = client.getEligibleLendersFromServer(moneyRequested);
				client.requestMoney(ipsToConnect, moneyRequested);
			}
			else if(option==2) {
				System.out.println(client.getMoneyBorrowedList());
				if(client.getMoneyBorrowedList().isEmpty()) {
		    		System.out.println("ILLEGAL OPERATION");	
		    	}
				else {
					System.out.println("Enter port of client to return");
					int port =  sc.nextInt();
					System.out.println("Enter amount to return");
					int amount =  sc.nextInt();
					client.returnMoney("127.0.0.1", port, amount);
				}
			}
			else if(option==3) {
				System.out.println("Enter amount to deposit");
				int money = sc.nextInt();
				client.depositMoney(money);
			}
			else if(option==4) {
				System.out.println("Enter amount to withdraw");
				int money = sc.nextInt();
				client.withdrawMoney(money);
			}
			else {
			boolean decision =	client.leaveNetwork();
			if(decision)
				break;
			}
		}
		sc.close();
	}
}
