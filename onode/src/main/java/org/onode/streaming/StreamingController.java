package org.onode.streaming;

import org.onode.Main;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingController implements Runnable
{
    private final Set<String> addressesToSend;
    private final Set<String> toBeAdded;
    private final Set<String> toBeRemoved;
    private final AtomicBoolean isLast;
    private final DatagramSocket socket;
    private final int BUFFER_SIZE = 15000;

    public StreamingController() throws SocketException
    {
        this.addressesToSend = new HashSet<>();
        this.toBeAdded = Collections.synchronizedSet(new HashSet<>());
        this.toBeRemoved = Collections.synchronizedSet(new HashSet<>());
        this.socket = new DatagramSocket(Main.STREAMING_PORT);
        isLast = new AtomicBoolean(false);
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
                    socket.receive(rcvPacket);
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error receiving datagram packet.");
                }

                int lenghtReceived = rcvPacket.getLength();
                for (String address : this.addressesToSend) {
                    // Send packet
                    try
                    {
                        DatagramPacket sendPacket = new DatagramPacket(buffer, lenghtReceived, InetAddress.getByName(address), Main.STREAMING_PORT);
                        this.socket.send(sendPacket);
                        System.out.println("[" + LocalDateTime.now() + "]: Sent streaming packet to [" + address + "].");
                    }
                    catch (IOException e) {
                        System.err.println("[" + LocalDateTime.now() + "]: Error sending datagram packet to host [" + address + "]");
                    }
                }
            }
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
}
