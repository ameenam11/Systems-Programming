package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.impl.tftp.TftpBaseServer;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private Connections<byte[]> connections;
    private int connectionId;
    private String clientName;
    private ConnectionHandler<byte[]> handler;
    int packetSize;
    public List<byte[]> readData = new LinkedList<>();
    public boolean ack;
    private volatile boolean loggedIn;
    int blockNumber;
    String filePath;
    private String WRQfileName;
    private String RRQfileName;
    private boolean dirq = false;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        handler = null;
        ack = false;
        blockNumber = 1;
        filePath = "C:\\Users\\amera\\Downloads\\Skeleton (1)\\Skeleton\\server\\Flies";
    }

    @Override
    public void process(byte[] message) { // update response
        int op = message[1];
        switch (op) {
            case 1:
                RRQ(message);
                break;
            case 2:
                WRQ(message);
                break;
            case 3:
                DATA(message);
                break;
            case 4:
                ACK(message);
                break;
            case 6:
                DIRQ(message);
                break;
            case 7:
                LOGRQ(message);
                break;
            case 8:
                DELRQ(message);
                break;
            case 10:
                DISC(message);
                break;

        }
    }

    public void RRQ(byte[] message) {
        if (loggedIn) {

            byte[] fileN = new byte[message.length - 3];
            for (int i = 0; i < fileN.length; i++) {
                fileN[i] = message[i + 2];
            }
            RRQfileName = new String(fileN);
            String path = filePath + "\\" + RRQfileName;
            File file1 = new File(path);
            try (FileInputStream file2 = new FileInputStream(path)) {
                if (file1.exists() && file1.isFile()) {
                    int read;
                    int len = (int) file1.length();
                    while (len > 0) {
                        byte[] curr;
                        if (len < 512) {
                            curr = new byte[len];
                        } else {
                            curr = new byte[512];
                        }

                        for (int i = 0; i < curr.length && (read = file2.read()) != -1; i++) {
                            curr[i] = (byte) read;
                        }
                        readData.add(curr);
                        len = len - 512;
                    }
                    blockNumber = 1;
                    prepareDataPacket(0);

                } else {
                    ERROR("File not found - RRQ DELRQ of non-existing file.".getBytes(), (byte) 1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            ERROR("User not logged in - Any opcode received before Login completes.".getBytes(), (byte) 6);
        }
    }

    public void WRQ(byte[] message) {
        if (loggedIn) {
            byte[] fName = new byte[message.length - 3];
            int i;
            for (i = 0; i < fName.length; i++) {
                fName[i] = message[i + 2];
            }
            WRQfileName = new String(fName, java.nio.charset.StandardCharsets.UTF_8);
            File file = new File(filePath, WRQfileName);
            if (file.exists() && file.isFile()) {
                ERROR("File already exists - File name exists on WRQ.".getBytes(), (byte) 5);
            } else {
                
                connections.send(connectionId, new byte[] { 0x0, 0x4, 0x0, 0x0 });
                
            }
        } else
            ERROR("User not logged in - Any opcode received before Login completes.".getBytes(), (byte) 6);
    }

    public byte[] getData(byte[] curr) {
        byte[] ret = new byte[curr.length - 6];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = curr[i + 6];
        }
        return ret;
    }

    public void DATA(byte[] message) {
        readData.add(getData(message));
        short size = byteToShort(new byte[] { message[2], message[3] });
        if (size < 512) {
            int totalLength = 0;
            for (byte[] b : readData) {
                totalLength += b.length;
            }
            byte[] packetsOfData = new byte[totalLength];
            int index = 0;
            for (byte[] packet : readData) {
                for (int i = 0; i < packet.length; i++) {
                    packetsOfData[index] = packet[i];
                    index++;
                }
            }

            File file = new File(filePath, WRQfileName);
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
            try (FileOutputStream outputStream = new FileOutputStream(filePath + "\\" + WRQfileName)) {
                outputStream.write(packetsOfData);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // BCAST
            byte[] fileNameBytes = WRQfileName.getBytes();
            byte[] bCastMsg = new byte[4 + fileNameBytes.length];
            bCastMsg[0] = 0x0;
            bCastMsg[1] = 0x9;
            bCastMsg[2] = 0x1; // file has been added
            for (int j = 0; j < fileNameBytes.length; j++) {
                bCastMsg[j + 3] = fileNameBytes[j];
            }
            bCastMsg[bCastMsg.length - 1] = 0x0;
            BCAST(bCastMsg);

            System.out.println("WRQ " + WRQfileName + " complete");
            blockNumber = 0;
            readData.clear();
            
        } else {
            blockNumber = byteToShort(new byte[] { message[5], message[4] });
            byte[] block = shortToByte((short) blockNumber);
            connections.send(connectionId, new byte[] { 0x0, 0x4, block[0], block[1] });
        }
    }

    public void ACK(byte[] message) {
        blockNumber = byteToShort(new byte[] { message[3], message[2] });
        if (blockNumber == 0) {
            ack = true;
        }
        if (blockNumber > 0) {
            prepareDataPacket(blockNumber);
        }
    }

    public void ERROR(byte[] message, byte errCode) {
        byte[] errMsg = new byte[message.length + 5];
        errMsg[0] = 0x0;
        errMsg[1] = 0x5;
        errMsg[2] = 0x0;
        errMsg[3] = errCode;
        for (int i = 0; i < message.length; i++) {
            errMsg[i + 4] = message[i];
        }
        errMsg[errMsg.length - 1] = 0x0;
        handler.send(errMsg);
    }

    public void DIRQ(byte[] message) {
        if (loggedIn) {
            dirq = true;
            File directory = new File(filePath);

            File[] files = directory.listFiles();

            int totalSize = 0;
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.getName().getBytes().length;
                }
            }
            int i = 0;
            int j = 0;
            byte[] tmp = new byte[totalSize + files.length];
            for (File file : files) {
                i = 0;
                if (file.isFile()) {
                    byte[] fileName = file.getName().getBytes();
                    while (i < fileName.length) {
                        if (j == 512) {
                            readData.add(tmp);
                            j = 0;
                        } else {
                            tmp[j] = fileName[i];
                            j++;
                            i++;
                        }

                    }
                    tmp[j] = 0x0;
                    j++;
                }
            }
            byte[] last = new byte[j];
            for (int k = 0; k < j; k++) {
                last[k] = tmp[k];
            }
            if (j > 0) {
                readData.add(last);
            }
            prepareDataPacket(0);
            dirq = false;
        } else
            ERROR("User not logged in - Any opcode received before Login completes.".getBytes(), (byte) 6);

    }

    public void LOGRQ(byte[] message) {
        System.out.println(connectionId);
        byte[] cName = new byte[message.length - 3];
        int i;
        for (i = 0; i < cName.length; i++) {
            cName[i] = message[i + 2];
        }

        String name = new String(cName, java.nio.charset.StandardCharsets.UTF_8);

        if (!connections.getNames().contains(name) && !connections.getIds().contains(connectionId)) {
            loggedIn = true;
            connections.connect(connectionId, handler);
            clientName = name;
            connections.addName(name);

            byte[] ret = { 0x0, 0x4, 0x0, 0x0 };
            connections.send(connectionId, ret);
        } else {
            ERROR("User already logged in - Login username already connected.".getBytes(), (byte) 7);
        }
    }

    public void DELRQ(byte[] message) {
        if (loggedIn) {
            byte[] fName = new byte[message.length - 3];
            int i;
            for (i = 0; i < fName.length; i++) {
                fName[i] = message[i + 2];
            }
            String fileName = new String(fName, java.nio.charset.StandardCharsets.UTF_8);
            File file = new File(filePath + "\\" + fileName);
            String errMsg = null;
            if (file.exists()) {
                // Attempt to delete the file
                if (file.delete()) {
                    // ACK
                    connections.send(connectionId, new byte[] { 0x0, 0x4, 0x0, 0x0 });

                    // BCAST
                    byte[] fileNameBytes = fileName.getBytes();
                    byte[] bCastMsg = new byte[4 + fileNameBytes.length];
                    bCastMsg[0] = 0x0;
                    bCastMsg[1] = 0x9;
                    bCastMsg[2] = 0x0; // file has been deleted
                    for (int j = 0; j < fileNameBytes.length; j++) {
                        bCastMsg[j + 3] = fileNameBytes[j];
                    }
                    bCastMsg[bCastMsg.length - 1] = 0x0;
                    BCAST(bCastMsg);
                } else {
                    errMsg = "Access violation - File cannot be written, read or deleted.";
                    ERROR(errMsg.getBytes(), (byte) 2);
                }
            } else {
                errMsg = "File not found - RRQ DELRQ of non-existing file.";
                ERROR(errMsg.getBytes(), (byte) 1);
            }
        } else
            ERROR("User not logged in - Any opcode received before Login completes.".getBytes(), (byte) 6);
    }

    public void BCAST(byte[] message) {
        for (int id : connections.getIds()) {
            connections.send(id, message);
        }
    }

    public void DISC(byte[] message) {
        if (loggedIn) {
            connections.send(connectionId, new byte[] { 0x0, 0x4, 0x0, 0x0 });
            loggedIn = false;
            shouldTerminate = true;
            ((TftpBlockingConnectionHandler) handler).connected = false;
            connections.removeName(clientName);
            connections.disconnect(connectionId);
        } else {
            ERROR("User not logged in - Any opcode received before Login completes.".getBytes(), (byte) 6);
        }

    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void setCH(TftpBlockingConnectionHandler handler) {
        this.handler = handler;
    }

    public short byteToShort(byte[] curr) {
        short a = (short) (curr[0] & 0xFF);
        short b = (short) (curr[1] & 0xFF);

        return (short) (b << 8 | a);
    }

    public byte[] shortToByte(short size) {
        return new byte[] { (byte) (size >> 8), (byte) (size & 0xff) };
    }

    public void prepareDataPacket(int b) {
        byte[] tmp = readData.get(b);
        byte[] size = shortToByte((short) tmp.length);
        byte[] block = shortToByte((short) blockNumber);

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
            if (!dirq)
                System.out.println("RRQ " + RRQfileName + " complete");
            readData.clear();
            blockNumber = 0;
        }
        connections.send(connectionId, ans);
    }

}
