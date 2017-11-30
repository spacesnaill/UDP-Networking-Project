package frostburg.pjgeiger0.com;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;

public class Main {



    public static void main(String[] args) {
        SAW test = null;
        try {
            test = new SAW(22, "10.103.6.226", 40323);
        }
        catch(IOException e){
            System.out.println(e);
        }
        // write your code here

//        MyClient client;
////        BufferedReader br1 = null;
////        BufferedReader br2 = null;
//        try{
//            new MyServer(20, 0).start();
//            client = new MyClient(20, 0);
//            client.sendMessage();
////            String echo = client.sendMessage(sc.nextLine());
////            System.out.println(echo);
////            echo = client.sendMessage("server is working");
////            System.out.println(echo);
////            echo = client.sendMessage("end");
////            System.out.println(echo);
//            client.close();
////            br1 = new BufferedReader(new FileReader("client_input.txt"));
////            br2 = new BufferedReader(new FileReader("server_output.txt"));
////            int lineCount = 0;
////            int goodLines = 0;
////            String clientLine = br1.readLine();
////            String serverLine = br2.readLine();
////            while(clientLine != null){
////                lineCount = lineCount + 1;
////                if(clientLine.equals(serverLine)){
////                    goodLines = goodLines + 1;
////                }
////                clientLine = br1.readLine();
////                serverLine = br2.readLine();
////            }
////            System.out.println("Good lines: " + ((lineCount - goodLines)/lineCount));
////            br1.close();
////            br2.close();
//
//        }
//        catch(IOException e){
//            System.out.println(e);
//        }

    }

}
