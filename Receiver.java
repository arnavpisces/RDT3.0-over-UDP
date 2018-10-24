package ass2;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Receiver implements Runnable {
	
	static DatagramPacket[] buffer=new DatagramPacket[10000];
	static byte[] receiveData = new byte[1024];
    static byte[] sendData = new byte[1024];
    static DatagramSocket receiverSocket;
    static int windowSize;
    static int lastAck; //KEEPS TRACK OF THE FRAME OF WHICH LAST ACK WAS NOT RECEIVED
    
	public static void main(String[] args) throws Exception {
//		for(int i=0;i<10;i++){
//			if(buffer[i]==null){
//				System.out.println("ITS NULL");
//			}
//		}
//		Runtime.getRuntime().halt(0);
		receiverSocket = new DatagramSocket(9876);
        windowSize=10;
        
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
		Thread forEarliestFrame=new Thread(new loopChcekingEarliestFrame());
		forEarliestFrame.start();
		int counterForSendingAck=0;
		while(true){
		//RECEIVING PACKETS
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		try {
			receiverSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
        String sentence=new String(receivePacket.getData());
        System.out.println("RECEIVED from Sender: "+sentence);
        buffer[Integer.parseInt(new String(sentence.split(" ")[0]))]=receivePacket;
        
        try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {		
			e1.printStackTrace();
		}
        //SENDING ACKS
        InetAddress IPAddress=receivePacket.getAddress();
        int port=receivePacket.getPort();
        String acknowledge="ACK of "+sentence.split(" ")[0]+" EXPECTED ACK: "+String.valueOf(lastAck);
//    	if(Integer.parseInt(sentence.split(" ")[0])==6){
//    		continue;
//    	}
        sendData=acknowledge.getBytes();
        DatagramPacket sendPacket=new DatagramPacket(sendData, sendData.length, IPAddress, port);
        try {
        	if(counterForSendingAck%2==0) //ONLY SEND THE ACK AFTER EVERY 3RD PACKET (SAME AS WHAT HAPPENS IN OPTIMIZED TCP NOW)
			receiverSocket.send(sendPacket);
		} catch (IOException e) { 
			e.printStackTrace();
		}
        counterForSendingAck++;
	}
		}
	
	class loopChcekingEarliestFrame implements Runnable{
		
		public void run(){
			while(true){
				int flag=0;
				for(int i=0;i<windowSize;i++){
					if(buffer[i]==null){
						lastAck=i;
						flag=1;
						break;
					}
				}
				if(flag==0){
					lastAck=-1;
				}
				try {
					Thread.sleep(200);
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
