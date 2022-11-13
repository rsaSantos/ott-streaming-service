package org.onode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
        String nodeAddress = nodeSocket.getInetAddress().getHostAddress();
        System.out.println("[" + LocalDateTime.now() + "]: Connection from [\u001B[32m" + nodeAddress + "\u001B[0m]..." );
    }
}
