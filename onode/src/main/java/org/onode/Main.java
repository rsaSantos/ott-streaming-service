package org.onode;

import org.onode.control.ConnectionStarter;
import org.onode.control.NodeController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    public static void main(String[] args)
    {
        try {
            System.out.println("[" + LocalDateTime.now() + "]: Openening TCP server socket...");
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[" + LocalDateTime.now() + "]: TCP server socket up and running at port " + PORT + ".");

            System.out.println("[" + LocalDateTime.now() + "]: Connecting to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            Socket socket = new Socket(BOOTSTRAPPER_IP, 25000);
            System.out.println("[" + LocalDateTime.now() + "]: Connected to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m].");

            System.out.println("[" + LocalDateTime.now() + "]: Opening DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            System.out.println("[" + LocalDateTime.now() + "]: DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] up and running.");

            List<String> adjacents = getAdjacents(dis);

            // Reading the message that signals that all nodes received their adjacent list.
            dis.readUTF();

            System.out.println("[" + LocalDateTime.now() + "]: All nodes received adjacent list. Creating connection starter threads...");
            ConnectionStarter connectionStarter = new ConnectionStarter(serverSocket, adjacents);
            connectionStarter.run();


            // Get sockets...
            Map<String, Socket> socketMap = connectionStarter.getSockets();

            System.out.println("[" + LocalDateTime.now() + "]: Opening DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("[" + LocalDateTime.now() + "]: DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] up and running.");

            System.out.println("[" + LocalDateTime.now() + "]: Write to server (DONE).");
            dos.writeUTF("DONE");

            // TODO: Fail safe with timeout handling
            System.out.println("Waiting for ALL the connections to be made...");
            dis.readUTF();
            System.out.println("ALL the connections were made!");

            // Close reading stream
            System.out.println("[" + LocalDateTime.now() + "]: Closing DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            dis.close();
            System.out.println("[" + LocalDateTime.now() + "]: DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] successfully closed.");

            // Close reading stream
            System.out.println("[" + LocalDateTime.now() + "]: Closing DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            dos.close();
            System.out.println("[" + LocalDateTime.now() + "]: DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] successfully closed.");

            // Terminate the connection with server
            System.out.println("[" + LocalDateTime.now() + "]: Terminating connection with server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            socket.close();
            System.out.println("[" + LocalDateTime.now() + "]: Connection with server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] successfully terminated.");


            // -------------------------------------------------
            // -------------------------------------------------


            // TODO: Streaming? Where to handle? Another thread? NodeController?
            System.out.println("[" + LocalDateTime.now() + "]: Creating UDP socket...");
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            System.out.println("[" + LocalDateTime.now() + "]: UDP socket created and bound to port " + PORT + ".");


            // Hyperparameters for thread pool
            System.out.println("[" + LocalDateTime.now() + "]: Setting hyper parameters for thread pool...");
            int maxPoolSize = Math.max(adjacents.size(), 2);
            int corePoolSize = 2;
            int maxQueuedTasks = 128;
            System.out.println("--------------------------------");
            System.out.println("maxPoolSize    = " + maxPoolSize);
            System.out.println("corePoolSize   = " + corePoolSize);
            System.out.println("maxQueuedTasks = " + maxQueuedTasks);
            System.out.println("--------------------------------");

            System.out.println("[" + LocalDateTime.now() + "]: Running node listener on main thread...");
            NodeController nodeController = new NodeController(
                    serverSocket,
                    socketMap,
                    corePoolSize,
                    maxPoolSize,
                    maxQueuedTasks
            );
            nodeController.run(); // TODO: Sequential? Depends on the streaming...
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

    private static List<String> getAdjacents(DataInputStream dis)
    {
        List<String> adjacents = new ArrayList<>();
        try
        {
            // Reading information
            System.out.println("[" + LocalDateTime.now() + "]: Receiving adjacent list from server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            String ips = dis.readUTF();
            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list from server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] received.");

            // Process adjacent list
            System.out.println("[" + LocalDateTime.now() + "]: Processing adjacent list...");
            String[] ip_list = ips.split(",");
            Collections.addAll(adjacents, ip_list);
            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list processed. Result: " + adjacents);
        }
        catch (Exception e)
        {
            adjacents.clear();
            e.printStackTrace();
        }

        return adjacents;
    }
}