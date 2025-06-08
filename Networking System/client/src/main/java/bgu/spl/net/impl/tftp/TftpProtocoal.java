package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.MessagingProtocol;

public class TftpProtocoal implements MessagingProtocol<byte[]> {
    public String fileName;
    private boolean shouldTerminate = false;
    private int connectionId;
    public volatile byte[] response;
    private int packetSize;
    public List<byte[]> data = new LinkedList<>();
    public boolean ack;
    private int blockNumber = 0;
    String filePath = "C:\\Users\\amera\\Downloads\\Skeleton (1)\\Skeleton\\client";
    public boolean dirq;
    public boolean wrq;
    public boolean loggedIn;

    @Override
    public byte[] process(byte[] message) { // update response
        int op = message[1];
        if (op == 3) {
            DATA(message);
            byte[] block = new byte[]{(byte)(blockNumber >> 8), (byte) (blockNumber & 0xff)};
            return new byte[] { 0x0, 0x4, block[0], block[1]};
        } else if (op == 4) {
            return ACK(message);
        } else if (op == 5) {
            ERROR(message);
        } else if (op == 9) {
            BCAST(message);
        }
        return null;
    }

    public byte[] getData(byte[] curr) {
        byte[] ret = new byte[curr.length - 6];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = curr[i + 6];
        }
        return ret;
    }

    public void DATA(byte[] message) {
        data.add(getData(message));
        short size = byteToShort(new byte[]{message[2], message[3]});
        if ((size) < 512) {
            int totalLength = 0;
            for (byte[] b : data) {
                totalLength += b.length;
            }
            byte[] packetsOfData = new byte[totalLength];
            int index = 0;
            for (byte[] packet : data) {
                for (int i = 0; i < packet.length; i++) {
                    packetsOfData[index] = packet[i];
                    index++;
                }
            }

            if (dirq) {
                int fileCount = 1;
                int start = 0;
                int count = 0;
                for (int i = 0; i < totalLength; i++) {
                    if (packetsOfData[i] == 0) {
                        byte[] newArray = new byte[i - count];
                        while (count < i) {
                            newArray[start] = packetsOfData[count];
                            start++;
                            count++;
                        }
                        count++;
                        start = 0;
                        String name = new String(newArray);
                        System.out.println(name + " " + fileCount);
                        fileCount++;
                    }
                }
                this.dirq = false;
            } else {
                File file = new File(filePath, fileName);
                try {
                    file.createNewFile();
                } catch (IOException e) {}

                try (FileOutputStream outputStream = new FileOutputStream(filePath + "\\" + fileName)) {
                    outputStream.write(packetsOfData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            blockNumber = 0;
            data.clear();
        }else{
            blockNumber++;
        }
    }

    public byte[] ACK(byte[] message) {
        if(!loggedIn){
            loggedIn = true;
        }
        byte[] ret = null;
        blockNumber = byteToShort(new byte[]{message[3], message[2]});
        System.out.println("ACK: " + blockNumber);
        if (blockNumber == 0) {
            if(wrq){
                ret = prepareDataPacket(blockNumber);
                wrq = false;
            }else{
                ack = true;
                ret = null;
            }
            
        }
        if (blockNumber > 0) {
            ret = prepareDataPacket(blockNumber);
        }
        return ret;
    }

    public void ERROR(byte[] message) {
        int errNum = (char) message[3];
        if(errNum == 5){
            data.clear();
            wrq = false;
        }
        byte[] tmp = new byte[message.length - 5];
        for (int i = 4; i < message.length - 1; i++) {
            tmp[i - 4] = message[i];
        }

        System.out.println("Error: " + errNum + " " + new String(tmp));
    }

    public void BCAST(byte[] message) {
        int actionNum = message[2];
        byte[] tmp = new byte[message.length - 4];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = message[i + 3];
        }

        if(actionNum == 1)
            System.out.println("BCAST: add " + new String(tmp));
        else
            System.out.println("BCAST: del " + new String(tmp));

    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void setFileName(String fName) {
        this.fileName = fName;
    }

    public short byteToShort(byte[] curr){
        short a = (short)(curr[0] & 0xFF);
        short b = (short)(curr[1] & 0xFF);

        return (short) (b << 8 | a);
    }
    public byte[] shortToByte(short size){
        return new byte[]{(byte)(size >> 8), (byte) (size & 0xff)};
    }

    public void pushInsideData(String name) {
        String path = filePath + "\\"+name;
        File file1 = new File(path);
        try (FileInputStream file2 = new FileInputStream(path)) {
            if (file1.exists() && file1.isFile()) {
                byte[] tmp = file2.readAllBytes();

                int read;
                int len = 0;
                while (len < tmp.length) {
                    byte[] curr;
                    if ((tmp.length - len) < 512) {
                        curr = new byte[tmp.length - len];
                    } else {
                        curr = new byte[512];
                    }

                    for (int i = 0; i < curr.length && len < tmp.length; i++) {
                        curr[i] = tmp[len];
                        len++;
                    }
                    data.add(curr);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] prepareDataPacket(int b) {
        byte[] tmp = data.get(b);
        byte[] size = shortToByte((short)tmp.length);
        byte[] block = shortToByte((short)(b+1));
        byte[] ans = new byte[tmp.length + 6];
        ans[0] = 0x0;
        ans[1] = 0x3;
        ans[2] = size[1];
        ans[3] = size[0];
        ans[4] = block[0];
        ans[5] = block[1];
        for (int i = 0; i < tmp.length; i++) {
            ans[i + 6] = tmp[i];
        }
        if (tmp.length < 512) {
            data.clear();
            blockNumber = 0;
        }
        return ans;
    }

}
