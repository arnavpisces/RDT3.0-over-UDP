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
	static int globalShifter;
	
	public static void main(String[] args) throws Exception{
		
		Scanner scan=new Scanner(System.in);
		senderSocket=new DatagramSocket();
		IPAddress=InetAddress.getByName("localhost");	
		packets=new PacketS[10000];
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
        for (int i=0;i<10000;i++){
        	
        	packets[i]=new PacketS();
        	packets[i].seqno=i;
        	packets[i].setPayload("Packet:"+Integer.toString(i)+"SaysHi!");
        	
        }
        
//		String sentence= scan.nextLine();
//		sendData=sentence.getBytes();
        

		
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
		int counterForSendingAck=0;
		while(true){
			//TO SIMULATE THE EFFECT OF SENDING ACKS AFTER A WHILE (TO SHOW NOT STOP AND WAIT)
	        if(counterForSendingAck%5==0){
	        	try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        }
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				senderSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        String sentence=new String(receivePacket.getData());
	        System.out.println("RECEIVED from Receiver: "+sentence);
	        String payloadReceived[]=sentence.split(" ");	        
	        int lastGoodAck=Integer.parseInt(payloadReceived[2].trim());
	        
	        //IF YOU WANT TO IMPLEMENT CUMULATIVE ACKS
//	        if(lastGoodAck==-1){
//	        	lastGoodAck=windowSize;
//	        }
//	        for(int makingBufferNull=0;makingBufferNull<lastGoodAck;makingBufferNull++){
//	        	buffer[makingBufferNull]=null; //TO REMOVE THE ACKED PACKETS FROM THE BUFFER
//	        }
	        
	        buffer[lastGoodAck]=null;
	        
	        //FOR EVERY PACKET'S ACKS
//	        for(int f=0;f<windowSize;f++){
//        		System.out.println(buffer[f]);
//        	}
//        	Runtime.getRuntime().halt(0);
	        counterForSendingAck++;
		}
	}
	
	class ForSendingPackets implements Runnable{

		
		public void run() {
			
	        int offset=windowSize*globalShifter;
			for(int i=0+offset;i<offset+windowSize;i++){
	        	
	        	sendData=packets[i].getPayload().getBytes();
	        	DatagramPacket sendPacket=new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
	        	buffer[Integer.parseInt(new String(sendPacket.getData()).split(" ")[0])]=sendPacket; //SETTING UP THE SLIDING WINDOW OF WHICH PACKETS TO SEND AND PUTTING THEM IN THE BUFFER
	        	if(i%7==0){
	        		continue;
	        	}
	        	try {
					senderSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        	//STORING IN THE BUFFER THE PACKETS THAT HAVE BEEN SENT BY THE SENDER
	        	
//	        	for(int f=0;f<windowSize;f++){
//	        		System.out.println(buffer[f]);
//	        	}
//	        	System.out.println();
//	        	Runtime.getRuntime().halt(0);
//	    		TimeUnit.SECONDS.sleep(2);

	        }
				
		}
		
		
	}
	
	class ForTimeouts implements Runnable{
		
		public void run(){
			Thread sendZePackets=new Thread(new ForSendingPackets());
	        sendZePackets.start();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while(true){
			int checkForShift=0;
			int offset=windowSize*globalShifter;
			for(int i=0+offset;i<offset+windowSize;i++){
				if(buffer[i]!=null){ //THIS MEANS THAT THE ACK FOR THIS PACKET WAS NOT RECEIVED
					checkForShift=1;
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
			if(checkForShift==0){
				globalShifter++;
				System.out.println(">>>>SLIDING WINDOW SHIFT");
				sendZePackets=new Thread(new ForSendingPackets()); //THIS IS TO AGAIN SEND THE PACKETS AFTER THE SLIDING WINDOW HAS SHIFTED
		        sendZePackets.start();
		        try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
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
