package org.server;

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
                System.out.println("Node " + "[\u001B[32m" + address + "\u001B[0m] has connected.");
                DataOutputStream outputStream = overlay.sendAdjacents(nodeConnection);
                
                if(outputStream != null)
                {
                    connectedNodes.add(new Pair<>(nodeConnection, outputStream));
                    System.out.println("Sent adjacent list to node " + "[\u001B[32m" + address + "\u001B[0m].");
                }
                else
                {
                    System.out.println("Adjacent list not sent to node " + "[\033[0;31m" + address + "\u001B[0m].");
                }
                
                if (overlay.isCritical(address))
                    criticalNodes--;                
            }

            // TODO (EXTRA): CRITICAL VS NON-CRITICAL NODES

            for(Pair<Socket, DataOutputStream> node : connectedNodes)
            {
                node.getSecond().writeUTF("DONE");
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