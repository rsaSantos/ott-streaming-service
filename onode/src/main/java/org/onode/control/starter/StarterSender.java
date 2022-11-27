package org.onode.control.starter;

import org.onode.Main;
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

public class StarterSender implements Runnable, IStarter
{
    private final int token;
    private final List<String> adjacents;
    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;


    public StarterSender(List<String> adjacents, int token)
    {
        this.adjacents = adjacents;
        this.token = token;
        this.connectionDataMap = new HashMap<>();
    }

    @Override
    public void run()
    {
        List<String> iteratorAdjacents = new ArrayList<>(this.adjacents);
        for (String address : iteratorAdjacents)
        {
            while(true)
            {
                try
                {
                    Socket socket = new Socket(address, Main.CONTROL_PORT);
                    System.out.println("[" + LocalDateTime.now() + "]: Connected to host [" + address + "].");
                    this.tryConnection(socket);
                    break;
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error in start sender for address [" + address + "].");
                }
            }
        }
    }

    private void tryConnection(Socket socket) throws IOException
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
            System.out.println("[" + LocalDateTime.now() + "]: Sent token " + this.token + ".");
            dos.writeInt(this.token);
            dos.flush();
            int neighbourRandom = dis.readInt();
            System.out.println("[" + LocalDateTime.now() + "]: Received token " + neighbourRandom + ".");

            System.out.println("Comparing neighbour=" + neighbourRandom + ", with ours=" + this.token);
            if(neighbourRandom > this.token)
            {
                ret = GOOD_KEEP;
                dos.writeUTF(OK);
            }
            else
            {
                ret = GOOD_DROP;
                dos.writeUTF(NOT_OK);
            }
            dos.flush();
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
