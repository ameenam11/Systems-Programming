package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class TftpClient {

    public static void main(String[] args) throws IOException {
        Scanner t = new Scanner(System.in);
        MessageEncoderDecoder<byte[]> encdec = new TftpEncoderDeccoder();
        MessagingProtocol<byte[]> protocol = new TftpProtocoal();
        boolean connected = false;

        if (args.length == 0) {
            args = new String[] { "localhost", "07ameen0" };
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, message");
            System.exit(1);
        }

        Socket sock = new Socket(args[0], 7777);
        BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());

        Thread listeningThread = new Thread(() -> {
            try {
                int read;
                while(!Thread.currentThread().isInterrupted() && (read = in.read()) >=0 ){
                    byte[] nextMessage = encdec.decodeNextByte((byte)read);
                    if(nextMessage != null){
                        byte[] tmp = protocol.process(nextMessage);
                        if(tmp != null){
                            out.write(encdec.encode(tmp));
                            out.flush();
                        }
                    }
                }
            
            }catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Listening thread terminated.");
        });
        listeningThread.start();

        Thread keyboardThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String command = t.nextLine();
                    String[] words = command.split(" ");

                    if (words[0].equals("LOGRQ")) {
                        byte[] tmp = new byte[words[1].getBytes().length + 3];
                        tmp[0] = 0x0;
                        tmp[1] = 0x7;
                        for (int i = 0; i < words[1].getBytes().length; i++) {
                            tmp[i + 2] = words[1].getBytes()[i];
                        }
                        tmp[tmp.length - 1] = 0x0;
                        out.write(tmp);
                        out.flush();
                    }

                    else if (words[0].equals("DELRQ")) {
                        byte[] tmp = new byte[words[1].getBytes().length + 3];
                        tmp[0] = 0x0;
                        tmp[1] = 0x8;
                        for (int i = 0; i < words[1].getBytes().length; i++) {
                            tmp[i + 2] = words[1].getBytes()[i];
                        }
                        tmp[tmp.length - 1] = 0x0;
                        out.write(tmp);
                        out.flush();
                    }

                    else if (words[0].equals("RRQ")) {
                        ((TftpProtocoal)protocol).setFileName(words[1]);
                        byte[] tmp = new byte[words[1].getBytes().length + 3];
                        tmp[0] = 0x0;
                        tmp[1] = 0x1;
                        for (int i = 0; i < words[1].getBytes().length; i++) {
                            tmp[i + 2] = words[1].getBytes()[i];
                        }
                        tmp[tmp.length - 1] = 0x0;
                        out.write(tmp);
                        out.flush();
                    }

                    else if (words[0].equals("WRQ")) {
                        ((TftpProtocoal)protocol).wrq = true;
                        byte[] tmp = new byte[words[1].getBytes().length + 3];
                        ((TftpProtocoal)protocol).pushInsideData(words[1]);
                        tmp[0] = 0x0;
                        tmp[1] = 0x2;
                        for (int i = 0; i < words[1].getBytes().length; i++) {
                            tmp[i + 2] = words[1].getBytes()[i];
                        }
                        tmp[tmp.length - 1] = 0x0;
                        out.write(tmp);
                        out.flush();
                    }

                    else if (words[0].equals("DIRQ")) {
                        ((TftpProtocoal)protocol).dirq = true;
                        byte[] tmp = new byte[2];
                        tmp[0] = 0x0;
                        tmp[1] = 0x6;
                        out.write(tmp);
                        out.flush();
                    }

                    else if (words[0].equals("DISC")) {
                        byte[] tmp = new byte[2];
                        tmp[0] = 0x0;
                        tmp[1] = 0xA;
                        out.write(tmp);
                        out.flush();
                        if (((TftpProtocoal) protocol).loggedIn) {
                            try {
                                listeningThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            listeningThread.interrupt();
                            Thread.currentThread().interrupt();
                        }

                    }

                    else {
                        System.out.println("Invalid command.");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("KeyBoard thread terminated.");
            
        });
        keyboardThread.start();
        
    }

}
