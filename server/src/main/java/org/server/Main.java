package org.server;

import org.server.streaming.Streaming;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final int BOOTSTRAPPER_PORT = 25000;
    private static final int CONTROL_PORT = 25000 + 1;
    public static final int INTERNAL_STREAMING_PORT = 25000 + 2;

    private static final String ALL_LIST = "ALL NODES HAVE THE LIST!";
    private static final String LISTENING = "LISTENING";
    private static final String ALL_LISTENING = "ALL NODES ARE LISTENING!";
    private static final String CONNECTED = "CONNECTED";
    private static final String ALL_CONNECTED = "ALL NODES ARE CONNECTED!";

    public static void main(String[] args)
    {
        String serverID = null;
        String node_1_address = null;
        boolean buildOverlay = true;
        if(args.length > 1)
        {
            serverID = args[0];
            node_1_address = args[1];
            if(args.length > 2)
                buildOverlay = Boolean.parseBoolean(args[2]);

            System.out.println("[" + LocalDateTime.now() + "]: Node Address is [" + node_1_address + "]. Build overlay? " + buildOverlay);
        }
        else
        {
            System.out.println("[" + LocalDateTime.now() + "]: Invalid arguments '" + Arrays.toString(args) + "'.");
            System.exit(1);
        }

        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(BOOTSTRAPPER_PORT);
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error creating server socket. Aborting...");
            System.exit(1);
        }

        if(buildOverlay)
            buildOverlay(ss);

        try
        {
            // TODO: Wait for nodes...needed? Maybe...
            System.out.println("[" + LocalDateTime.now() + "]: Waiting 3 seconds...");
            Thread.sleep(3000);
            System.out.println("[" + LocalDateTime.now() + "]: Trying to connect with node 1...");

            // Start flooding
            Socket socket = new Socket(node_1_address, CONTROL_PORT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("0;" + serverID + ";" + Instant.now().toEpochMilli());
            dos.flush();
            dos.close();
            socket.close();

            // Start stream
            String videoPath = "target/classes/movie.Mjpeg";
            if (Files.exists(Path.of(videoPath)))
            {
                Streaming streaming = null;
                while(true)
                {
                    if(streaming == null || !streaming.isStreamOn())
                    {
                        streaming = new Streaming(videoPath, node_1_address);
                        streaming.run();
                    }
                }
            }
            else
                System.err.println("[" + LocalDateTime.now() + "]: Video file does not exist.");

            System.out.println("[" + LocalDateTime.now() + "]: Closing server...");
            ss.close();
        }
        catch (IOException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void buildOverlay(ServerSocket ss)
    {
        // Read the configuration file.
        System.out.println("[" + LocalDateTime.now() + "]: Reading config file...");
        Overlay overlay = new Overlay("target/classes/test_medium_config.json");
        System.out.println("[" + LocalDateTime.now() + "]: Config file processed successfully!");

        // Get number of nodes: all and critical.
        int criticalNodes = overlay.getCriticalNodes();

        // Sockets and output streams from all the initially connected nodes.
        List<Pair<Socket, DataOutputStream>> connectedNodes = new ArrayList<>();

        // Connect with all the nodes
        try
        {
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
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
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