package org.onode.control.starter;

import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

public class StarterListener implements Runnable, IStarter
{
    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;
    private final ServerSocket serverSocket;
    private final int token;
    private final List<String> adjacents;


    public StarterListener(ServerSocket serverSocket, List<String> adjacents, int token)
    {
        this.token = token;
        this.serverSocket = serverSocket;
        this.connectionDataMap = new HashMap<>();
        this.adjacents = adjacents;
    }

    @Override
    public void run()
    {
        while(!this.adjacents.isEmpty())
        {
            try
            {
                Socket socket = this.serverSocket.accept();
                System.out.println("[" + LocalDateTime.now() + "]: Connected to host [\u001B[32m" + socket.getInetAddress().getHostAddress() + "\u001B[0m].");
                this.tryConnection(socket);
            }
            catch (IOException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Error in start listener...");
            }
        }
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
            else if (testResult == GOOD_DROP)
            {
                dis.close();
                dos.close();
                socket.close();
            }

            if(testResult != ERROR)
                this.adjacents.remove(address);
        }
    }

    protected int testConnection(DataInputStream dis, DataOutputStream dos)
    {
        int ret = ERROR;
        try
        {
            int neighbourRandom = dis.readInt();
            System.out.println("[" + LocalDateTime.now() + "]: Received token " + neighbourRandom + ".");

            dos.writeInt(this.token);
            dos.flush();
            System.out.println("[" + LocalDateTime.now() + "]: Sent token " + this.token + ".");
            String ok = dis.readUTF();

            System.out.println("Comparing neighbour=" + neighbourRandom + ", with ours=" + this.token);
            if(ok.equals(OK) && neighbourRandom <= this.token)
                ret = GOOD_KEEP;
            else if(ok.equals(NOT_OK))
                ret = GOOD_DROP;
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error testing start listener connection.");
        }

        return ret;
    }

    public Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> getConnectionDataMap() {
        return connectionDataMap;
    }
}
