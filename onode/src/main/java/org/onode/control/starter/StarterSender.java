package org.onode.control.starter;

import org.onode.Main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

public class StarterSender extends AbstractStart implements Runnable
{
    public StarterSender(List<String> adjacents, Integer token)
    {
        super(adjacents, token);
    }

    @Override
    public void run()
    {
        for (String address : super.getAdjacents())
        {
            while(true)
            {
                try
                {
                    Socket socket = new Socket(address, Main.PORT);
                    System.out.println("[" + LocalDateTime.now() + "]: Connected to host [" + address + "].");
                    super.tryConnection(socket);
                    break;
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error in start sender for address [" + address + "].");
                }
            }
        }
    }

    protected int testConnection(DataInputStream dis, DataOutputStream dos)
    {
        int ret = ERROR;
        int myToken = super.getToken();
        try
        {
            dos.writeInt(myToken);
            dos.flush();
            int neighbourRandom = dis.readInt();

            if(neighbourRandom != myToken)
            {
                if(neighbourRandom < myToken)
                    ret = GOOD_KEEP;
                else
                    ret = GOOD_DROP;

                dos.writeUTF(OK);
                dos.flush();
            }
            else
                System.err.println("[" + LocalDateTime.now() + "]: Random numbers are equal!. Restart program!");
        }
        catch (IOException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error testing start listener connection.");
        }

        return ret;
    }

}
