package frostburg.pjgeiger0.com;

import java.net.*;
import java.io.*;
import java.util.Random;


public class GBN_Server extends Thread {

    private DatagramSocket socket;
    private boolean running;
    private int faultPercentage;
    private int windowSize = 3;
    private int bufferSpace = 256;
    private int expectedSequenceNumber = 0;

    public GBN_Server(int input, int port) throws IOException{
        socket = new DatagramSocket(4445);
        faultPercentage = input;

    }

    public void run() {
        Random dropSim = null;
        running = true;
        PrintWriter pr = null;
        try{
            pr = new PrintWriter("server_output.txt");
        }
        catch(IOException e){
            System.out.println(e);
        }
//        DatagramPacket lastPacket = null;
        int previousSeqNumber = 1;
        while(running){
            dropSim = new Random(System.currentTimeMillis());
            byte[] buff = new byte[bufferSpace];
            try {

                DatagramPacket packet = new DatagramPacket(buff, buff.length);
                socket.receive(packet);
                int seqNumber = packet.getData()[3];
                //compare the sequence number of the current packet to what's expected, if it doesn't match up we got an issue
                //if you don't get what you expect to get, resend the ACK for the last packet that was received
                if(seqNumber == expectedSequenceNumber){
                    byte[] ackBuff = new byte[254];
                    String ACK = "ACK";
                    ackBuff = ACK.getBytes();
                    packet = new DatagramPacket(ackBuff, ackBuff.length, packet.getAddress(), packet.getPort());
                    if(dropSim.nextInt(101) > faultPercentage){
                        socket.send(packet);
                    }
                    continue; //last packet is the same seq number as this packet, resend ack
                }

                //add 1 to the expected sequence number
                expectedSequenceNumber = expectedSequenceNumber + 1;
                if(expectedSequenceNumber > windowSize){
                    expectedSequenceNumber = 0; //if the expected sequence number gets bigger than the window size, reset to 0 because that will be what's next
                }
                String received = new String(packet.getData(), 4, packet.getLength());
                //System.out.println(received);
                pr.println(received);
                pr.flush();
                if (received.equals("end")) {
                    running = false;
                    System.out.println("Server closing");
                    pr.close();
                    continue;
                }
                //sending ACK
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                byte[] ackBuff = new byte[256];
                String ACK = "ACK";
                ackBuff = ACK.getBytes();
                packet = new DatagramPacket(ackBuff, ackBuff.length, address, port);
                if(dropSim.nextInt(101) > faultPercentage){
                    socket.send(packet);
                }
//                lastPacket = new DatagramPacket(buff, buff.length);
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
        socket.close();
    }


}
