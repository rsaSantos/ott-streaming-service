package org.onode.streaming;

import org.onode.Main;
import org.onode.utils.Pair;

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
    private final AtomicBoolean isStreaming;
    private final DatagramSocket socket;
    private final int BUFFER_SIZE = 15000;

    private final Lock addressLock;
    private String lastSenderAddress;

    public StreamingController() throws SocketException
    {

        this.addressesToSend = Collections.synchronizedSet(new HashSet<>());
        this.socket = new DatagramSocket(Main.STREAMING_PORT_EXTERNAL);
        this.socket.setSoTimeout(5000);
        this.isStreaming = new AtomicBoolean(false);
        this.addressLock = new ReentrantLock();
        this.lastSenderAddress = null;
    }

    @Override
    public void run()
    {
        int printCounter = 0;

        while(true)
        {
            boolean someoneToSend;
            synchronized (this.addressesToSend)
            {
                someoneToSend = !this.addressesToSend.isEmpty();
            }

            if(someoneToSend)
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
                catch (SocketTimeoutException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Socket timeout.");
                    this.isStreaming.set(false);
                }
                catch (IOException e)
                {
                    System.err.println("[" + LocalDateTime.now() + "]: Error receiving datagram packet.");
                    this.isStreaming.set(false);
                }

                int lenghtReceived = rcvPacket.getLength();
                synchronized (this.addressesToSend)
                {
                    for (String address : this.addressesToSend) {
                        // Send packet
                        try
                        {
                            int PORT = Main.STREAMING_PORT_EXTERNAL;
                            if(address.equals("localhost"))
                                PORT = Main.STREAMING_PORT_LOCALHOST;

                            DatagramPacket sendPacket = new DatagramPacket(buffer, lenghtReceived, InetAddress.getByName(address), PORT);
                            this.socket.send(sendPacket);

                            if(printCounter % 25 == 0)
                                System.out.println("[" + LocalDateTime.now() + "]: Sent streaming packet to [" + address + "].");
                            printCounter++;
                        }
                        catch (IOException e) {
                            System.err.println("[" + LocalDateTime.now() + "]: Error sending datagram packet to host [" + address + "]");
                        }
                    }
                }
            }
            else
                this.isStreaming.set(false);
        }
    }

    public void add(String newAddress)
    {
        synchronized (this.addressesToSend)
        {
            this.addressesToSend.add(newAddress);
        }
    }

    public int remove(String removeAddress)
    {
        synchronized (this.addressesToSend)
        {
            this.addressesToSend.remove(removeAddress);
            return this.addressesToSend.size();
        }
    }

    public Pair<Boolean,String> isStreaming()
    {
        synchronized (this.addressLock)
        {
            return new Pair<>(this.isStreaming.get(), this.lastSenderAddress);
        }
    }
}
