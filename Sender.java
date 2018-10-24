package ass2;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Sender implements Runnable {
	
	static DatagramSocket senderSocket;
	static byte[] sendData=new byte[1024];
	static byte[] receiveData=new byte[1024];
	static DatagramPacket[] buffer=new DatagramPacket[10000];
	static int windowSize;
	static PacketS[] packets;
	static InetAddress IPAddress;
	
	public static void main(String[] args) throws Exception{
		
		Scanner scan=new Scanner(System.in);
		senderSocket=new DatagramSocket();
		IPAddress=InetAddress.getByName("localhost");	
		packets=new PacketS[1000];
		windowSize=10;
//		ArrayList<PacketS> packets=new ArrayList<PacketS>();  
		
		//FOR SETTING UP THE WINDOW SIZE
		System.out.println("Please enter the window size: ");
		windowSize=scan.nextInt();
		byte[] window=new byte[1024];
		window=String.valueOf(windowSize).getBytes();
		DatagramPacket windowSizePacket = new DatagramPacket(window, window.length, IPAddress,9876);
		senderSocket.send(windowSizePacket);
				
//		scan.nextLine();
		//THREAD FOR RECEIVING ACKNOWLEDGEMENTS
		Thread forAck= new Thread(new Sender());
		forAck.start();
        for (int i=0;i<windowSize;i++){
        	
        	packets[i]=new PacketS();
        	packets[i].seqno=i;
        	packets[i].setPayload("PacketNumber:"+Integer.toString(i));
        	
        }
//		String sentence= scan.nextLine();
//		sendData=sentence.getBytes();
        for (int i=0;i<windowSize;i++){
        	
        	sendData=packets[i].getPayload().getBytes();
        	DatagramPacket sendPacket=new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
        	buffer[Integer.parseInt(new String(sendPacket.getData()).split(" ")[0])]=sendPacket; //SETTING UP THE SLIDING WINDOW OF WHICH PACKETS TO SEND AND PUTTING THEM IN THE BUFFER
        	if(i==6){
        		continue;
        	}
        	senderSocket.send(sendPacket);
        	//STORING IN THE BUFFER THE PACKETS THAT HAVE BEEN SENT BY THE SENDER
        	
//        	for(int f=0;f<windowSize;f++){
//        		System.out.println(buffer[f]);
//        	}
//        	System.out.println();
//        	Runtime.getRuntime().halt(0);
//    		TimeUnit.SECONDS.sleep(2);

        }
		
//		DatagramPacket receivePacket= new DatagramPacket(receiveData, receiveData.length);
//		clientSocket.receive(receivePacket);
//		String modifiedSentence=new String(receivePacket.getData());
//		System.out.println("FROM SERVER: "+modifiedSentence);
//        senderSocket.close();

	}
	public void run(){
		Thread forTimeout=new Thread(new ForTimeouts());
		forTimeout.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while(true){
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				senderSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        String sentence=new String(receivePacket.getData());
	        System.out.println("RECEIVED from Receiver: "+sentence);
	        String payloadReceived[]=sentence.split(" ");	        
	        int lastGoodAck=Integer.parseInt(payloadReceived[5].trim());
	        if(lastGoodAck==-1){
	        	lastGoodAck=10;
	        }
	        for(int makingBufferNull=0;makingBufferNull<lastGoodAck;makingBufferNull++){
	        	buffer[makingBufferNull]=null;
	        }
	        
//	        for(int f=0;f<windowSize;f++){
//        		System.out.println(buffer[f]);
//        	}
//        	Runtime.getRuntime().halt(0);
		}
	}
	
//	class ForSendingPackets implements Runnable{
//
//		
//		public void run() {
//			
//				
//		}
//		
//		
//	}
	
	class ForTimeouts implements Runnable{
		
		public void run(){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			for(int i=0;i<windowSize;i++){
				if(buffer[i]!=null){ //THIS MEANS THAT THE ACK FOR THIS PACKET WAS NOT RECEIVED					
					System.out.println("UH OH!!, THERE IS A TIMEOUT FOR PACKET "+i);
					sendData=packets[i].getPayload().getBytes();
		        	DatagramPacket sendPacket=new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
		        	try {
						senderSocket.send(sendPacket);
						Thread.sleep(2000); //THIS IS FOR WHEN ONE IS NOT FOUND TO BE NULL (I.E. ACK NOT RECEIVED, THEN AFTER SENDING THE PACKET, THERE SHOULD BE SOME WAIT TIME SO THAT THE WINDOW CAN BE SHIFTED AGAIN (BY MAKING NULL IN BUFFER)
					} catch (IOException e) {						
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		        	
				}
			}
		}
	}

}

//payload format - [0]=seqno, [1]=data, [2]=ack
class PacketS{
	int seqno;
	String payload;
	int ack;
	
	public int getSeqno(){
		return Integer.parseInt(payload.split(" ")[0]);
	}
	public int getAck(){
		return Integer.parseInt(payload.split(" ")[2]);
	}
	public void setAck(){
		ack=1;
	}
	public String getPayload(){
		return payload;
	}
	public void setPayload(String load){
		payload=seqno+" "+load+" "+ack;
	}
}
