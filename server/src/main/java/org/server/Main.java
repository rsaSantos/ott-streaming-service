package org.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int PORT = 25000;

    private static final String ALL_LIST = "ALL NODES HAVE THE LIST!";

    private static final String LISTENING = "LISTENING";
    private static final String ALL_LISTENING = "ALL NODES ARE LISTENING!";
    private static final String CONNECTED = "CONNECTED";
    private static final String ALL_CONNECTED = "ALL NODES ARE CONNECTED!";

    public static void main(String[] args)
    {
        // Read the configuration file.
        System.out.println("[" + LocalDateTime.now() + "]: Reading config file...");
        Overlay overlay = new Overlay("target/classes/config.json");
        System.out.println("[" + LocalDateTime.now() + "]: Config file processed successfully!");

        // Get number of nodes: all and critical.
        int criticalNodes = overlay.getCriticalNodes();

        // Sockets and output streams from all the initially connected nodes.
        List<Pair<Socket, DataOutputStream>> connectedNodes = new ArrayList<>();

        // Connect with all the nodes
        try
        {
            ServerSocket ss = new ServerSocket(PORT);
            while (criticalNodes > 0)
            {
                Socket nodeConnection = ss.accept();
                String address = nodeConnection.getInetAddress().getHostAddress();
                System.out.println("[" + LocalDateTime.now() + "]: Node " + "[\u001B[32m" + address + "\u001B[0m] has connected.");
                DataOutputStream outputStream = overlay.sendAdjacents(nodeConnection);
                
                if(outputStream != null)
                {
                    connectedNodes.add(new Pair<>(nodeConnection, outputStream));
                    System.out.println("[" + LocalDateTime.now() + "]: Sent adjacent list to node " + "[\u001B[32m" + address + "\u001B[0m].");
                }
                else
                {
                    System.out.println("[" + LocalDateTime.now() + "]: Adjacent list not sent to node " + "[\033[0;31m" + address + "\u001B[0m].");
                }
                
                if (overlay.isCritical(address))
                    criticalNodes--;                
            }

            // TODO (EXTRA): CRITICAL VS NON-CRITICAL NODES
            List<Pair<Boolean, DataInputStream>> inputStreams = new ArrayList<>();
            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                inputStreams.add(new Pair<>(true, new DataInputStream(node.getFirst().getInputStream())));
            }

            System.out.println("[" + LocalDateTime.now() + "]: All nodes have the list of adjacents.");
            informAllNodes(connectedNodes, ALL_LIST);

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for nodes to start listening...");
            waitAllNodes(inputStreams, LISTENING);
            System.out.println("[" + LocalDateTime.now() + "]: All nodes have started their listeners.");

            informAllNodes(connectedNodes, ALL_LISTENING);

            // Set the boolean values of the input streams to true (will need to iterate again)
            inputStreams.forEach(p -> p.setFirst(true));

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for nodes to be connected...");
            waitAllNodes(inputStreams, CONNECTED);
            System.out.println("[" + LocalDateTime.now() + "]: All nodes are connected.");

            informAllNodes(connectedNodes, ALL_CONNECTED);

            System.out.println("[" + LocalDateTime.now() + "]: Closing all the connections...");
            for(Pair<Boolean, DataInputStream> node : inputStreams)
            {
                node.getSecond().close();
            }

            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                node.getSecond().close();
                node.getFirst().close();
            }

            connectedNodes.clear();
            System.out.println("[" + LocalDateTime.now() + "]: Connections closed.");

            System.out.println("[" + LocalDateTime.now() + "]: Closing server...");
            ss.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitAllNodes(List<Pair<Boolean, DataInputStream>> inputStreams, String message) throws IOException
    {
        int i = inputStreams.size();
        while(i > 0)
        {
            for(Pair<Boolean, DataInputStream> node : inputStreams)
            {
                if(node.getFirst() && node.getSecond().available() > 0)
                {
                    String listening = node.getSecond().readUTF();
                    if(listening.equals(message))
                    {
                        node.setFirst(false);
                        i--;
                    }
                }
            }
        }
    }

    private static void informAllNodes(List<Pair<Socket, DataOutputStream>> connectedNodes, String message) throws IOException
    {
        for(Pair<Socket, DataOutputStream> node : connectedNodes)
        {
            System.out.println("[" + LocalDateTime.now() + "]: Informing host with address " + "[\u001B[32m" + node.getFirst().getInetAddress().getHostAddress() + "\u001B[0m].");
            node.getSecond().writeUTF(message);
            node.getSecond().flush();
        }
    }
}