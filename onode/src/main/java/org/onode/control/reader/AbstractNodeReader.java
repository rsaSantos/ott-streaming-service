package org.onode.control.reader;

import org.onode.control.NodeController;
import org.onode.utils.Triplet;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractNodeReader implements Runnable
{
    private final String address;

    private final LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue;

    public AbstractNodeReader(LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue, String address)
    {
        this.address = address;
        this.dataQueue = dataQueue;
    }

    @Override
    public void run()
    {
        while(true)
        {
            String data = this.waitForData();
            if(data != null)
            {
                try
                {
                    System.out.println("[" + LocalDateTime.now() + "]: Trying to add data to queue of host [" + this.address + "]");
                    this.dataQueue.put(new Triplet<>(NodeController.OP_READ, Collections.singletonList(this.address), data));
                    System.out.println("[" + LocalDateTime.now() + "]: Added data to queue of host [" + this.address + "]");
                }
                catch (InterruptedException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Failed to insert data into queue of host [" + this.address + "].");
                }

                if (data.equals(NodeController.DELETE_ME)) break;
            }
        }
        System.out.println("[" + LocalDateTime.now() + "]: Exiting NodeReceiver for address [" + this.address + "].");
    }

    abstract protected String waitForData();
    abstract protected void closeListener();
    public String getAddress()
    {
        return this.address;
    }
}
