package org.onode.control;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionStarter implements Runnable
{
    private final ServerSocket serverSocket;

    private final Map<String, Socket> socketsMap;
    private final List<String> adjacents;

    public ConnectionStarter(ServerSocket serverSocket, List<String> adjacents)
    {
        this.serverSocket = serverSocket;
        this.adjacents = adjacents;
        this.socketsMap = new HashMap<>();
    }

    public void run()
    {
        try
        {
            int size = 0;
            while (size < adjacents.size())
            {
                Socket _socket = serverSocket.accept();
                String address = _socket.getInetAddress().getHostAddress();
                if(adjacents.contains(address))
                {
                    this.socketsMap.put(address, _socket);
                    System.out.println("[" + LocalDateTime.now() + "]: Connected to [\u001B[32m" + address + "\u001B[0m]." );
                    size++;
                }
                else
                    System.err.println("Undefined address [" + address + "].");
            }
        }
        catch (IOException e)
        {
            this.socketsMap.clear();
            e.printStackTrace();
        }
    }

    public Map<String, Socket> getConnections() {
        return this.socketsMap;
    }
}
