package org.onode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;

public class NodeConnectionTCP implements Runnable
{
    private final Socket nodeSocket;

    public NodeConnectionTCP(String address, int PORT) throws IOException
    {
        this.nodeSocket = new Socket(InetAddress.getByName(address), PORT);
    }

    @Override
    public void run()
    {
        try {
            while(!nodeSocket.isClosed())
            {
                System.out.println("[" + LocalDateTime.now() + "]: Connection with address[\u001B[32m" + this.getAddress() + "\u001B[0m] is open. Sleeping for 5s...");
                Thread.sleep(5000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void closeConnection()
    {
        try {
            this.nodeSocket.close();
            System.out.println("[" + LocalDateTime.now() + "]: Connection with address[\u001B[32m" + this.getAddress() + "\u001B[0m] was closed.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAddress()
    {
        return this.nodeSocket.getInetAddress().getHostAddress();
    }
}
