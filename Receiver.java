package ass2;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

import org.w3c.dom.css.Counter;

public class Receiver implements Runnable {
	
	static DatagramPacket[] buffer=new DatagramPacket[10000];
	static byte[] receiveData = new byte[1024];
    static byte[] sendData = new byte[1024];
    static DatagramSocket receiverSocket;
    static int windowSize;
    static int lastAck; //KEEPS TRACK OF THE FRAME OF WHICH LAST ACK WAS NOT RECEIVED
    static int globalShifter;
    static int counterForReceivingDirectlyInArray=0;
    
	public static void main(String[] args) throws Exception {
//		for(int i=0;i<10;i++){
//			if(buffer[i]==null){
//				System.out.println("ITS NULL");
//			}
//		}
//		Runtime.getRuntime().halt(0);
		receiverSocket = new DatagramSocket(9876);
//        windowSize=10;
        
		//FOR SETTING UP THE WINDOW SIZE
	
	    
		DatagramPacket windowSizePacket = new DatagramPacket(receiveData, receiveData.length);
		receiverSocket.receive(windowSizePacket);
		String sentence=new String(windowSizePacket.getData());
		windowSize=Integer.parseInt(sentence.trim());
        System.out.println("THE WINDOW SIZE IS - "+sentence);
        
        Thread sendingAck=new Thread(new Receiver());
        sendingAck.start();
        
//        while(true)
//           {
//              DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//              receiverSocket.receive(receivePacket);
//              String sentence=new String(receivePacket.getData());
//              System.out.println("RECEIVED: "+sentence);
//              
//                     
//           }
	}	
	public void run(){
		Thread forEarliestFrame=new Thread(new loopCheckingEarliestFrame());
		forEarliestFrame.start();
		int counterForSendingAck=0;
		counterForReceivingDirectlyInArray=0;
		
		while(true){
		
//		System.out.println("counterForReceivingDirectlyInArray is "+counterForReceivingDirectlyInArray);
		//RECEIVING PACKETS
			byte[] receiveData1 = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData1, receiveData1.length);
		try {
			receiverSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
        String sentence=new String(receivePacket.getData());
        System.out.println("RECEIVED from Sender: "+sentence);
        
        buffer[Integer.parseInt(new String(sentence.split(" ")[0]))]=receivePacket;
//        for(int x=0;buffer[x]!=null;x++){
//        	System.out.println(new String(buffer[x].getData())+" GLobalShifter"+globalShifter);
//        }
        try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {		
			e1.printStackTrace();
		}
        //SENDING ACKS
        
        //TO SIMULATE THE EFFECT OF SENDING ACKS AFTER A WHILE (TO SHOW NOT STOP AND WAIT)
//        if(counterForSendingAck%5==0){
//        	try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//        }
        
        InetAddress IPAddress=receivePacket.getAddress();
        int port=receivePacket.getPort();
        
//        String acknowledge="ACK of "+sentence.split(" ")[0]+" EXPECTED ACK: "+String.valueOf(lastAck); //USE THIS WHEN YOU WANT TO IMPLEMENT CUMULATIVE ACKS
        String acknowledge="ACK of "+sentence.split(" ")[0];
//    	if(Integer.parseInt(sentence.split(" ")[0])==6){
//    		continue;
//    	}S
        sendData=acknowledge.getBytes();
        DatagramPacket sendPacket=new DatagramPacket(sendData, sendData.length, IPAddress, port);
        
        try {
//        	if(counterForSendingAck%5==0) //ONLY SEND THE ACK AFTER EVERY 3RD PACKET (SAME AS WHAT HAPPENS IN OPTIMIZED TCP NOW)
			receiverSocket.send(sendPacket);
		} catch (IOException e) { 
			e.printStackTrace();
		}
        counterForReceivingDirectlyInArray++;
        counterForSendingAck++;
	}
		
		}
	
	class loopCheckingEarliestFrame implements Runnable{
		
		public void run(){
			while(true){
				int flag=0;
				int offset=windowSize*globalShifter;
				for(int i=0+offset;i<offset+windowSize;i++){
					if(i==0)continue; //AS 0%ANY NUMBER IS 0 THEREFORE lastAck WONT EVER CHANGE, SO BETTER TO CONTINUE AT I=0
					if(buffer[i]==null){
//						System.out.println("BUFFER IS NULL AT "+i);					
						lastAck=i;
						flag=1;
						break;
					}
				}
				if(flag==0){
//					try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					lastAck=-1;
					System.out.println("In-Order Data:");
					for(int z=0+offset;z<offset+windowSize;z++){
						System.out.print(Integer.parseInt(new String(buffer[z].getData()).split(" ")[0].trim())+" ");
					}
					System.out.println();
					globalShifter++;
					counterForReceivingDirectlyInArray=0+offset;
					System.out.println("\n>>>>SLIDING WINDOW SHIFT");
				}
//				System.out.println();
				try {
					Thread.sleep(300); //SO THAT LAST ACK GETS UPDATED ALSO AND DOESNT REMAIN STICK ON ONE INDEX
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}

class PacketR{
	int seqno;
	String payload;
	int ack;
	
	public int getSeqno(){
		return seqno;
	}
	public int getAck(){
		return ack;
	}
	public void setPayload(String load){
		payload=load;
	}
}
