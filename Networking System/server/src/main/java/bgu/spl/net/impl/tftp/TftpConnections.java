package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class TftpConnections<T> implements Connections<T> {
 
    public ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> clients = new ConcurrentHashMap<>();
    public List<Integer> clientsIds = new LinkedList<>();
    public List<String> clientsNames = new LinkedList<>();
    public List<Integer> toFix = new LinkedList<>();
    
    @Override
    public void connect(int connectionId, ConnectionHandler<byte[]> handler){
        clients.put(connectionId, handler);
        clientsIds.add(connectionId);
    }

    @Override
    public synchronized boolean send(int connectionId, T msg){
        ConnectionHandler<byte[]> currHandler = clients.get(connectionId);
        if(currHandler != null){
            currHandler.send((byte[])msg);
            return true;
        }
        return false;
    }
    
    @Override
    public void disconnect(int connectionId){
        clients.remove(connectionId);
        clientsIds.remove((Integer)connectionId);
    }

    public List<String> getNames(){
        return clientsNames;
    }

    public List<Integer> getIds(){
        return clientsIds;
    }   

    public void addName(String name){
        clientsNames.add(name);
    }
    
    public void removeName(String name){
        clientsNames.remove(name);
    }

    public List<Integer> getToFix(){
        return toFix;
    }

    public void addToFix(int x){
        toFix.add(x);
    }
}
