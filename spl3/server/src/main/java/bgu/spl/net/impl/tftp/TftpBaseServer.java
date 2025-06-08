package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public class TftpBaseServer implements Server<byte[]> {

    private final int port;
    private final Supplier<TftpProtocol> protocolFactory;
    private final Supplier<TftpEncoderDecoder> encdecFactory;
    private ServerSocket sock;

    public TftpBaseServer(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<TftpEncoderDecoder> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.sock = null;


    }

    @Override
    public void serve() {
        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");

            this.sock = serverSock; // just to be able to close
            Connections<byte[]> connections = new TftpConnections<>();

            while (!Thread.currentThread().isInterrupted()) {
                
                Socket clientSock = serverSock.accept();
                ConnectionHandler<byte[]> handler = new TftpBlockingConnectionHandler(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        connections);

                execute((TftpBlockingConnectionHandler) handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    protected void execute(TftpBlockingConnectionHandler handler) {
        new Thread(handler).start();
    }

}
