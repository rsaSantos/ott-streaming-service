package org.onode;

import org.onode.control.NodeReaderTCP;
import org.onode.control.NodeListener;

import java.io.DataInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Node
 */
public class Main
{
    public static final String BOOTSTRAPPER_IP = "10.0.20.10";
    public static final int PORT = 25000 + 1;

    private static List<String> getAdjacents()
    {
        List<String> adjacents = new ArrayList<>();
        try
        {
            // Establish connection to server
            System.out.println("[" + LocalDateTime.now() + "]: Connecting to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            Socket bt = new Socket(BOOTSTRAPPER_IP, 25000);
            System.out.println("[" + LocalDateTime.now() + "]: Connected to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m].");

            // Opening reading method from server conection
            System.out.println("[" + LocalDateTime.now() + "]: Opening DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            DataInputStream in = new DataInputStream(bt.getInputStream());
            System.out.println("[" + LocalDateTime.now() + "]: DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] up and running.");

            // Reading information
            System.out.println("[" + LocalDateTime.now() + "]: Receiving adjacent list from server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            String ips = in.readUTF();
            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list from server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] received.");

            // Process adjacent list
            System.out.println("[" + LocalDateTime.now() + "]: Processing adjacent list...");
            String[] ip_list = ips.split(",");

            Collections.addAll(adjacents, ip_list);
            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list processed. Result: " + adjacents.toString());

            // Reading the closing message...
            in.readUTF();

            // Close reading stream
            System.out.println("[" + LocalDateTime.now() + "]: Closing DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            in.close();
            System.out.println("[" + LocalDateTime.now() + "]: DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] successfully closed.");

            // Terminate the connection with server
            System.out.println("[" + LocalDateTime.now() + "]: Terminating connection with server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            bt.close();
            System.out.println("[" + LocalDateTime.now() + "]: Connection with server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] successfully terminated.");

        }
        catch (Exception e)
        {
            adjacents.clear();
            e.printStackTrace();
        }

        return adjacents;
    }

    public static void main(String[] args)
    {
        try {

            System.out.println("[" + LocalDateTime.now() + "]: Openening TCP server socket...");
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[" + LocalDateTime.now() + "]: TCP server socket up and running at port " + PORT + ".");

            // TODO: Streaming? Where to handle? Another thread? NodeListener?
            System.out.println("[" + LocalDateTime.now() + "]: Creating UDP socket...");
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            System.out.println("[" + LocalDateTime.now() + "]: UDP socket created and bound to port " + PORT + ".");

            // Get adjacents nodes IP's.
            List<String> adjacents = getAdjacents();

            // Open connections with all the neighbours.
            Map<String, Socket> nodeNeighboursSockets = new HashMap<>();
            for (String address : adjacents)
            {
                System.out.println("[" + LocalDateTime.now() + "]: Connecting to [\u001B[32m" + address + "\u001B[0m]..." );
                nodeNeighboursSockets.put(address, new Socket(InetAddress.getByName(address), PORT));
                System.out.println("[" + LocalDateTime.now() + "]: Connected to [\u001B[32m" + address + "\u001B[0m]." );
            }

            System.out.println("[" + LocalDateTime.now() + "]: Running node listener on main thread...");
            NodeListener nodeListener = new NodeListener(serverSocket, nodeNeighboursSockets);
            nodeListener.run(); // TODO: Sequential? Depends on the streaming...
            System.out.println("[" + LocalDateTime.now() + "]: Node listener exited.");

            System.out.println("[" + LocalDateTime.now() + "]: Closing the server socket...");
            if(!serverSocket.isClosed()) serverSocket.close();
            System.out.println("[" + LocalDateTime.now() + "]: Server socket closed.");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}