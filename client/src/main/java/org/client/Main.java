package org.client;

import java.time.LocalDateTime;

public class Main {

    public static final int STREAMING_PORT = 25000 + 2;
    public static final int CONTROL_PORT = 25000 + 1;

    public static void main(String[] args) throws InterruptedException
    {
        if(args.length != 2)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Please insert the node IP address as command line argument.");
            System.exit(1);
        }

        // Receives node IP via command line argument.
        String myAddress = args[0];
        String nodeAddress = args[1];

        // Create and run stream client
        StreamingClient streamingClient = new StreamingClient(myAddress, nodeAddress);
        streamingClient.run();
    }
}