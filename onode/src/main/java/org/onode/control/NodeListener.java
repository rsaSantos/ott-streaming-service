package org.onode.control;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NodeListener implements Runnable
{
    public static final String DELETE_ME = "8D3rL2=E?2T.-E!"; // Some random string
    public static final String START_FLOOD = "START FLOOD";

    private final ServerSocket serverSocket;
    private final Map<String, Socket> nodeNeighboursSockets;

    private final Map<String, Thread> readerThreadsMap;
    private final Map<String, NodeReaderTCP> readerTCPMap;
    private final Map<String, DataOutputStream> outputStreamMap;

    private final Condition isReadDataAvailable;
    private final ExecutorService threadPoolExecutor;

    public NodeListener(
            ServerSocket serverSocket,
            Map<String, Socket> nodeNeighboursSockets,
            int corePoolSize,
            int maxPoolSize,
            int maxQueuedTasks)
    {
        this.serverSocket = serverSocket;
        this.nodeNeighboursSockets = nodeNeighboursSockets;
        this.readerTCPMap = new HashMap<>();
        this.outputStreamMap = new HashMap<>();
        this.readerThreadsMap = new HashMap<>();

        // TODO: Thread pool for writing tasks ? Create pre-defined number of threads?
        this.isReadDataAvailable = new ReentrantLock().newCondition();

        threadPoolExecutor = new ThreadPoolExecutor(
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
        // TODO: Needed? How to effectively close connections? Send closing message?
        // Behaviour on exit...(closing sockets)
        this.createShutdownHook();

        // Setup reading threads/instances and writing instances.
        for(Map.Entry<String, Socket> socketEntry : this.nodeNeighboursSockets.entrySet())
        {
            String address = socketEntry.getKey();
            Socket socket = socketEntry.getValue();

            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Processing threads for address [\u001B[32m" + address + "\u001B[0m]...");

                NodeReaderTCP nodeReaderTCP = new NodeReaderTCP(this.isReadDataAvailable, new DataInputStream(socket.getInputStream()), address);
                this.readerTCPMap.put(address, nodeReaderTCP);

                this.outputStreamMap.put(address, new DataOutputStream(socket.getOutputStream()));

                Thread readingThread = new Thread(nodeReaderTCP, "read_" + address);
                this.readerThreadsMap.put(address, readingThread);
                System.out.println("[" + LocalDateTime.now() + "]: Reading thread created with ID='\u001B[32m" + readingThread.getName() + "\u001B[0m'.");

                System.out.println("[" + LocalDateTime.now() + "]: Starting reading thread...");
                readingThread.start();
            }
            catch (IOException e)
            {
                System.err.println("Failed to get input/output stream for address [" + address + "] .");
                e.printStackTrace();
            }
        }

        // Wait notification (or timeout) from reading queue updates in threads.
        while(!this.readerThreadsMap.isEmpty())
        {
            try
            {
                // Wait until some reading thread reads something...
                // TODO: Define optimal time...(current time for debugging only).
                this.isReadDataAvailable.await(5, TimeUnit.MINUTES);

                // Check all reading queues...
                List<String> addressesToDelete = new ArrayList<>();
                for(Map.Entry<String, NodeReaderTCP> readerTCP : this.readerTCPMap.entrySet())
                {
                    String data = readerTCP.getValue().readFromQueue();
                    if(data != null)
                    {
                        String address = readerTCP.getKey();
                        if(data.equals(DELETE_ME))
                        {
                            addressesToDelete.add(address);
                        }
                        else
                        {
                            // TODO: Add task for thread pool.
                            this.threadPoolExecutor.execute(new NodeTask());
                        }
                    }
                }
                if (!addressesToDelete.isEmpty())
                    this.deleteAddress(addressesToDelete);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteAddress(List<String> addresses)
    {
        // Input stream already closed.
        // Delete instances of Thread, NodeReaderTCP and NodeWriterTCP.
        // Close socket.
        for(String address : addresses)
        {
            this.readerThreadsMap.remove(address);
            this.readerTCPMap.remove(address);

            DataOutputStream dos = this.outputStreamMap.get(address);
            if(dos != null)
            {
                try
                {
                    dos.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error closing output stream for address [" + address + "].");
                    e.printStackTrace();
                }
            }
            this.outputStreamMap.remove(address);

            try
            {
                Socket socket = this.nodeNeighboursSockets.get(address);
                if(socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException e)
            {
                System.err.println("Error closing socket to address [" + address + "].");
                e.printStackTrace();
            }
            this.nodeNeighboursSockets.remove(address);
        }
    }

    // TODO: Test this...is it needed?
    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                // TODO: Delete thread pool and run sequentially.

                for(Map.Entry<String, NodeReaderTCP> readerTCPEntry : this.readerTCPMap.entrySet())
                {
                    readerTCPEntry.getValue().closeInputStream();
                }

                for(Map.Entry<String, DataOutputStream> dosEntry : this.outputStreamMap.entrySet())
                {
                    try
                    {
                        DataOutputStream dos = dosEntry.getValue();
                        dos.flush();
                        dos.writeUTF(DELETE_ME);
                        dos.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                for(Map.Entry<String, Socket> socketEntry : this.nodeNeighboursSockets.entrySet())
                {
                    socketEntry.getValue().close();
                }

                this.readerThreadsMap.clear();
                this.nodeNeighboursSockets.clear();
                this.readerTCPMap.clear();
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
