package edu.nyu.cs.crowdlending.driver;

import java.util.Arrays;
import java.util.StringTokenizer;

class Message {
	ProtocolMessageType type;
    String message;

    public Message(ProtocolMessageType type, String message) {
        this.type = type;
        this.message = message;
    }
    
    public static String[] parseMessage(String msg) {
    	String[] parsed = msg.split(" ");
    	return Arrays.copyOfRange(parsed, 1, parsed.length);	 
    }
    @Override
    public String toString() {
        return this.message;
    }
}
