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
        System.out.println("[" + LocalDateTime.now().toString() + "]: Reading config file...");
        Overlay overlay = new Overlay("target/classes/config.json");
        System.out.println("[" + LocalDateTime.now().toString() + "]: Config file processed successfully!");

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

            List<Pair<Socket, DataInputStream>> inputStreams = new ArrayList<>();
            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                node.getSecond().writeUTF("ALL NODES HAVE THE LIST!");
                inputStreams.add(new Pair<>(node.getFirst(), new DataInputStream(node.getFirst().getInputStream())));
            }

            // TODO: Sleep for a few seconds while nodes connect to each other?

            while(!inputStreams.isEmpty())
            {
                List<Pair<Socket, DataInputStream>> toRemove = new ArrayList<>();
                for(Pair<Socket, DataInputStream> node : inputStreams)
                {
                    if(node.getSecond().available() > 0)
                    {
                        node.getSecond().close();
                        toRemove.add(node);
                    }
                }
                inputStreams.removeAll(toRemove);
            }

            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                node.getSecond().writeUTF("ALL NODES ARE CONNECTED!");
                node.getSecond().close();
                node.getFirst().close();
            }

            connectedNodes.clear();
            ss.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}