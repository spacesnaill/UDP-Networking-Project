package frostburg.pjgeiger0.com;

//stop and wait
import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class SAW implements Runnable {

    //#- - - - - - - - - - Global Variables - - - - - - - - - -#

    private DatagramSocket socket; //socket we're listening on
    private InetAddress address; //ip we want to send data too
    private int packetDropRate; //holds the value for how often the packets are dropped
    private final int payloadSize = 8000; //how many bytes the packets can hold
    private boolean ACKreceived = true; //see whether or not the ACK arrived
    private boolean sendACK = false; //decide whether or not we need to send an ACK
    private byte[] dataBuffer = new byte[payloadSize];
    private ArrayList<byte[]> packetBuffer = new ArrayList<byte[]>();
    private String fileName = "";
    private int lastSequenceNumber = 0;
    private long timeOut = 1000000000;


    //#- - - - - - - - - - Constructor - - - - - - - - - -#

    /**
     *
     * @param userInput the user defined number between 0 and 99 to use for the simulated packet drop rate
     * @param ipAddress the IP of the host to make a connection with
     * @param port the port to listen on
     * @throws IOException in the case that the port or address are invalid
     */
    public SAW(int userInput, String ipAddress, int port) throws IOException{
        socket = new DatagramSocket(port);
        address = InetAddress.getByName(ipAddress); //convert String IP to InetAddress
        packetDropRate = userInput; //this is how often the packets will be dropped
        System.out.println("The IP of this computer is: " + socket.getLocalAddress());
        System.out.println("Listening on port: " + socket.getPort());

    }

    //remember that the address can be stripped from the incoming packets and used to send ACKs


    //#- - - - - - - - - - Threads - - - - - - - - - -#

    Thread sending = new Thread(){
        private boolean running;
        public void run(){
            running = true;
            long startTime = 0; //time when the last packet was sent, used to see if timeout has happened
            int packetsSentCounter = 0;
            byte[] fileBuffer = fileToBytes("client_input.txt");
            int currentSequenceNumber = 0;
            int segments = numberOfSegments(fileBuffer.length, payloadSize);
            DatagramPacket[] packetBuffer = new DatagramPacket[1];
            while(running){
                while(packetsSentCounter <= segments){
                    if(sendACK == true){
                        //send an ACK
                        byte[] ackBuffer = new byte[10];
                        DatagramPacket sendingPacketAck = new DatagramPacket(createPacket(lastSequenceNumber, 3, ackBuffer), ackBuffer.length, address, socket.getPort());
                        sendPacket(sendingPacketAck);
                    }
                    if (ACKreceived == true && packetsSentCounter < segments){
                        //send next packet
                        //make sure the data put into the packet doesn't go over the buffer
                        if(((packetsSentCounter*payloadSize)+payloadSize) >= fileBuffer.length){
                            byte[] packetData = createPacket(currentSequenceNumber, 2, Arrays.copyOfRange(fileBuffer, packetsSentCounter*payloadSize, fileBuffer.length));
                            DatagramPacket sendingPacket = new DatagramPacket(packetData, packetData.length, address, socket.getPort());
                            packetBuffer[0] = sendingPacket;
                            sendPacket(sendingPacket);
                            packetsSentCounter = packetsSentCounter + 1;
                        }
                        else{
                            byte[] packetData = createPacket(currentSequenceNumber, 2, Arrays.copyOfRange(fileBuffer, packetsSentCounter*payloadSize, packetsSentCounter*payloadSize+payloadSize));
                            DatagramPacket sendingPacket = new DatagramPacket(packetData, packetData.length, address, socket.getPort());
                            packetBuffer[0] = sendingPacket;
                            sendPacket(sendingPacket);
                            packetsSentCounter = packetsSentCounter + 1;
                        }
                    }
                    if (((System.nanoTime() - startTime) > timeOut)){
                        //send last packet again
                        DatagramPacket sendingPacketPrevious = packetBuffer[0];
                        sendPacket(sendingPacketPrevious);
                    }
                    if(packetsSentCounter == segments && ACKreceived == true){
                        //send packet to signal end of file
                        byte[] endBuffer = new byte[10];
                        DatagramPacket sendingPacketEnd = new DatagramPacket(createPacket(lastSequenceNumber, 0, endBuffer), endBuffer.length, address, socket.getPort());
                        packetBuffer[0] = sendingPacketEnd;
                        sendPacket(sendingPacketEnd);
                    }
                }//end of nested while loop



            }//end of inner while loop
        }//end of run
        public void sendPacket(DatagramPacket packet){
            try{
                socket.send(packet);
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
    };//end of sending thread

    Thread receiving = new Thread(){
        private boolean running;
        public void run(){
            running = true;
            while(running){
                byte[] buffer = new byte[payloadSize];
                DatagramPacket receivingPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(receivingPacket);
                }
                catch(IOException e){
                    System.out.println(e);
                }
                finally {
                    int code = byteArrayToInteger(Arrays.copyOfRange(receivingPacket.getData(),2, 4));
                    int sequenceNumber = byteArrayToInteger(Arrays.copyOfRange(receivingPacket.getData(), 0, 2));
                    if(code == 2) {
                        System.out.println("Packet received with: " + receivingPacket.getData().length + " bytes within.");
                        if(sequenceNumber == lastSequenceNumber){
                            System.out.println("Packet already received, sending another ACK");
                        }
                        else{
                            fillPacketBuffer(receivingPacket.getData(), 4);
                            lastSequenceNumber = sequenceNumber;
                        }
                        sendACK = true;
                    }//data filled packet
                    else if(code == 3){
                        ACKreceived = true;
                        System.out.println("ACK Received.");
                    }//ACK packet
                    else if(code == 1){
                        fileName = getStringFromByteArray(receivingPacket.getData(), 4);
                        System.out.println(fileName + " now downloading.");
                    }//beginning of file packet
                    else if(code == 0){
                        fillPacketBuffer(receivingPacket.getData(), 4);
                        System.out.println("End of file found.");
                        fileToBytes(fileName);
                        System.out.println(fileName + " fully downloaded.");
                    }//end of file packet
                }
            }
        }//end of run
    };//end of receiving thread

    public void run(){
        while(true) {
            if(socket.isConnected()) {
                sending.start();
                receiving.start();
            }
            else{
                System.out.println("Waiting for connection");
            }
        }
    }

    //#- - - - - - - - - - Helpers - - - - - - - - - -#

    /**
     *
     * @param seqNumber the sequence number to be assigned to the packet
     * @param data the data to go in the packet
     * @return byte array with data for a packet
     */
    public byte[] createPacket(int seqNumber, int identificationCode, byte[] data){
        //puts the packets together, the sequence number taking up the first couple of bytes
        byte[] seqNumBytes = ByteBuffer.allocate(2).putInt(seqNumber).array(); //sequence number to byte array
        byte[] idCodeBytes = ByteBuffer.allocate(2).putInt(identificationCode).array(); //identification number for what sort of packet this is

        ByteBuffer packetBuffer = ByteBuffer.allocate(4 + data.length); //allocate space for sequence number and data
        packetBuffer.put(seqNumBytes); //put the sequence number byte array in
        packetBuffer.put(idCodeBytes); //put the packet id code byte array in
        packetBuffer.put(data); //put the data byte array in
        return packetBuffer.array(); //return the byte array that is the packet
    }

    /**
     *
     * @return the byte array with the data for the packet
     */
    public byte[] createACK(){
        //put together the ACK
        byte[] seqNumBytes = ByteBuffer.allocate(2).putInt(41).array();

        ByteBuffer packetBuffer = ByteBuffer.allocate(12);
        packetBuffer.put(seqNumBytes);
        return packetBuffer.array();
    }

    /**
     *
     * @param fileByteArray bytes to be converted into a file
     * @param fileName the name of the file, complete with the extension
     * @return true if the file was successfully created, false if not along with the error printed to the console
     */
    public boolean bytesToFile(byte[] fileByteArray, String fileName){
        File newFile = new File(fileName);
        try(FileOutputStream bytesToFile = new FileOutputStream(fileName)){
            bytesToFile.write(fileByteArray);
            bytesToFile.close();
            return true;
        }
        catch (IOException e){
            System.out.println(e);
            return false;
        }
    }

    /**
     *
     * @param pathName path to the file to break down
     * @return returns a byte array filled with the bytes of the file
     */
    public byte[] fileToBytes(String pathName){
        Path path = Paths.get(pathName);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        }
        catch (IOException e){
            System.out.println(e);
        }
        finally {
            return data;
        }
    }

    /**
     *
     * @param packetData byte[] that should come from the Datagram packet's payload
     * @param offset int value for in case the data doesn't start until a certain point, set as 0 if there is no offset
     */
    public void fillPacketBuffer(byte[] packetData, int offset){
        if(offset == 0){
            packetBuffer.add(packetData);
        }
        else {
            packetBuffer.add(Arrays.copyOfRange(packetData, offset, packetData.length));
        }
    }

    /**
     *
     * @return turns the packetBuffer ArrayList into one byte array and returns that
     */
    public byte[] packetBufferToByteArray(){
        ByteBuffer tempBuffer = ByteBuffer.allocate(packetBuffer.size() * payloadSize);
        for(int i = 0; i < packetBuffer.size(); i++){
            tempBuffer.put(packetBuffer.get(i));
        }
        return tempBuffer.array();
    }

    /**
     *
     * @param total total number of items, should not be 0
     * @param segmentSize numbers of items in each segment, should not be < 1
     * @return number of segments required, rounded up
     */
    public int numberOfSegments(int total, int segmentSize){

        if(total== 0 || segmentSize < 1){
            return 0; //incase an inputArray is given that has nothing in it or segment size is less than 1
        }
        else {
            return (total - 1) / segmentSize + 1; //(a - 1) / b + 1, fails if a = 0 or b < 1
        }

    }

    /**
     *
     * @param byteArray takes in a byte array that it will convert into an integer
     * @return the integer that the bytes in byteArray create
     */
    public int byteArrayToInteger(byte[] byteArray){

        return new BigInteger(byteArray).intValue();
    }


    /**
     *
     * @param byteArray byte array that contains the bytes to be made into a String
     * @param offset in case the data in the array does not start at the beginning of the payload
     * @return creates a String made from the bytes given in the byteArray input, intended to fetch the file name
     */
    public String getStringFromByteArray(byte[] byteArray, int offset){
        return new String(byteArray, offset, byteArray.length);
    }

    public byte[] createFileNamePacket(){
        return null;
    }

    public void close(){
        socket.close();

    }
}
