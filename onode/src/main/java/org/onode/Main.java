package org.onode;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node
 */
public class Main
{
    private static final String BOOTSTRAPPER_IP = "10.0.20.10";
    private static final int PORT = 25000;

    private static void getAdjacents(List<String> adjacents)
    {
        try
        {
            // Establish connection to server
            System.out.println("[" + LocalDateTime.now().toString() + "]: Connecting to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            Socket bt = new Socket(BOOTSTRAPPER_IP, 25000);
            System.out.println("[" + LocalDateTime.now().toString() + "]: Connected to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m].");

            // Opening reading method from server conection
            System.out.println("[" + LocalDateTime.now().toString() + "]: Opening DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            DataInputStream in = new DataInputStream(bt.getInputStream());
            System.out.println("[" + LocalDateTime.now().toString() + "]: DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] up and running.");

            // Reading information
            System.out.println("[" + LocalDateTime.now().toString() + "]: Receiving adjacent list from server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            String ips = in.readUTF();
            System.out.println("[" + LocalDateTime.now().toString() + "]: Adjacent list from server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] received.");

            // Process adjacent list
            System.out.println("[" + LocalDateTime.now().toString() + "]: Processing adjacent list...");
            String[] ip_list = ips.split(",");

            Collections.addAll(adjacents, ip_list);
            System.out.println("[" + LocalDateTime.now().toString() + "]: Adjacent list processed. Result: " + adjacents.toString());

            in.readUTF();

            // Close reading stream
            System.out.println("[" + LocalDateTime.now().toString() + "]: Closing DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            in.close();
            System.out.println("[" + LocalDateTime.now().toString() + "]: DataInputStream to server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] successfully closed.");

            // Terminate the connection with server
            System.out.println("[" + LocalDateTime.now().toString() + "]: Terminating connection with server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m]...");
            bt.close();
            System.out.println("[" + LocalDateTime.now().toString() + "]: Connection with server [\u001B[32m"
                    + BOOTSTRAPPER_IP
                    + "\u001B[0m] successfully terminated.");

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        List<String> adjacents = new ArrayList<>();

        try
        {
            System.out.println("[" + LocalDateTime.now().toString() + "]: Openening TCP server socket...");
            ServerSocket ss = new ServerSocket(PORT);
            System.out.println("[" + LocalDateTime.now().toString() + "]: TCP server socket up and running at port " + PORT + ".");

            getAdjacents(adjacents);

            ss.close();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}