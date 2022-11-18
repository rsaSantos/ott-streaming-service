package org.onode.control;

import java.io.DataOutputStream;
import java.io.IOException;

public class NodeWriterTCP implements Runnable
{
    private final String address;
    private final DataOutputStream dataOutputStream;

    public NodeWriterTCP(DataOutputStream dataOutputStream, String address)
    {
        this.dataOutputStream = dataOutputStream;
        this.address = address;
    }

    @Override
    public void run()
    {

    }

    public void closeOutputStream(boolean sendCloseMessage)
    {
        try
        {
            if (sendCloseMessage)
            {
                // TODO: Send closing message before leaving
            }
            this.dataOutputStream.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
