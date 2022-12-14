package org.onode.control;


import org.exceptions.UpdateNodeStateException;
import org.onode.control.packet.NodePacketGeneric;
import org.onode.control.reader.NodeReceiver;
import org.onode.control.writer.NodeTask;
import org.onode.streaming.StreamingController;
import org.onode.utils.Pair;
import org.onode.utils.Triplet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NodeController implements Runnable
{
    public static final String DELETE_ME = "8D3rL2=E?2T.-E!"; // Some random string
    public static final String IGNORE = "IgNoReMe"; // Some random string
    public static final int OP_READ = 0;
    public static final int OP_WRITE = 1;
    public static final int OP_CHANGE_STATE = 2;
    public static final int OP_ACTIVATE_STREAM = 3;
    public static final int OP_DEACTIVATE_STREAM = 4;
    private final List<Pair<String, String>> adjacent_ID_IP;
    private final String myID;
    private final ServerSocket serverSocket;
    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;
    private final Map<String, Thread> readerThreadsMap;
    private final Map<String, NodeReceiver<?>> nodeReceiverMap;
    private final ExecutorService threadPoolExecutor;
    private final LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue;


    public NodeController(
            String myID,
            List<String> adjacent_ID,
            List<String> adjacent_IP,
            ServerSocket serverSocket,
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap,
            int corePoolSize,
            int maxPoolSize,
            int maxQueuedTasks
    )
    {
        this.myID = myID;
        this.serverSocket = serverSocket;
        this.connectionDataMap = Collections.synchronizedMap(connectionDataMap);
        this.nodeReceiverMap = new HashMap<>();
        this.readerThreadsMap = new HashMap<>();

        this.adjacent_ID_IP = new ArrayList<>();
        for(int i = 0; i < adjacent_ID.size(); ++i)
            this.adjacent_ID_IP.add(new Pair<>(adjacent_ID.get(i), adjacent_IP.get(i)));

        this.dataQueue = new LinkedBlockingQueue<>();

        this.threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                5,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maxQueuedTasks));
    }

    // Run sequentially (in main thread).
    @Override
    public void run()
    {
        // Behaviour on exit...(closing sockets)
        this.createShutdownHook();

        synchronized (this.connectionDataMap)
        {
            // Setup reading threads/instances and writing instances.
            for (Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet())
            {
                String address = conEntry.getKey();
                try {
                    NodeReceiver<DataInputStream> nodeListener = new NodeReceiver<>(this.dataQueue, conEntry.getValue().second(), address);
                    startingReceiverThread(nodeListener, address);
                } catch (IOException e) {
                    System.err.println("[" + LocalDateTime.now() + "]: Failed to get input/output stream for address [" + address + "] .");
                    e.printStackTrace();
                }
            }
        }

        try
        {
            String address = "localhost";
            // Create receiver thread with server socket
            NodeReceiver<ServerSocket> nodeListener = new NodeReceiver<>(this.dataQueue, this.serverSocket ,"localhost");
            startingReceiverThread(nodeListener, address);
        }
        catch (SocketException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to create Node Receiver for server socket.");
        }

        // Streaming
        StreamingController streamingController = null;
        try
        {
            streamingController = new StreamingController();
            Thread streamingControllerThread = new Thread(streamingController, "StreamingController");
            streamingControllerThread.start();
        }
        catch (SocketException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to create streaming controller.");
        }
        assert streamingController != null;

        // Holds streaming state
        NodeState nodeState = new NodeState();

        // Wait notification (or timeout) from reading queue updates in threads.
        while(!this.readerThreadsMap.isEmpty()) {
            try {
                // Wait until some reading thread reads something...
                System.out.println("[" + LocalDateTime.now() + "]: Main thread waiting for data...");
                Triplet<Integer, List<String>, Object> dataTriplet = this.dataQueue.take();
                Integer operation = dataTriplet.first();
                List<String> addresses = dataTriplet.second();
                System.out.println("[" + LocalDateTime.now() + "]: Main thread received data...");

                // From receiver threads
                if (Objects.equals(operation, OP_READ))
                {
                    String data = (String) dataTriplet.third();
                    if (data.equals(DELETE_ME)) {
                        for (String address : addresses)
                            this.deleteAddress(address);
                    }
                    else
                    {
                        this.threadPoolExecutor.execute(new NodeTask(
                                NodeTask.TASK_PACKET,
                                addresses.get(0),
                                data,
                                this.myID,
                                this.adjacent_ID_IP,
                                this.dataQueue
                        ));
                    }
                }
                // From worker threads
                else if (Objects.equals(operation, OP_WRITE))
                {
                    // Send data to addresses
                    String data = (String) dataTriplet.third();
                    for (String address : addresses)
                        this.writeDataTo(data, address);
                }
                // From worker threads
                else if (Objects.equals(operation, OP_CHANGE_STATE))
                {
                    try
                    {
                        String lastBestNodeToReceive = nodeState.getBestToReceive();
                        nodeState.update(new Pair<>(addresses.get(0), dataTriplet.third()));
                        String currentBestToReceive = nodeState.getBestToReceive();

                        Pair<Boolean, String> streamingNodeInfo = streamingController.isStreaming();
                        Boolean isStreaming = streamingNodeInfo.first();
                        String streamingNode = streamingNodeInfo.second();

                        List<Object> data = (List<Object>) dataTriplet.third();
                        String serverID = (String) data.get(0);

                        System.out.println("-------------------");
                        System.out.println("ServerID=" + serverID);
                        System.out.println("flood received from= " + addresses.get(0));
                        System.out.println("lastBestNodeToReceive=" + lastBestNodeToReceive);
                        System.out.println("currentBestToReceive=" + currentBestToReceive);
                        System.out.println("streamSenderAddress=" + streamingNode);
                        System.out.println("isStreaming? " + isStreaming);
                        System.out.println("-------------------");

                        // Create activation/deactivation packets
                        String activatePacket = NodePacketGeneric.createActivatePacket(IGNORE);
                        String deactivatePacket = NodePacketGeneric.createDeactivatePacket(IGNORE);

                        // There is a node sending stream, but there is no record of a best sending node.
                        if(isStreaming && currentBestToReceive == null)
                            System.err.println("[" + LocalDateTime.now() + "]: Receiving unrequested stream.");

                        // There is a node sending stream, but it is not the best one.
                        else if(isStreaming && !streamingNode.equals(currentBestToReceive))
                        {
                            this.writeDataTo(deactivatePacket, streamingNode);
                            this.writeDataTo(activatePacket, currentBestToReceive);
                        }
                    }
                    catch (UpdateNodeStateException e) {
                        System.err.println(e.getMessage());
                    }
                }
                else if (Objects.equals(operation, OP_ACTIVATE_STREAM))
                {
                    // Add a new address to send the stream, get streaming node and best parent node.
                    streamingController.add(addresses.get(0));

                    Pair<Boolean, String> streamingNodeInfo = streamingController.isStreaming();
                    Boolean isStreaming = streamingNodeInfo.first();

                    String bestToReceive = nodeState.getBestToReceive();

                    // Create activation/deactivation packets
                    String activatePacket = NodePacketGeneric.createActivatePacket(IGNORE);

                    System.out.println("ACTIVATE PACKET");
                    System.out.println("activate node = " + addresses.get(0));
                    System.out.println("streaming node = " + streamingNodeInfo.second());
                    System.out.println("IsStreaming? " + streamingNodeInfo.first());
                    System.out.println("best to receive = " + bestToReceive);

                    // There is a node sending stream, but there is no record of a best sending node.
                    if(isStreaming && bestToReceive == null)
                        System.err.println("[" + LocalDateTime.now() + "]: Receiving unrequested stream.");

                    // There is no node sending stream, activate the best node.
                    else if(!isStreaming && bestToReceive != null)
                        this.writeDataTo(activatePacket, bestToReceive);
                }
                else if (Objects.equals(operation, OP_DEACTIVATE_STREAM))
                {
                    // Remove address from streaming list.
                    int nToSendStream = streamingController.remove(addresses.get(0));
                    Pair<Boolean, String> streamingNodeInfo = streamingController.isStreaming();
                    Boolean isStreaming = streamingNodeInfo.first();
                    String streamingNode = streamingNodeInfo.second();

                    System.out.println("DEACTIVATION PACKET");
                    System.out.println("nToSendStream = " + nToSendStream);
                    System.out.println("address that sent deactivation = " + addresses.get(0));
                    System.out.println("streamer node = " + streamingNode);
                    System.out.println("is streaming? " + isStreaming);

                    // If we have no more nodes to send, send deactivate packets to the node that is sending the stream.
                    if(nToSendStream == 0 && streamingNode != null)
                    {
                            // Create deactivation packet
                            String data = NodePacketGeneric.createDeactivatePacket(IGNORE);

                            // Send packet
                            this.writeDataTo(data, streamingNode);
                    }
                }
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeDataTo(String data, String address)
    {
        try {
            synchronized (this.connectionDataMap)
            {
                System.out.println("[" + LocalDateTime.now() + "]: Sending data to " + address);

                DataOutputStream dos = null;
                if(this.connectionDataMap.containsKey(address))
                {
                    dos = this.connectionDataMap.get(address).third();
                    dos.writeUTF(data);
                    dos.flush();
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error sending data to [" + address + "].");
        }
    }

    private void startingReceiverThread(NodeReceiver<?> nodeListener, String address)
    {
        this.nodeReceiverMap.put(address, nodeListener);

        Thread readingThread = new Thread(nodeListener, "read_" + address);
        this.readerThreadsMap.put(address, readingThread);
        System.out.println("[" + LocalDateTime.now() + "]: Reading thread created with ID='\u001B[32m" + readingThread.getName() + "\u001B[0m'.");

        System.out.println("[" + LocalDateTime.now() + "]: Starting reading thread...");
        readingThread.start();
    }

    private void deleteAddress(String address)
    {
        // Input stream already closed.
        // Delete instances of Thread, NodeReceiver and NodeWriterTCP.
        // Close socket.

        this.readerThreadsMap.remove(address);
        this.nodeReceiverMap.remove(address);

        Triplet<Socket, DataInputStream, DataOutputStream> conData = null;
        synchronized (this.connectionDataMap)
        {
             conData = this.connectionDataMap.get(address);
        }

        if(conData == null)
            return;

        DataOutputStream dos = conData.third();
        if(dos != null) {
            try {
                dos.close();
            } catch (IOException e) {
                System.err.println("Error closing output stream for address [" + address + "].");
                e.printStackTrace();
            }
        }

        Socket socket = conData.first();
        try {
            if (socket != null) {
                socket.close();
            }
        }
        catch (IOException e) {
            System.err.println("[" + LocalDateTime.now() + "]: Error closing socket to address [" + address + "].");
            e.printStackTrace();
        }

        synchronized (this.connectionDataMap)
        {
            this.connectionDataMap.remove(address);
        }
        System.out.println("[" + LocalDateTime.now() + "]: Removed host [" + address + "].");
    }

    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                this.threadPoolExecutor.shutdown();

                synchronized (this.connectionDataMap)
                {
                    for(Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet()) {

                        DataOutputStream dos = conEntry.getValue().third();
                        try {
                            dos.writeUTF(DELETE_ME);
                            dos.flush();
                            dos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        DataInputStream dis = conEntry.getValue().second();
                        dis.close();

                        Socket socket = conEntry.getValue().first();
                        socket.close();
                    }

                    this.connectionDataMap.clear();
                }

                this.nodeReceiverMap.clear();
                this.readerThreadsMap.clear();

                this.serverSocket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }));
    }
}
