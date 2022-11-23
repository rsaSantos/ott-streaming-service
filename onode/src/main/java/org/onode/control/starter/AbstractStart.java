package org.onode.control.starter;

import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractStart
{
    protected final static int ERROR = 0;
    protected final static int GOOD_DROP = 1;
    protected final static int GOOD_KEEP = 1;
    protected final static String OK = "OK";
    private final List<String> adjacents;
    private final int token;

    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;

    public AbstractStart(List<String> adjacents, int token)
    {
        this.adjacents = new ArrayList<>(adjacents);
        this.token = token;
        this.connectionDataMap = new HashMap<>();
    }

    protected void tryConnection(Socket socket) throws IOException
    {
        String address = socket.getInetAddress().getHostAddress();
        if(this.adjacents.contains(address))
        {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int testResult = this.testConnection(dis, dos);
            if (testResult == GOOD_KEEP)
            {
                Triplet<Socket, DataInputStream, DataOutputStream> connectionData = new Triplet<>(socket, dis, dos);
                this.connectionDataMap.put(address, connectionData);
                System.out.println("[" + LocalDateTime.now() + "]: Connection to host [\u001B[32m" + address + "\u001B[0m] successful.");
            }
            else
            {
                dis.close();
                dos.close();
                socket.close();
            }

            if(testResult != ERROR)
                this.adjacents.remove(address);
        }
    }

    abstract protected int testConnection(DataInputStream dis, DataOutputStream dos);

    public Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> getConnectionDataMap() {
        return connectionDataMap;
    }

    public List<String> getAdjacents()
    {
        return new ArrayList<>(this.adjacents);
    }

    public int getToken() {
        return token;
    }

    protected boolean isEmpty()
    {
        return this.adjacents.isEmpty();
    }
}
