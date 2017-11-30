package frostburg.pjgeiger0.com;

/**
 * Client
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class MyClient {

    private DatagramSocket socket;
    private InetAddress address;
    private int percentage;

    private byte[] buff;

    public MyClient(int input, int ip) throws IOException{
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
        percentage = input;
    }

    public void sendMessage() throws IOException{

        Random dropSim = null;
        boolean ackReceived = false;
        int sequenceNumber = 0;
        byte[] ackBuff = new byte[256];
        String message = "";
        BufferedReader br;
        br = new BufferedReader(new FileReader("client_input.txt"));
        message = br.readLine();
        socket.setSoTimeout(1000); //sets timeout to 15 seconds
        while(message != null){

            buff = createPacket(sequenceNumber, message.getBytes());
            DatagramPacket packet = new DatagramPacket(buff, buff.length, address, 4445);
            DatagramPacket ackPacket = new DatagramPacket(ackBuff, ackBuff.length);
            //socket.send(packet);
            while(true) {
                dropSim = new Random(System.currentTimeMillis());
                if(dropSim.nextInt(101) > percentage){
                    socket.send(packet);
                }

                //ACK checking
                try {
                    socket.receive(ackPacket);
                    ackReceived = true;
                    System.out.println("ACK Received, sending next packet.");
                    if(sequenceNumber == 0){
                        sequenceNumber = 1;
                    }
                    else{
                        sequenceNumber = 0;
                    }
                    break;

//                    String received = new String(ackPacket.getData(), 0, ackPacket.getLength());
//                    if(received.equals("ACK")){
//                        break; //if the contents of packet is ACK, send next packet
//                    }
//                    else{
//                        continue; //if it's not, then send the last sent packet
//                    }
                } catch (InterruptedIOException e) {
                    //ack not received
                    System.out.println("ACK not received, retransmitting");
                    ackReceived = false;
                    continue;
                }
                finally {
                    break; //just in case
                }
            }
            if(ackReceived == true){
                message = br.readLine(); //read next line of the file if we got an ACK
            }


        }//end of while loop
        br.close();
//        buff = message.getBytes();
//        DatagramPacket packet = new DatagramPacket(buff, buff.length, address, 4445);
//        socket.send(packet);
//        packet = new DatagramPacket(buff, buff.length);
//        socket.receive(packet);
//        String received = new String(packet.getData(), 0, packet.getLength());
        //return received;

    }

    public void close(){
        socket.close();
        System.out.println("Client Closing");
    }

    //puts the sequence number and the payload together
    public byte[] createPacket(int seqNumber, byte[] data){
        byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNumber).array();

        ByteBuffer packetBuffer = ByteBuffer.allocate(4 + data.length);
        packetBuffer.put(seqNumBytes);
        packetBuffer.put(data);
        return packetBuffer.array();

    }
}
