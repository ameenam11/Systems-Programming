package bgu.spl.net.srv;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface Connections<T> {
    public ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> clients = new ConcurrentHashMap<>();
    public List<Integer> clientsIds = new LinkedList<>();
    public List<String> clientsNames = new LinkedList<>();

    void connect(int connectionId, ConnectionHandler<byte[]> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    List<String> getNames();
    List<Integer> getIds();
    void addName(String name);
    void removeName(String name);
}
