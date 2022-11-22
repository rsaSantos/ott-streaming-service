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
            System.out.println("[" + LocalDateTime.now() + "]: All nodes have the list of adjacents.");

            List<Pair<Boolean, DataInputStream>> inputStreams = new ArrayList<>();
            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                System.out.println("[" + LocalDateTime.now() + "]: Informing host with address " + "[\u001B[32m" + node.getFirst().getInetAddress().getHostAddress() + "\u001B[0m].");
                node.getSecond().writeUTF("ALL NODES HAVE THE LIST!");
                node.getSecond().flush();
                inputStreams.add(new Pair<>(true, new DataInputStream(node.getFirst().getInputStream())));
            }

            // TODO: Sleep for a few seconds while nodes connect to each other?
            System.out.println("[" + LocalDateTime.now() + "]: Waiting for nodes to connect to each other...");

            int i = inputStreams.size();
            while(i > 0)
            {
                for(Pair<Boolean, DataInputStream> node : inputStreams)
                {
                    if(node.getFirst() && node.getSecond().available() > 0)
                    {
                        node.setFirst(false);
                        i--;
                    }
                }
            }

            System.out.println("[" + LocalDateTime.now() + "]: All nodes have connected to each other.");

            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                System.out.println("[" + LocalDateTime.now() + "]: Informing host with address " + "[\u001B[32m" + node.getFirst().getInetAddress().getHostAddress() + "\u001B[0m].");
                node.getSecond().writeUTF("ALL NODES ARE CONNECTED!");
                node.getSecond().flush();
            }


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
}