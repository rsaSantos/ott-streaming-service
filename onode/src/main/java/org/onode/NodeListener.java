package org.onode;

import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.List;

public class NodeListener implements Runnable
{
    private final ServerSocket serverSocket;
    private final List<NodeConnectionTCP> connectionsTCP;

    public NodeListener(ServerSocket serverSocket, List<NodeConnectionTCP> connectionsTCP)
    {
        this.serverSocket = serverSocket;
        this.connectionsTCP = connectionsTCP;
    }

    @Override
    public void run()
    {
        this.createShutdownHook();
        for(NodeConnectionTCP nodeConnectionTCP : this.connectionsTCP)
        {
            String address = nodeConnectionTCP.getAddress();
            System.out.println("[" + LocalDateTime.now() + "]: Creating thread for address[\u001B[32m" + address + "\u001B[0m].");
            Thread thread = new Thread(nodeConnectionTCP, address);
            System.out.println("[" + LocalDateTime.now() + "]: Thread created with ID='" + thread.getName() + "'.");
            thread.start();
        }

        // If the thread is not blocked, the socket will be closed...
        try
        {
            // TODO: Reading packets here!
            // Interpret and add to respective thread queue.

            // wait for connections with .accept() -> move to main


            // Sleeping for 292 billion years...should be enough.
            Thread.sleep(Long.MAX_VALUE);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                this.connectionsTCP.forEach(NodeConnectionTCP::closeConnection);
                this.serverSocket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }));
    }
}
