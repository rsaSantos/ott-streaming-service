package org.onode.control;

import org.onode.Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;

public class ConnectionStarter implements Runnable
{
    private static final int SOCKET_READ_TIMEOUT = 10000; // TODO: CHECK THIS -> 10s timeout for the read call...
    private final ServerSocket serverSocket;
    private final List<String> adjacents;
    private final Map<String, Socket> socketsMap;
    private final Map<String, DataOutputStream> outputStreamMap;
    private final Map<String, DataInputStream> inputStreamMap;

    public ConnectionStarter(ServerSocket serverSocket, List<String> adjacents)
    {
        this.serverSocket = serverSocket;
        this.adjacents = new ArrayList<>();
        this.adjacents.addAll(adjacents);
        this.socketsMap = new HashMap<>();
        this.outputStreamMap = new HashMap<>();
        this.inputStreamMap = new HashMap<>();
    }

    public void run()
    {
        int randomTimeoutInt = new Random().nextInt(10) + 1;
        System.err.println("[" + LocalDateTime.now() + "]: Socket timeout with random waiting time of " + randomTimeoutInt + ".");

        while(!this.adjacents.isEmpty())
        {
            String randomNeighbourAddress = this.adjacents.get(new Random().nextInt(this.adjacents.size()));
            String addressToRemove = null;
            Socket socket = null;

            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Trying to connect with host [" + randomNeighbourAddress + "]...");
                socket = new Socket(InetAddress.getByName(randomNeighbourAddress), Main.PORT);
                System.out.println("[" + LocalDateTime.now() + "]: Provisionally connected with host [" + randomNeighbourAddress + "]...");
            }
            catch (IOException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Failed to connect to host [" + randomNeighbourAddress + "].");
            }

            try
            {
                if(socket != null && this.testConnection(true, socket))
                {
                    addressToRemove = randomNeighbourAddress;
                    socket.setSoTimeout(0);
                    this.socketsMap.put(addressToRemove, socket);
                }
                else
                {
                    // Set random timeout to avoid deadlocks.
                    this.serverSocket.setSoTimeout(1000 * randomTimeoutInt);
                    System.out.println("[" + LocalDateTime.now() + "]: Waiting for connections...");
                    socket = this.serverSocket.accept();

                    String receivedConnectionAddress = socket.getInetAddress().getHostAddress();
                    System.out.println("[" + LocalDateTime.now() + "]: Received connection from host [" + receivedConnectionAddress + "].");
                    if(this.adjacents.contains(receivedConnectionAddress))
                    {
                        if(this.testConnection(false, socket))
                        {
                            addressToRemove = receivedConnectionAddress;
                            socket.setSoTimeout(0);
                            this.socketsMap.put(addressToRemove, socket);
                        }
                    }
                    else
                    {
                        System.err.println("[" + LocalDateTime.now() + "]: Undefined host [" + receivedConnectionAddress + "].");
                    }
                }
            }
            catch (SocketTimeoutException ste)
            {
                randomTimeoutInt = new Random().nextInt(10) + 1;
                System.out.println("[" + LocalDateTime.now() + "]: New random timeout value of " + randomTimeoutInt + " seconds.");
            }
            catch (SocketException se)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Socket exception on setting the server socket timeout...");
            }
            catch (IOException ioe)
            {
                System.err.println("[" + LocalDateTime.now() + "]: IOException on accept call...");
            }

            if(addressToRemove != null)
                this.adjacents.remove(addressToRemove);
        }
    }

    private boolean testConnection(boolean send, Socket socket)
    {
        String greeting = "Are you there?";
        String response = "YES";
        String address = socket.getInetAddress().getHostAddress();
        boolean result = false;

        DataInputStream dis = null;
        DataOutputStream dos = null;

        try
        {
            System.out.println("[" + LocalDateTime.now() + "]: Testing connection to host [" + address + "]...");

            socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            System.out.println("[" + LocalDateTime.now() + "]: Socket timeout set to " + SOCKET_READ_TIMEOUT + " ms.");

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            if(send)
            {
                System.out.println("[" + LocalDateTime.now() + "]: Sending greeting to host [" + address + "]...");
                dos.writeUTF(greeting);
                dos.flush();
                System.out.println("[" + LocalDateTime.now() + "]: Waiting response from host [" + address + "]...");
                String data = dis.readUTF();
                if(data.equals(response))
                {
                    System.out.println("[" + LocalDateTime.now() + "]: Connected to host " + "[\u001B[32m" + address + "\u001B[0m]!");
                    result = true;
                }
            }
            else
            {
                System.out.println("[" + LocalDateTime.now() + "]: Waiting greeting from host [" + address + "]...");
                String data = dis.readUTF();
                if(data.equals(greeting))
                {
                    System.out.println("[" + LocalDateTime.now() + "]: Sending response to host [" + address + "]...");
                    dos.writeUTF(response);
                    dos.flush();
                    System.out.println("[" + LocalDateTime.now() + "]: Connected to host " + "[\u001B[32m" + address + "\u001B[0m]!");
                    result = true;
                }
            }
        }
        catch (SocketTimeoutException ste)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Reading input stream timeout to address " + address + ".");
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error testing connection to address " + address + ".");
            e.printStackTrace();
        }

        if(!result)
        {
            try
            {
                if(dis != null)
                    dis.close();
                if(dos != null)
                    dos.close();
            }
            catch (IOException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Error closing streams...");
            }
        }
        else {
            this.inputStreamMap.put(address, dis);
            this.outputStreamMap.put(address, dos);
        }

        return result;
    }

    public Map<String, Socket> getSockets()
    {
        return this.socketsMap;
    }

    public Map<String, DataOutputStream> getOutputStreamMap() {
        return this.outputStreamMap;
    }

    public Map<String, DataInputStream> getInputStreamMap() {
        return this.inputStreamMap;
    }
}
