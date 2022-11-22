package org.onode.control;


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
    public static final String START_FLOOD = "START FLOOD";

    private final ServerSocket serverSocket;
    private final Map<String, Socket> socketsMap;

    private final Map<String, Thread> readerThreadsMap;
    private final Map<String, NodeListener<?>> listenerTCPMap;
    private final Map<String, DataInputStream> inputStreamMap;
    private final Map<String, DataOutputStream> outputStreamMap;

    private final ExecutorService threadPoolExecutor;

    public NodeController(
            ServerSocket serverSocket,
            Map<String, Socket> socketsMap,
            Map<String, DataInputStream> inputStreamMap,
            Map<String, DataOutputStream> outputStreamMap,
            int corePoolSize,
            int maxPoolSize,
            int maxQueuedTasks)
    {
        this.serverSocket = serverSocket;
        this.socketsMap = socketsMap;
        this.outputStreamMap = outputStreamMap;
        this.inputStreamMap = inputStreamMap;
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
        for(Map.Entry<String, DataInputStream> disEntry : this.inputStreamMap.entrySet())
        {
            String address = disEntry.getKey();
            DataInputStream dis = disEntry.getValue();

            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Processing threads for address [\u001B[32m" + address + "\u001B[0m]...");

                NodeListener<DataInputStream> nodeListener = new NodeListener<>(dis, address);
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
                for(Map.Entry<String, NodeListener<?>> readerTCP : this.listenerTCPMap.entrySet())
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
        // Delete instances of Thread, NodeListener and NodeWriterTCP.
        // Close socket.
        for(String address : addresses)
        {
            this.readerThreadsMap.remove(address);
            this.listenerTCPMap.remove(address);
            this.inputStreamMap.remove(address);

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
                Socket socket = this.socketsMap.get(address);
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
            this.socketsMap.remove(address);
        }
    }

    // TODO: Test this...is it needed?
    private void createShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try
            {
                // TODO: Delete thread pool and run sequentially.

                for(Map.Entry<String, DataInputStream> disEntry : this.inputStreamMap.entrySet())
                {
                    disEntry.getValue().close();
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

                for(Map.Entry<String, Socket> socketEntry : this.socketsMap.entrySet())
                {
                    socketEntry.getValue().close();
                }


                this.readerThreadsMap.clear();
                this.socketsMap.clear();
                this.listenerTCPMap.clear();
                this.inputStreamMap.clear();
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
