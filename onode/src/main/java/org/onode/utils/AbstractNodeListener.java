package org.onode.utils;

import org.onode.control.NodeController;

import java.io.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractNodeListener implements Runnable
{
    private final Lock readLock;
    private final Lock writeLock;
    private final Condition isReadDataAvailable;

    private final String address;

    private final Queue<String> dataQueue;

    public AbstractNodeListener(Condition isReadDataAvailable, String address)
    {
        this.address = address;
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
            String data = this.waitForData();
            if(data != null)
                this.putData(data);
        }
    }

    abstract protected String waitForData();
    abstract protected void closeListener();


    protected void putData(String data)
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

    public String readData()
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
}
