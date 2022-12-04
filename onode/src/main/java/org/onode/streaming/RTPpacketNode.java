package org.onode.streaming;

import org.exceptions.StreamingPacketException;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;

public class RTPpacketNode
{
    public static int HEADER_SIZE = 12;
    private final long serverTimestamp;
    private final long now;

    public RTPpacketNode(DatagramPacket rcvPacket) throws StreamingPacketException
    {
        if (rcvPacket.getLength() >= HEADER_SIZE)
        {
            now = Instant.now().toEpochMilli();

            //get the header bitsream:
            byte[] header = new byte[HEADER_SIZE];
            System.arraycopy(rcvPacket.getData(), 0, header, 0, HEADER_SIZE);

            serverTimestamp = ByteBuffer.wrap(header, 4, 8).getLong();
        }
        else
            throw new StreamingPacketException("[" + LocalDateTime.now() + "]: Invalid header size of " + rcvPacket.getLength() + ".");
    }

    public long getTimeToServer()
    {
        return now - serverTimestamp;
    }
}
