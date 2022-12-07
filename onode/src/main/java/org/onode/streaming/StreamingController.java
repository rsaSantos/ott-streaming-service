package org.onode.streaming;

import org.onode.Main;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamingController implements Runnable
{
    private final Set<String> addressesToSend;
    private final Set<String> toBeAdded;
    private final Set<String> toBeRemoved;
    private final AtomicBoolean isLast;
    private final AtomicBoolean isStreaming;
    private final DatagramSocket socket;
    private final int BUFFER_SIZE = 15000;

    private final Lock addressLock;
    private String lastSenderAddress;

    public StreamingController() throws SocketException
    {

        this.addressesToSend = new HashSet<>();
        this.toBeAdded = Collections.synchronizedSet(new HashSet<>());
        this.toBeRemoved = Collections.synchronizedSet(new HashSet<>());
        this.socket = new DatagramSocket(Main.STREAMING_PORT_EXTERNAL);
        this.isLast = new AtomicBoolean(false);
        this.isStreaming = new AtomicBoolean(false);
        this.addressLock = new ReentrantLock();
        this.lastSenderAddress = null;
    }

    @Override
    public void run()
    {
        while(true)
        {
            if(!this.addressesToSend.isEmpty())
            {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket rcvPacket = new DatagramPacket(buffer, buffer.length);
                try
                {
                    this.socket.receive(rcvPacket);
                    this.isStreaming.set(true);
                    String senderAddress = rcvPacket.getAddress().getHostAddress();
                    if(!senderAddress.equals(lastSenderAddress))
                        synchronized (this.addressLock)
                        {
                            this.lastSenderAddress = senderAddress;
                        }
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error receiving datagram packet.");
                    this.isStreaming.set(false);
                }


                int lenghtReceived = rcvPacket.getLength();
                for (String address : this.addressesToSend) {
                    // Send packet
                    try
                    {
                        int PORT = Main.STREAMING_PORT_EXTERNAL;
                        if(address.equals("localhost"))
                            PORT = Main.STREAMING_PORT_LOCALHOST;

                        DatagramPacket sendPacket = new DatagramPacket(buffer, lenghtReceived, InetAddress.getByName(address), PORT);
                        this.socket.send(sendPacket);
                        // System.out.println("[" + LocalDateTime.now() + "]: Sent streaming packet to [" + address + "].");
                    }
                    catch (IOException e) {
                        System.err.println("[" + LocalDateTime.now() + "]: Error sending datagram packet to host [" + address + "]");
                    }
                }
            }
            else
                this.isStreaming.set(false);

            this.updateAddresses();
        }
    }

    private void updateAddresses()
    {
        // Add new addresses to send
        synchronized (this.toBeAdded)
        {
            if(!this.toBeAdded.isEmpty())
            {
                this.addressesToSend.addAll(this.toBeAdded);
                this.toBeAdded.clear();
            }
        }

        // Remove addresses
        synchronized (this.toBeRemoved)
        {
            if(!this.toBeRemoved.isEmpty())
            {
                this.addressesToSend.removeAll(this.toBeRemoved);
                this.toBeRemoved.clear();
            }
        }

        this.isLast.set(this.addressesToSend.size() == 1);
    }


    public void add(String newAddress)
    {
        synchronized (this.toBeAdded)
        {
            this.toBeAdded.add(newAddress);
        }
    }

    public boolean remove(String removeAddress)
    {
        synchronized (this.toBeRemoved)
        {
            this.toBeRemoved.add(removeAddress);
        }
        return this.isLast.get();
    }

    public String isStreaming()
    {
        synchronized (this.addressLock)
        {
            return this.isStreaming.get() ? this.lastSenderAddress : null;
        }
    }
}
