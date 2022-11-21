package org.onode.control;

import org.onode.Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class ConnectionStarter implements Runnable
{
    private final ServerSocket serverSocket;
    private final List<String> adjacents;
    private final Map<String, Socket> socketsMap;

    public ConnectionStarter(ServerSocket serverSocket, List<String> adjacents)
    {
        this.serverSocket = serverSocket;
        this.adjacents = new ArrayList<>();
        this.adjacents.addAll(adjacents);
        this.socketsMap = new HashMap<>();
    }

    public void run()
    {
        int randomTimeoutInt = new Random().nextInt(10) + 1;

        while(!this.adjacents.isEmpty())
        {
            String addressToRemove = null;
            try
            {
                String randomNeighbourAddress = this.adjacents.get(new Random().nextInt(this.adjacents.size()));
                Socket socket = new Socket(InetAddress.getByName(randomNeighbourAddress), Main.PORT);
                // TODO: CHECK THIS -> 3s timeout for the read call...
                socket.setSoTimeout(3000);

                if(this.testConnection(true, socket))
                {
                    addressToRemove = randomNeighbourAddress;
                    this.socketsMap.put(addressToRemove, socket);
                }
                else
                {
                    // Set random timeout to avoid deadlocks.
                    this.serverSocket.setSoTimeout(1000 * randomTimeoutInt);
                    socket = this.serverSocket.accept();

                    String receivedConnectionAddress = socket.getInetAddress().getHostAddress();
                    if(this.adjacents.contains(receivedConnectionAddress))
                    {
                        // TODO: CHECK THIS -> 3s timeout for the read call...
                        socket.setSoTimeout(3000);
                        if(this.testConnection(false, socket))
                        {
                            addressToRemove = receivedConnectionAddress;
                            this.socketsMap.put(addressToRemove, socket);
                        }
                    }
                }
            }
            catch (SocketTimeoutException e)
            {
                System.err.println("Socket timeout with random waiting time of " + randomTimeoutInt + ".");
                randomTimeoutInt = new Random().nextInt(10) + 1;
                System.out.println("New timeout value of " + randomTimeoutInt + " seconds.");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            if(addressToRemove != null)
                this.adjacents.remove(addressToRemove);
        }
    }

    private boolean testConnection(boolean send, Socket socket)
    {
        String greeting = "Are you there?";
        String response = "YES";
        boolean result = false;

        try
        {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            if(send)
            {
                dos.writeUTF(greeting);
                String data = dis.readUTF();
                if(data.equals(response))
                {
                    result = true;
                }
            }
            else
            {
                String data = dis.readUTF();
                if(data.equals(greeting))
                {
                    dos.writeUTF(response);
                    result = true;
                }
            }

            dis.close();
            dos.close();
        }
        catch (IOException e)
        {
            System.err.println("Error testing connection to address " + socket.getInetAddress().getHostAddress() + ".");
        }

        return result;
    }

    public Map<String, Socket> getSockets()
    {
        return this.socketsMap;
    }
}
