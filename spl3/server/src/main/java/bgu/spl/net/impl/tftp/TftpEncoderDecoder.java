package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes;// check if length is correct****
    private int index = 0;
    private int op = 0;
    private int packetSize = 0;
    

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if(index == 0)
            bytes = new byte[518];

        if(index == 1)
            op = nextByte;
       
        if (op == 1 || op == 2 || op == 7 || op == 8) {
            if (nextByte == 0) {
                bytes[index] = nextByte;
                byte[] ret = buildArray(index+1);
                index = 0;
                op = 0;
                return ret;
            }
        }
        if (op == 3) {
            if (index == 3) {
                packetSize = byteToShort(new byte[]{nextByte, bytes[2]});
            }
            if (index == packetSize + 5) {
                bytes[index] = nextByte;
                byte[] ret = buildArray(index+1);
                index = 0;
                op = 0;
                packetSize = 0;
                return ret;
            }
        }
        if (op == 4) {
            if (index == 3) {
                bytes[index] = nextByte;
                byte[] ret = buildArray(index+1);
                index = 0;
                op = 0;
                return ret;
            }
        }
        if (op == 6 || op == 10) {
            bytes[index] = nextByte;
            byte[] ret = buildArray(index+1);
            index = 0;
            op = 0;
            return ret;
        }
        if (op == 9 || op == 5) {
            if (index > 3) {
                if (nextByte == 0) {
                    bytes[index] = nextByte;
                    byte[] ret = buildArray(index+1);
                    index = 0;
                    op = 0;
                    return ret;
                }
            }
        }
        bytes[index] = nextByte;
        index++;

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return (byte[]) message;
    }

    public short byteToShort(byte[] curr){
        short a = (short)(curr[0] & 0xFF);
        short b = (short)(curr[1] & 0xFF);

        return (short) (a << 8 | b);
    }

    public byte[] shortToByte(short size){
        return new byte[]{(byte)(size >> 8), (byte) (size & 0xff)};
    }


    public byte[] buildArray(int size){
        byte[] ret = new byte[size];
        for(int i=0; i<size; i++){
            ret[i] = bytes[i];
        }
        return ret;
    }
}