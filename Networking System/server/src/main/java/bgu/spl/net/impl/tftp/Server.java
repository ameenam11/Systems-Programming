
package bgu.spl.net.impl.tftp;

import java.io.Closeable;
import java.util.function.Supplier;

public interface Server<T> extends Closeable {

    /**
     * The main loop of the server, Starts listening and handling new clients.
     */
    void serve();

    /**
     *This function returns a new instance of a thread per client pattern server
     * @param port The port for the server socket
     * @param protocolFactory A factory that creats new MessagingProtocols
     * @param encoderDecoderFactory A factory that creats new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new Thread per client server
     */
    public static <T> Server<byte[]>  threadPerClient(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<TftpEncoderDecoder> encoderDecoderFactory) {

        return new TftpBaseServer(port, protocolFactory, encoderDecoderFactory) {};
    }


}
