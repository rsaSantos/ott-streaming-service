package org.onode.control;


import org.onode.control.reader.NodeReader;
import org.onode.control.writer.NodeTask;
import org.onode.utils.Triplet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NodeController implements Runnable
{
    public static final String DELETE_ME = "8D3rL2=E?2T.-E!"; // Some random string

    private final ServerSocket serverSocket;
    private final Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap;
    private final Map<String, Thread> readerThreadsMap;
    private final Map<String, NodeReader<?>> listenerTCPMap;
    private final ExecutorService threadPoolExecutor;

    public NodeController(
            ServerSocket serverSocket,
            Map<String, Triplet<Socket, DataInputStream, DataOutputStream>> connectionDataMap,
            int corePoolSize,
            int maxPoolSize,
            int maxQueuedTasks)
    {
        this.serverSocket = serverSocket;
        this.connectionDataMap = connectionDataMap;
        this.listenerTCPMap = new HashMap<>();
        this.readerThreadsMap = new HashMap<>();

        // TODO: Thread pool for writing tasks ? Create pre-defined number of threads?
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
        for(Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet())
        {
            String address = conEntry.getKey();
            DataInputStream dis = conEntry.getValue().second();

            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Processing threads for address [\u001B[32m" + address + "\u001B[0m]...");

                NodeReader<DataInputStream> nodeListener = new NodeReader<>(dis, address);
                this.listenerTCPMap.put(address, nodeListener);

                Thread readingThread = new Thread(nodeListener, "read_" + address);
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
                synchronized (this)
                {
                    System.out.println("[" + LocalDateTime.now() + "]: Main thread waiting to wake up...");
                    wait(60000); // 1 minute
                    System.out.println("[" + LocalDateTime.now() + "]: Main thread woke up!");
                }

                // Check all reading queues...
                List<String> addressesToDelete = new ArrayList<>();
                for(Map.Entry<String, NodeReader<?>> readerTCP : this.listenerTCPMap.entrySet())
                {
                    String data = readerTCP.getValue().readData();
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
        // Delete instances of Thread, NodeReader and NodeWriterTCP.
        // Close socket.
        for(String address : addresses)
        {
            this.readerThreadsMap.remove(address);
            this.listenerTCPMap.remove(address);

            Triplet<Socket, DataInputStream, DataOutputStream> conData = this.connectionDataMap.get(address);

            DataOutputStream dos = conData.third();
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

            Socket socket = conData.first();
            try
            {
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

            this.connectionDataMap.remove(address);
        }
    }

    // TODO: Test this...is it needed?
    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                // TODO: Delete thread pool and run sequentially.

                for(Map.Entry<String, Triplet<Socket, DataInputStream, DataOutputStream>> conEntry : this.connectionDataMap.entrySet())
                {
                    DataInputStream dis = conEntry.getValue().second();
                    dis.close();

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

                    Socket socket = conEntry.getValue().first();
                    socket.close();
                }

                this.connectionDataMap.clear();
                this.readerThreadsMap.clear();
                this.listenerTCPMap.clear();
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
