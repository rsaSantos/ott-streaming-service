package org.onode;

import org.onode.control.NodeController;
import org.onode.control.starter.StarterListener;
import org.onode.control.starter.StarterSender;
import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private static final int BOOTSTRAPPER_PORT = 25000;
    public static final int CONTROL_PORT = 25000 + 1;
    public static final int STREAMING_PORT = 25000 + 2;
    private static final String ALL_LIST = "ALL NODES HAVE THE LIST!";
    private static final String LISTENING = "LISTENING";
    private static final String ALL_LISTENING = "ALL NODES ARE LISTENING!";
    private static final String CONNECTED = "CONNECTED";
    private static final String ALL_CONNECTED = "ALL NODES ARE CONNECTED!";

    public static void main(String[] args)
    {
        try {
            System.out.println("[" + LocalDateTime.now() + "]: Openening TCP server socket...");
            ServerSocket serverSocket = new ServerSocket(CONTROL_PORT);
            System.out.println("[" + LocalDateTime.now() + "]: TCP server socket up and running at port " + CONTROL_PORT + ".");

            System.out.println("[" + LocalDateTime.now() + "]: Connecting to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            Socket socket = new Socket(BOOTSTRAPPER_IP, BOOTSTRAPPER_PORT);
            System.out.println("[" + LocalDateTime.now() + "]: Connected to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m].");

            System.out.println("[" + LocalDateTime.now() + "]: Opening DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            System.out.println("[" + LocalDateTime.now() + "]: DataInputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] up and running.");

            List<String> adjacents = getAdjacents(dis);

            int token = new Random().nextInt();
            System.out.println("[" + LocalDateTime.now() + "]: Generated token [\u001B[32m" + token + "\u001B[0m].");

            // Reading the message that signals that all nodes received their adjacent list.
            String permission = dis.readUTF();
            while(!permission.equals(ALL_LIST))
                permission = dis.readUTF();
            StarterListener starterListener = new StarterListener(serverSocket, new ArrayList<>(adjacents), token);
            Thread starterListenerThread = new Thread(starterListener);
            starterListenerThread.start();

            System.out.println("[" + LocalDateTime.now() + "]: Opening DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("[" + LocalDateTime.now() + "]: DataOutputStream to server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] up and running.");


            System.out.println("[" + LocalDateTime.now() + "]: Inform server we are waiting for neighbours to connect....");
            dos.writeUTF(LISTENING);
            dos.flush();

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for permission to start sending requests...");
            permission = dis.readUTF();
            while(!permission.equals(ALL_LISTENING))
                permission = dis.readUTF();
            System.out.println("[" + LocalDateTime.now() + "]: Starting to send requests...");

            StarterSender starterSender = new StarterSender(new ArrayList<>(adjacents), token);
            starterSender.run();

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for listener thread...");
            starterListenerThread.join();
            System.out.println("[" + LocalDateTime.now() + "]: Listener thread joined.");

            // TODO: Get final list of streams...
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> listenerConnectionDataMap = starterListener.getConnectionDataMap();
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> senderConnectionDataMap = starterSender.getConnectionDataMap();

            // Merge these maps...
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap = new HashMap<>();
            connectionDataMap.putAll(listenerConnectionDataMap);
            connectionDataMap.putAll(senderConnectionDataMap);

            // Clear old maps
            listenerConnectionDataMap.clear();
            senderConnectionDataMap.clear();

            System.out.println("[" + LocalDateTime.now() + "]: Inform server we are connected to all neighbours.");
            dos.writeUTF(CONNECTED);
            dos.flush();

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for ALL the connections to be made...");
            permission = dis.readUTF();
            while(!permission.equals(ALL_CONNECTED))
                permission = dis.readUTF();
            System.out.println("[" + LocalDateTime.now() + "]: ALL the connections were made!");

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
                    new ArrayList<>(adjacents),
                    serverSocket,
                    connectionDataMap,
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