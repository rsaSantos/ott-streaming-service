package org.onode;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;

public class NodeListenerTCP implements Runnable
{
    private final ServerSocket serverSocket;

    public NodeListenerTCP(ServerSocket serverSocket)
    {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run()
    {
        while (true) {
            System.out.println("[" + LocalDateTime.now() + "]: Waiting connections...");
            try {
                serverSocket.accept();
            } catch (IOException e)
            {
                e.printStackTrace();
                break;
            }
        }

        System.out.println("[" + LocalDateTime.now() + "]: Listener thread going to sleep...");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
