package org.onode.control.reader;

import org.onode.control.NodeController;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;

public class NodeReader<L> extends AbstractNodeReader
{
    private final L listener; // L type is either DataInputStream or ServerSocket

    public NodeReader(L listener, String address) throws SocketException
    {
        super(address);
        this.listener = listener;
        if(this.listener instanceof ServerSocket)
            ((ServerSocket) this.listener).setSoTimeout(0);
    }

    @Override
    protected String waitForData()
    {
        String data = null;

        // L type is either DataInputStream or ServerSocket
        if(this.listener instanceof DataInputStream)
        {
            try
            {
                System.out.println("[" + LocalDateTime.now() + "]: Waiting for data from [" + super.getAddress() + "].");
                data = ((DataInputStream) this.listener).readUTF();
            }
            catch (EOFException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Input stream from address [" + super.getAddress() + "] received EOF.");

                // Tell node listener to close this socket and exit.
                data = NodeController.DELETE_ME;
                super.putData(NodeController.DELETE_ME);
                this.closeListener();
            }
            // Keeps running after this exception....
            catch (UTFDataFormatException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Input stream from address [" + super.getAddress() + "] received non UTF data.");
            }
            // Stop running...
            catch (IOException e)
            {
                System.err.println("[IOException] Stream has been closed.");

                // Tell node listener to close this socket and exit.
                super.putData(NodeController.DELETE_ME);
                this.closeListener();
            }
        }
        else if(this.listener instanceof ServerSocket)
        {
            try
            {
                Socket socket = ((ServerSocket) this.listener).accept();
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                data = dis.readUTF();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return data;
    }

    @Override
    protected void closeListener()
    {
        try {

            if (this.listener instanceof DataInputStream)
                ((DataInputStream) this.listener).close();
            else if (this.listener instanceof ServerSocket)
                ((ServerSocket) this.listener).close();
        }
        catch (IOException e)
        {
            System.err.println("Error closing listener on address " + super.getAddress());
        }
    }
}
