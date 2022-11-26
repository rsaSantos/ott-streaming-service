package org.onode.control;


import org.exceptions.UpdateNodeStateException;
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
    public static final Integer OP_READ = 0;
    public static final Integer OP_WRITE = 1;
    public static final Integer OP_CHANGE_STATE = 2;

    private final List<String> adjacents;
    private final ServerSocket serverSocket;
    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;
    private final Map<String, Thread> readerThreadsMap;
    private final Map<String, NodeReceiver<?>> nodeReceiverMap;
    private final ExecutorService threadPoolExecutor;

    private final LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue;


    public NodeController(
            List<String> adjacents,
            ServerSocket serverSocket,
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap,
            int corePoolSize,
            int maxPoolSize,
            int maxQueuedTasks
    )
    {
        this.serverSocket = serverSocket;
        this.connectionDataMap = connectionDataMap;
        this.nodeReceiverMap = new HashMap<>();
        this.readerThreadsMap = new HashMap<>();
        this.adjacents = new ArrayList<>(adjacents);

        // TODO: limit the size of the queue? then threads inserting data will have to wait...
        this.dataQueue = new LinkedBlockingQueue<>();

        // TODO: Thread pool for writing tasks ? Create pre-defined number of threads?
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

        // Setup reading threads/instances and writing instances.
        for(Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet())
        {
            String address = conEntry.getKey();
            DataInputStream dis = conEntry.getValue().second();

            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Processing threads for address [\u001B[32m" + address + "\u001B[0m]...");

                NodeReceiver<DataInputStream> nodeListener = new NodeReceiver<>(this.dataQueue, dis, address);
                this.nodeReceiverMap.put(address, nodeListener);

                Thread readingThread = new Thread(nodeListener, "read_" + address);
                this.readerThreadsMap.put(address, readingThread);
                System.out.println("[" + LocalDateTime.now() + "]: Reading thread created with ID='\u001B[32m" + readingThread.getName() + "\u001B[0m'.");

                System.out.println("[" + LocalDateTime.now() + "]: Starting reading thread...");
                readingThread.start();
            }
            catch (IOException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Failed to get input/output stream for address [" + address + "] .");
                e.printStackTrace();
            }
        }

        try
        {
            String address = "localhost";
            // Create receiver thread with server socket
            NodeReceiver<ServerSocket> nodeListener = new NodeReceiver<>(this.dataQueue, this.serverSocket ,"localhost");
            this.nodeReceiverMap.put(address, nodeListener);

            Thread readingThread = new Thread(nodeListener, "read_" + address);
            this.readerThreadsMap.put(address, readingThread);
            System.out.println("[" + LocalDateTime.now() + "]: Reading thread created with ID='\u001B[32m" + readingThread.getName() + "\u001B[0m'.");

            System.out.println("[" + LocalDateTime.now() + "]: Starting reading thread...");
            readingThread.start();
        }
        catch (SocketException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to create Node Receiver for server socket.");
        }

        // TODO: Streaming
        StreamingController streamingController = new StreamingController();

        // TODO: State
        NodeState nodeState = new NodeState();

        // Wait notification (or timeout) from reading queue updates in threads.
        while(!this.readerThreadsMap.isEmpty())
        {
            try
            {
                // Wait until some reading thread reads something...
                // TODO: (If more than one thread is reading from this queue consider using ConcurrentLinkedQueue.)
                System.out.println("[" + LocalDateTime.now() + "]: Main thread waiting for data...");
                Triplet<Integer, List<String>, Object> dataTriplet = this.dataQueue.take();
                Integer operation = dataTriplet.first();
                List<String> addresses = dataTriplet.second();
                System.out.println("[" + LocalDateTime.now() + "]: Main thread received data...");

                // From receiver threads
                if (Objects.equals(operation, OP_READ))
                {
                    String data = (String) dataTriplet.third();
                    if(data.equals(DELETE_ME))
                    {
                        for(String address : addresses)
                            this.deleteAddress(address);
                    }
                    else
                    {
                        // TODO: Add task for thread pool. (Update constructor!)
                        this.threadPoolExecutor.execute(new NodeTask(
                                NodeTask.TASK_PACKET,
                                addresses.get(0),
                                data,
                                new ArrayList<>(adjacents),
                                dataQueue
                        ));
                    }
                }
                // From worker threads
                else if (Objects.equals(operation, OP_WRITE)) 
                {
                    String data = (String) dataTriplet.third();

                    // Send data to addresses
                    for(String address : addresses) {
                        try
                        {
                            System.out.println("[" + LocalDateTime.now() + "]: Sending data to " + address);
                            System.out.println(data);
                            DataOutputStream dos = this.connectionDataMap.get(address).third();
                            dos.writeUTF(data);
                            dos.flush();
                        }
                        catch (IOException e)
                        {
                            System.err.println("[" + LocalDateTime.now() + "]: Error sending data to [" + address + "].");
                        }
                    }
                }
                // From worker threads
                else if (Objects.equals(operation, OP_CHANGE_STATE)) 
                {
                    try
                    {
                        nodeState.update(new Pair<>(addresses.get(0), dataTriplet.third()));
                    }
                    catch (UpdateNodeStateException e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteAddress(String address)
    {
        // Input stream already closed.
        // Delete instances of Thread, NodeReceiver and NodeWriterTCP.
        // Close socket.

        this.readerThreadsMap.remove(address);
        this.nodeReceiverMap.remove(address);

        Triplet<Socket, DataInputStream, DataOutputStream> conData = this.connectionDataMap.get(address);

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

        this.connectionDataMap.remove(address);
        System.out.println("[" + LocalDateTime.now() + "]: Removed host [" + address + "].");
    }

    // TODO: Test this...is it needed?
    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                // TODO: later coordinate this shutdown
                this.threadPoolExecutor.shutdown();

                for(Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet())
                {

                    DataOutputStream dos = conEntry.getValue().third();
                    try
                    {
                        dos.writeUTF(DELETE_ME);
                        dos.flush();
                        dos.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    DataInputStream dis = conEntry.getValue().second();
                    dis.close();

                    Socket socket = conEntry.getValue().first();
                    socket.close();
                }

                this.connectionDataMap.clear();
                this.readerThreadsMap.clear();
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
