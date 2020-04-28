/*
Shreyas Pandey
Sowmiya Iyengar
*/



import java.io.IOException;
import java.net.*;


public class serverlastproject {
	static  int counter=1;
	private static final int NUMBER_OF_PACKETS=10;
	private static final int PORT_NUMBER=3000;
	private static final int INIT_LENGTH=9;
	private static final int ACK_LENGTH=5;
	private static final int DATA_LENGTH=305;
	private static final int INTEGRITYCHECK_BYTES=2;
	private static final int PAYLOAD_BYTES=300;
	private static final int INIT_PACKET_TYPE=0x55;
	private static final int IACK_PACKET_TYPE=0xaa;
	private static final int DATA_PACKET_TYPE=0x33;
	private static final int DACK_PACKET_TYPE=0xcc;

	public static void main(String[] args) throws IOException {
		DatagramSocket serversocket=new DatagramSocket(PORT_NUMBER);
		byte init[]=new byte[INIT_LENGTH];
		byte iack[]=new byte[ACK_LENGTH];
		int initial_sequence_number,sequence_number=0;
		long time_firstData_packet=0;
		long time_lastData_packet=0;
		long time_calculate=0;
		DatagramPacket firstpacket = new DatagramPacket(init,INIT_LENGTH);
		serversocket.receive(firstpacket);
		initial_sequence_number=byteToIntegerSequenceConversion(init);
		System.out.println("INIT Packet is:-");
		for(int i=0;i<init.length;i++)
		{
			System.out.println(init[i]);
		}
		if(initCheck(init))                                                            //Call initcheck method to check authenticity
			iack=iackPacket(initial_sequence_number);
		else
		{
			System.out.println("INIT Packet is corrupt");
			System.exit(0);
		}
		

		DatagramPacket sendpacket = new DatagramPacket(iack,iack.length,firstpacket.getAddress(),firstpacket.getPort()); 
		serversocket.send(sendpacket);        //Send IACK packet
		while(counter<=NUMBER_OF_PACKETS)     //Loop for 10 packets
		{     

			byte[] data = new byte[DATA_LENGTH];
			DatagramPacket receivepacket = new DatagramPacket(data,DATA_LENGTH);
			serversocket.receive(receivepacket);

			if(counter==1)          //time measurement of 1st packet
			{
				time_firstData_packet=System.currentTimeMillis();
			}
			else if(counter==10)     //time measurement of 2nd packet
			{
				time_lastData_packet = System.currentTimeMillis();
				time_calculate = time_lastData_packet-time_firstData_packet;
			} 
			sequence_number=byteToIntegerSequenceConversion(data);
			int packetType=data[0]&0xff;
			int dack_seq=0;

			if(integrityCheck(data)&&(sequence_number==(initial_sequence_number+1)||sequence_number==(dack_seq))&&(packetType==DATA_PACKET_TYPE))  //DATA testing
			{
				dataPacketDisplay(data);
				byte dack[]=dackPacket(sequence_number);
				dack_seq=byteToIntegerSequenceConversion(dack);
				DatagramPacket dackpacket = new DatagramPacket(dack,dack.length,firstpacket.getAddress(),firstpacket.getPort()); 
				serversocket.send(sendpacket);
			}
			else
				System.out.println("Wrong data!");
		}
		double time=(300*0.08)/(double)time_calculate;                                       //Data transmitted rate calculated
		System.out.println("Data Transmission Rate:- "+time+" Mbits/sec    ");             
	}

	//byte to integer conversion for sequence number
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
  //data packet received from client
	private static void dataPacketDisplay(byte[] data)
	{

		System.out.println("Data from "+counter+" packet is:-");
		for(int i=3;i<303;i++)
		{                                                                              //Displaying 300 bytes of data in the screen
			System.out.println("Byte "+(i-2)+":- "+data[i]);
		}
		counter++;
	}
//DACK packet formation and sending
	private static byte[] dackPacket(int sequence_number) throws SocketException
	{
		byte[] dack=new byte[ACK_LENGTH];
		dack[0]=(byte) DACK_PACKET_TYPE;
		sequence_number=sequence_number+PAYLOAD_BYTES;
		                                                                                  //to check if highest value of sequence value is passed.
		dack[1]=(byte)((sequence_number>>>8)&0xff);
		dack[2]=(byte)((sequence_number)&0xff);
		dack[3]=(byte) (dack[0]^dack[3]);
		dack[4]=(byte) (dack[1]^dack[4]);
		return dack;

	}
//INIT package check
	private static boolean initCheck(byte[] init)
	{
		int init_packet_type;
		init_packet_type=Integer.parseInt(String.valueOf(init[0]));
		if(integrityCheck(init)&&init_packet_type==INIT_PACKET_TYPE)             //check INIT condition
			return true;
		else
			return false;
	}
//IACK packet formation
	private static byte[] iackPacket(int initial_sequence_number) throws SocketException
	{
		byte[] iack=new byte[ACK_LENGTH];
		iack[0]=(byte) IACK_PACKET_TYPE;
		int sequence_number=initial_sequence_number+1;                              //incrementing sequence number by 1
		iack[1]=(byte)((sequence_number>>>8)&0xff);
		iack[2]=(byte)((sequence_number)&0xff);
		iack[3]=(byte) (iack[0]^iack[3]);
		iack[4]=(byte) (iack[1]^iack[4]);
		return iack;

	}
//integrity check method
	private static boolean integrityCheck(byte[] packet) 
	{
		byte integrity[]=new byte[INTEGRITYCHECK_BYTES];

		for(int init_loop=0;init_loop<packet.length-3;init_loop++)                         //XOR bytes
		{
			if(init_loop%2==0)
				integrity[0]=(byte) (integrity[0]^packet[init_loop]);           
			else
				integrity[1]=(byte) (integrity[1]^packet[init_loop]);	

		}
		integrity[0]=(byte)(integrity[0]^packet[packet.length-2]);                         //XOR with checksum ie., with itself
		integrity[1]=(byte)(integrity[1]^packet[packet.length-1]);

		if(integrity[0]==0&&integrity[1]==0)
			return true;
		else 
			return false;

	}


}
