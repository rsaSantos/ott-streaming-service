package org.client;

import java.time.LocalDateTime;
import java.util.Arrays;

public class Main {

    public static final int CONTROL_PORT = 25000 + 1;

    public static void main(String[] args) throws InterruptedException
    {
        if(args.length != 2)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Please insert the node IP address and the clients interface IP as command line argument.");
            System.exit(1);
        }

        // Receives node IP via command line argument.
        String myAddress = args[0];
        String nodeAddress = args[1];

        System.out.println("[" + LocalDateTime.now() + "]: myAddress=" + myAddress + ", nodeAddress=" + nodeAddress);

        int STREAMING_PORT = 25000 + 2;
        if(nodeAddress.equals("localhost"))
        {
            STREAMING_PORT += 1;
            System.out.println("[" + LocalDateTime.now() + "]: STREAMING_PORT=" + STREAMING_PORT);
        }

        // Create and run stream client
        StreamingClient streamingClient = new StreamingClient(myAddress, nodeAddress, STREAMING_PORT);
        streamingClient.run();
    }
}