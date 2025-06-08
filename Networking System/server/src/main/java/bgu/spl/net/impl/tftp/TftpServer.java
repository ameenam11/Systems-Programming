package bgu.spl.net.impl.tftp;

import java.io.File;

public class TftpServer {

    public static void main(String[] args) {
        // you can use any server...
        Server.threadPerClient(
                Integer.parseInt(args[0]), //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();

    }
}




