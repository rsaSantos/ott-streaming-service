package org.onode.control.starter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

public class StarterListener extends AbstractStart implements Runnable
{
    private final ServerSocket serverSocket;

    public StarterListener(ServerSocket serverSocket, List<String> adjacents, Integer token)
    {
        super(adjacents, token);
        this.serverSocket = serverSocket;
    }

    @Override
    public void run()
    {
        while(!super.isEmpty())
        {
            try
            {
                Socket socket = this.serverSocket.accept();
                System.out.println("[" + LocalDateTime.now() + "]: Connected to host [\u001B[32m" + socket.getInetAddress().getHostAddress() + "\u001B[0m].");
                super.tryConnection(socket);
            }
            catch (IOException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Error in start listener...");
            }
        }
    }

    protected int testConnection(DataInputStream dis, DataOutputStream dos)
    {
        int ret = ERROR;
        int myToken = super.getToken();
        try
        {
            int neighbourRandom = dis.readInt();
            System.out.println("[" + LocalDateTime.now() + "]: Received token " + neighbourRandom + ".");

            dos.writeInt(myToken);
            dos.flush();
            System.out.println("[" + LocalDateTime.now() + "]: Sent token " + myToken + ".");
            String ok = dis.readUTF();
            if(ok.equals(OK))
            {
                System.out.println("[" + LocalDateTime.now() + "]: Received neighbour confirmation.");
                if(neighbourRandom < myToken)
                    ret = GOOD_KEEP;
                else if(neighbourRandom > myToken)
                    ret = GOOD_DROP;
                else
                    System.err.println("[" + LocalDateTime.now() + "]: Random numbers are equal!. Restart program!");
            }
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error testing start listener connection.");
        }

        return ret;
    }
}
