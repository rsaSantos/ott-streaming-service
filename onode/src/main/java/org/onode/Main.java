package org.onode;

import org.onode.control.NodeController;
import org.onode.control.starter.StarterListener;
import org.onode.control.starter.StarterSender;
import org.onode.utils.Pair;
import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    public static final int STREAMING_PORT_EXTERNAL = 25000 + 2;
    public static final int STREAMING_PORT_LOCALHOST = 25000 + 3;
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

            Triplet<String, List<String>, List<String>> adjacentInfo = getAdjacents(dis);
            String myID = adjacentInfo.first();
            List<String> adjacentIDs = adjacentInfo.second();
            List<String> adjacentIPs = adjacentInfo.third();

            int token = new Random().nextInt();
            System.out.println("[" + LocalDateTime.now() + "]: Generated token [\u001B[32m" + token + "\u001B[0m].");

            // Reading the message that signals that all nodes received their adjacent list.
            String permission = dis.readUTF();
            while(!permission.equals(ALL_LIST))
                permission = dis.readUTF();
            StarterListener starterListener = new StarterListener(serverSocket, new ArrayList<>(adjacentIPs), token);
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

            StarterSender starterSender = new StarterSender(new ArrayList<>(adjacentIPs), token);
            starterSender.run();

            System.out.println("[" + LocalDateTime.now() + "]: Waiting for listener thread...");
            starterListenerThread.join();
            System.out.println("[" + LocalDateTime.now() + "]: Listener thread joined.");

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
            // Hyperparameters for thread pool
            System.out.println("[" + LocalDateTime.now() + "]: Setting hyper parameters for thread pool...");
            int maxPoolSize = Math.max(adjacentIDs.size(), 2);
            int corePoolSize = 2;
            int maxQueuedTasks = 128;
            System.out.println("--------------------------------");
            System.out.println("maxPoolSize    = " + maxPoolSize);
            System.out.println("corePoolSize   = " + corePoolSize);
            System.out.println("maxQueuedTasks = " + maxQueuedTasks);
            System.out.println("--------------------------------");

            System.out.println("[" + LocalDateTime.now() + "]: Running node listener on main thread...");
            NodeController nodeController = new NodeController(
                    myID,
                    adjacentIDs,
                    adjacentIPs,
                    serverSocket,
                    connectionDataMap,
                    corePoolSize,
                    maxPoolSize,
                    maxQueuedTasks
            );
            nodeController.run();
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

    private static Triplet<String, List<String>, List<String>> getAdjacents(DataInputStream dis)
    {
        // Info format: myID;adjacents;origins
        //  adjacents = id1:it0,id2:it0,id3:it1...
        //  origins   = ipX,idY;ipZ,idK;...
        //
        // Build the following structures:
        // - list with the adjacent nodes ID
        // - list with the adjacent nodes best IP
        // - list of triplets: nodeID, i#, address
        //
        // Return pair with list of ids and list of best ips.
        //
        String myID = null;
        List<String> adjacentIDs = new ArrayList<>();
        List<String> adjacentIPs = new ArrayList<>();
        List<Triplet<String, String, String>> nodeIntAddressInfo = new ArrayList<>();
        try
        {
            // Reading information
            System.out.println("[" + LocalDateTime.now() + "]: Receiving adjacent list from server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m]...");
            String info = dis.readUTF();
            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list from server [\u001B[32m" + BOOTSTRAPPER_IP + "\u001B[0m] received.");

            System.out.println("[" + LocalDateTime.now() + "]: Processing adjacent list...");

            // results = myID;origins;adjacents
            //  nodeID,i#,address;...   ;   O1:it0,O2:it0,O3:it2...
            String[] fragments = info.split(";");

            myID = fragments[0];

            // Build list of triplets
            int i;
            for(i = 1; i < fragments.length - 1; ++i)
            {
                String[] nodeIntAddress = fragments[i].split(",");
                nodeIntAddressInfo.add(new Triplet<>(nodeIntAddress[0], nodeIntAddress[1], nodeIntAddress[2]));
            }

            // Build list of adjacent id and best ip
            String[] adjacentsInfo = fragments[i].split(",");
            for(i = 0; i < adjacentsInfo.length; ++i)
            {
                String[] idAndInt = adjacentsInfo[i].split(":");
                String id = idAndInt[0];
                String bestInt = idAndInt[1];

                // Get address from node id and interface
                String address = null;
                for(Triplet<String, String, String> idIntAdd : nodeIntAddressInfo)
                {
                    if(idIntAdd.first().equals(id) && idIntAdd.second().equals(bestInt))
                    {
                        address = idIntAdd.third();
                        break;
                    }
                }

                if(address != null)
                {
                    adjacentIDs.add(id);
                    adjacentIPs.add(address);
                }
            }

            System.out.println("[" + LocalDateTime.now() + "]: Adjacent list processed.");
            System.out.println("Adjacent ID's: " + adjacentIDs);
            System.out.println("Adjacent IP's: " + adjacentIPs);
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error reading adjacent info from server.");
        }

        return new Triplet<>(myID, adjacentIDs, adjacentIPs);
    }
}