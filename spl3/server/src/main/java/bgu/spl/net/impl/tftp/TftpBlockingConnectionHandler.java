package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpBlockingConnectionHandler implements Runnable, ConnectionHandler<byte[]> {

    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    public volatile boolean connected = true;
    public int id;
    public Connections<byte[]> connections;

    public boolean first;

    public TftpBlockingConnectionHandler(Socket sock, TftpEncoderDecoder reader,
            TftpProtocol protocol, Connections<byte[]> connections ) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        first = true;
        
    }

    @Override
    public void run() {
        synchronized (connections) {
            if (((TftpConnections<byte[]>)connections).getToFix().size() == 0) {
                this.id = 0;
            }else{
                this.id = ((LinkedList<Integer>) ((TftpConnections<byte[]>)connections).getToFix()).peekLast() + 1;
            }
            ((TftpConnections<byte[]>)connections).addToFix(id);
        }
        try (Socket sock = this.sock) { // just for automatic closing
            int read;
            protocol.start(id, connections);
            protocol.setCH(this);

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                byte[] nextMessage = encdec.decodeNextByte((byte)read);
                
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override


    public void send(byte[] msg) {
        try {
            if (msg != null) {
                out.write((byte[])encdec.encode(msg));
                out.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
