package org.onode.control;

import java.io.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeReaderTCP implements Runnable
{
    private final Lock readLock;
    private final Lock writeLock;
    private final Condition isReadDataAvailable;

    private final String address;
    private final DataInputStream dataInputStream;
    private final Queue<String> dataQueue;

    public NodeReaderTCP(Condition isReadDataAvailable, DataInputStream dataInputStream, String address)
    {
        this.address = address;
        this.dataInputStream = dataInputStream;
        this.dataQueue = new LinkedList<>();

        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.isReadDataAvailable = isReadDataAvailable;
    }

    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                String data = this.dataInputStream.readUTF();
                this.putToQueue(data);
            }
            // Stop running...
            catch (EOFException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Input stream from address ["
                        + this.address + "] received EOF.");

                // Tell node listener to close this socket and exit.
                this.putToQueue(NodeController.DELETE_ME);
                this.closeInputStream();
                return;
            }
            // Keeps running after this exception....
            catch (UTFDataFormatException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: Input stream from address [" +
                        this.address + "] received non UTF data.");
            }
            // Stop running...
            catch (IOException e)
            {
                System.err.println("[IOException] Stream has been closed.");

                // Tell node listener to close this socket and exit.
                this.putToQueue(NodeController.DELETE_ME);
                this.closeInputStream();
                return;
            }
        }
    }

    private void putToQueue(String data)
    {
        try {
            this.writeLock.lock();
            this.dataQueue.add(data);
            this.isReadDataAvailable.notifyAll();
        }
        finally {
            this.writeLock.unlock();
        }
    }

    public String readFromQueue()
    {
        try
        {
            this.readLock.lock();
            if(!this.dataQueue.isEmpty())
            {
                try
                {
                    this.writeLock.lock();
                    return this.dataQueue.poll();
                }
                finally
                {
                    this.writeLock.unlock();
                }
            }
            else
                return null;
        }
        finally
        {
            this.readLock.unlock();
        }
    }

    public String getAddress()
    {
        return this.address;
    }

    public void closeInputStream()
    {
        try
        {
            this.dataInputStream.close();
        }
        catch (IOException e)
        {
            System.out.println("Error closing input stream for address [" + this.getAddress() + "].");
            e.printStackTrace();
        }
    }
}
