/*
Shreyas Pandey
Sowmiya Iyengar
*/

import java.net.*;
import java.util.*;

public class client_UDP {

	private static final int NUMBER_OF_PACKETS=10;
	private static final int PORT_NUMBER=3000;
	private static final int INIT_LENGTH=9;
	private static final int ACK_LENGTH=5;
	private static final int DATA_LENGTH=305;
	private static final int INTEGRITYCHECK_BYTES=2;
	private static final int PAYLOAD_BYTES=300;
	private static final int SEQUENCENUMBER_BYTES=2;
	private static final int INIT_PACKET_TYPE=0x55;
	private static final int IACK_PACKET_TYPE=0xaa;
	private static final int DATA_PACKET_TYPE=0x33;
	private static final int DACK_PACKET_TYPE=0xcc;
	private static int timer_counter=1;
	private static long time_data_packet;
	private static long time_dack_packet;
	private static int counter_of_packet=0;

	public static void main(String[] args) throws Exception  {
		DatagramSocket ds=new DatagramSocket();//create socket
		InetAddress ip=InetAddress.getLocalHost();
		int sequence_number = 0;
		byte[]data = null;
		byte init[];
		byte ack[];
		Long[] time_calculate=new Long[11];
		init=init_method();//INIT is an array that will be sending to server
		int initial_sequence_number=byteToIntegerSequenceConversion(init);
		DatagramPacket dp=new DatagramPacket(init,INIT_LENGTH,ip,PORT_NUMBER); //create packet
		ds.send(dp);                                                           //send INIT packet

		while(counter_of_packet<=NUMBER_OF_PACKETS)
		{
			try {
				ack= new byte[ACK_LENGTH];
				DatagramPacket receivepacket = new DatagramPacket(ack,ACK_LENGTH);//receive ack?
				ds.setSoTimeout(1000*timer_counter); 
				ds.receive(receivepacket);//receive actual ack packet as well!?ack and receive both separately? 
				timer_counter=1;
				if(counter_of_packet!=0)
				{
					time_dack_packet=System.currentTimeMillis();
					time_calculate[counter_of_packet]=time_dack_packet-time_data_packet;
					if(counter_of_packet==10)                                      //when counter reached 10th packet
						roundTripTime_calculation(time_calculate);                //calculate and display round trip time

				}
				sequence_number=byteToIntegerSequenceConversion(ack);
				int packetType=ack[0]&0xff;
				if(integritycheck(receivepacket.getData())&&sequence_number==(initial_sequence_number+1)&&packetType==IACK_PACKET_TYPE&&timer_counter==1)  //IACK packet testing
				data=dataoperation(sequence_number);
				else if(integritycheck(receivepacket.getData())&&sequence_number==(initial_sequence_number+PAYLOAD_BYTES)&&packetType==DACK_PACKET_TYPE)   //DACK packet testing
				data=dataoperation(sequence_number);
				else 
				{
					System.out.println("Wrong Acknowledgment Packet");
				}


				counter_of_packet++;
			

			}
			catch (SocketTimeoutException e) {
				timer_counter=timer_counter*2;                    //If timeout then double the counter
				if(timer_counter<9)
				{
					if(counter_of_packet==0)
					{                                                    //send INIT again
						init=init_method();
						initial_sequence_number=byteToIntegerSequenceConversion(init);
						System.out.println("Sending init packet again");
						DatagramPacket send_init=new DatagramPacket(init,INIT_LENGTH,ip,PORT_NUMBER); 
						ds.send(send_init);
						continue;
					}
					else
					{
					System.out.println("Sending data packet again");
					DatagramPacket sendAgain=new DatagramPacket(data,data.length,ip,PORT_NUMBER);
					ds.send(sendAgain);
					continue;
					}
				}
				System.out.println("Timeout reached!!! " + e);                            //Timeout Reached
				ds.close();
				System.exit(0);
			}
		}



	}
//integrity check code
	private static boolean integritycheck(byte[] data) {

		byte integrity_check[]=new byte[INTEGRITYCHECK_BYTES];
		integrity_check[0]=(byte) (data[0]^data[3]);     //XOR data with integrity check variable
		integrity_check[1]=(byte) (data[1]^data[4]);     //XOR data with integrity check variable
		if(integrity_check[0]==0&&integrity_check[1]==0)
			return true;
		else 
			return false;
	}
//byte to integer conversion method
	private static int byteToIntegerSequenceConversion(byte[] data) 
	{
		int sequence_number;
		String lsb;
		lsb=Integer.toBinaryString(data[2]&0xff);
		if(lsb.length()==7)
			lsb=0+lsb;
		String test_dack=(Integer.toBinaryString(data[1]&0xff)+lsb);
		sequence_number=Integer.parseInt(test_dack,2);

		return sequence_number;


	}
//init packet creation
	private static byte[] init_method() {
		//creating byte array 
		byte[]init=new byte[INIT_LENGTH];                             //INIT package length
		byte[]initial_seqnumber=new byte[SEQUENCENUMBER_BYTES];
		byte data_packet[]= {1,0,30,0};                               //number of data packets and number of payload bytes
		byte[]integrity=new byte[INTEGRITYCHECK_BYTES];
		int loop=0; //call integrity check function
		init[0]=(byte)INIT_PACKET_TYPE;//first element of array is packet type...INIT or DATA

		Random rand=new Random();
		int seq_number=rand.nextInt(65535);                           // randomly generated 16 bit integer
		init[1]=(byte)((seq_number>>>8)&0xff);
		init[2]=(byte)((seq_number)&0xff);//next two array element contain sequence number ,since sequence number is 16 bit
		System.arraycopy(data_packet, 0, init, 3,data_packet.length);//next elements
		loop=initial_seqnumber.length+data_packet.length;
		for(int i=0;i<loop;i++)                                       //checksum part
		{    if(i%2==0)
		{ 
			integrity[0]=(byte)(integrity[0]^init[i]);//even elements are XORed as well as odd elementd are XORed.
			                                          //same thing will be done in server side and then XOR with integrity to get 0.if not zero then thow error 
		}
		else
		{
			integrity[1]=(byte)(integrity[1]^init[i]);
			
		}
		}
		System.arraycopy(integrity, 0, init, 7,INTEGRITYCHECK_BYTES);//copy integrity array to init.INIT's 7th array element
		for(int i=0;i<init.length;i++)                             //displaying content of INIT packet
		{System.out.println("init[i]"+init[i]);}
		return init;


	}

//DATA packet creation and sending
	private static byte[] dataoperation(int ackNumber) throws Exception {
		
		DatagramSocket ds=new DatagramSocket();
		InetAddress ip=InetAddress.getLocalHost();
		byte integrity[]=new byte[INTEGRITYCHECK_BYTES];
		byte payload[]=new byte[PAYLOAD_BYTES];
		Random rand=new Random();
		rand.nextBytes(payload);
		int payload_loop;
		byte data[]=new byte[DATA_LENGTH];                                       //payload+seq,hexnumber
		int data_loop;
		data[0]=DATA_PACKET_TYPE;
		data[1]=(byte)((ackNumber>>>8)&0xff);
		data[2]=(byte)((ackNumber)&0xff);

		System.arraycopy(payload,0,data,3,PAYLOAD_BYTES);
		System.out.println("data.length"+DATA_LENGTH);
		for(payload_loop=0;payload_loop<PAYLOAD_BYTES;payload_loop++)            //display packet bytes
		{
			System.out.println("Payload"+counter_of_packet+" "+payload_loop+"th location value  "+payload[payload_loop]);			
		}
		timer_counter++;
		for(data_loop=0;data_loop<data.length-3;data_loop++)                    //getting integrity check bytes
		{
			if(data_loop%2==0)
				integrity[0]=(byte) (integrity[0]^data[data_loop]);
			else
				integrity[1]=(byte) (integrity[1]^data[data_loop]);
		}
		System.arraycopy(integrity,0,data,303,INTEGRITYCHECK_BYTES);
		DatagramPacket dp=new DatagramPacket(data,data.length,ip,PORT_NUMBER);
		time_data_packet=System.currentTimeMillis();                             //current time recording
		ds.send(dp);
		return data;

	}
	private static void roundTripTime_calculation(Long[] time_calculate) {
		Long min_value=time_calculate[1];
		Long max_value=time_calculate[1];
		Long average_value=(long) 0;  
		for(int i=1;i<=NUMBER_OF_PACKETS;i++)
		{
			if(time_calculate[i]<min_value)
			{
				min_value=time_calculate[i];
			}
			if(time_calculate[i]>max_value)
			{
				max_value=time_calculate[i];
			}
			System.out.println("Packet"+(i)+" round trip time:- "+time_calculate[i]+"milliseconds");
			average_value=average_value+time_calculate[i];
		}
		average_value=average_value/NUMBER_OF_PACKETS;
		System.out.println("Minimum Round Trip Time:- "+min_value+"milliseconds");
		System.out.println("Maximum Round Trip Time:- "+max_value+"milliseconds");
		System.out.println("Average Round Trip Time:- "+average_value+"milliseconds");
		System.exit(0);
	}

}
