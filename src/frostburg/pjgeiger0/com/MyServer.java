package frostburg.pjgeiger0.com;

/**
 * Server
 */

import java.io.*;
import java.net.*;
import java.util.Random;

public class MyServer extends Thread{
    private DatagramSocket socket;
    private boolean running;
    private int percentage;
    //private byte[] buff = new byte[256];

    public MyServer(int input, int port) throws IOException{
        socket = new DatagramSocket(4445);
        percentage = input;
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
            byte[] buff = new byte[256];
            try {

                DatagramPacket packet = new DatagramPacket(buff, buff.length);
                socket.receive(packet);
                int seqNumber = packet.getData()[3];
                if(seqNumber == previousSeqNumber){
                    byte[] ackBuff = new byte[254];
                    String ACK = "ACK";
                    ackBuff = ACK.getBytes();
                    packet = new DatagramPacket(ackBuff, ackBuff.length, packet.getAddress(), packet.getPort());
                    if(dropSim.nextInt(101) > percentage){
                        socket.send(packet);
                    }
                    continue; //last packet is the same seq number as this packet, resend ack
                }
                previousSeqNumber = seqNumber;
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
                if(dropSim.nextInt(101) > percentage){
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
