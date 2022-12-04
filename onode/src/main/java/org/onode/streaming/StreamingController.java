package org.onode.streaming;

import org.exceptions.StreamingPacketException;
import org.onode.Main;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingController implements Runnable
{
    private final Set<String> addressesToSend;
    private final Set<String> toBeAdded;
    private final Set<String> toBeRemoved;
    private final AtomicBoolean isLast;
    private final DatagramSocket socket;
    private final int BUFFER_SIZE = 15000;
    private final int MAX_QUEUE_SIZE = 50;

    private final Queue<Long> lastTimeToServer;

    public StreamingController() throws SocketException
    {
        this.lastTimeToServer = new ConcurrentLinkedQueue<>();
        for(int i = 0; i < MAX_QUEUE_SIZE; ++i)
            this.lastTimeToServer.add(0L);

        this.addressesToSend = new HashSet<>();
        this.toBeAdded = Collections.synchronizedSet(new HashSet<>());
        this.toBeRemoved = Collections.synchronizedSet(new HashSet<>());
        this.socket = new DatagramSocket(Main.STREAMING_PORT_EXTERNAL);
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
                    this.socket.receive(rcvPacket);
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error receiving datagram packet.");
                }

                try
                {
                    RTPpacketNode rtPpacketNode = new RTPpacketNode(rcvPacket);
                    this.lastTimeToServer.remove();
                    this.lastTimeToServer.add(rtPpacketNode.getTimeToServer());
                }
                catch (StreamingPacketException e)
                {
                    System.err.println(e.getMessage());
                }


                int packetLength = rcvPacket.getLength();
                for (String address : this.addressesToSend) {
                    // Send packet
                    try
                    {
                        int PORT = Main.STREAMING_PORT_EXTERNAL;
                        if(address.equals("localhost"))
                            PORT = Main.STREAMING_PORT_LOCALHOST;

                        DatagramPacket sendPacket = new DatagramPacket(buffer, packetLength, InetAddress.getByName(address), PORT);
                        this.socket.send(sendPacket);
                        System.out.println("[" + LocalDateTime.now() + "]: Sent streaming packet to [" + address + "].");
                    }
                    catch (IOException e) {
                        System.err.println("[" + LocalDateTime.now() + "]: Error sending datagram packet to host [" + address + "]");
                    }
                }

                // TODO: REMOVE, TEST ONLY
                System.out.println("[" + LocalDateTime.now() + "]: Average TTS is " + this.getAverageTTS() + "ms.");

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

    public double getAverageTTS()
    {
        return this.lastTimeToServer.stream().mapToDouble(Long::doubleValue).average().orElse(-1);
    }
}
