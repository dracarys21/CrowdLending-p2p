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
			int option = sc.nextInt();
			if(option==1) {
				System.out.println("Enter amount to borrow: ");
				int moneyRequested = sc.nextInt();
				String[] ipsToConnect = client.getEligibleLendersFromServer(moneyRequested);
				client.requestMoney(ipsToConnect, moneyRequested);
			}
			else if(option==2) {
				System.out.println("Enter port Number of client to return");
				int port =  sc.nextInt();
				client.returnMoney("localhost:"+port);
			}
			else 
				break;
		
		}
		sc.close();
	}
}
