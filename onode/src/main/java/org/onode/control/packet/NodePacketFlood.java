package org.onode.control.packet;

import org.exceptions.PacketFormatException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodePacketFlood implements INodePacket
{
    // Flood packet
    // ----------------------------
    private final int jumps;
    private final long timestamp;
    private final long elapsedTime;
    private final List<String> addressRoute;
    // ----------------------------


    /**
     * Here we assume the packet has ID=1
     * The payload format is: 1;14;timestamp;elapsedTime;ip1,ip2,ip3,
     * 1;0;1669476686020;8;,
     * or
     * 0;timestamp
     */
    public NodePacketFlood(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        int ID = Integer.parseInt(splited[0]);
        if(ID == NodePacketFlood.FLOOD_PACKET_ID && splited.length == 5)
        {
            this.jumps = Integer.parseInt(splited[1]);
            this.timestamp = Long.parseLong(splited[2]);
            this.elapsedTime = Long.parseLong(splited[3]);
            String[] ips = splited[4].split(LST_SEP);
            this.addressRoute = new ArrayList<>();
            this.addressRoute.addAll(Arrays.asList(ips));
        }
        else
            throw new PacketFormatException("Wrong Flood Packet format!");
    }

    public int getJumps() {
        return jumps;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<String> getAddressRoute()
    {
        return addressRoute;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public static String createFloodPacket(
            long receivedTimestamp,
            long elapsedTime,
            int jumps,
            List<String> routeAddresses)
    {
        long timestamp = Instant.now().toEpochMilli();
        long newElapsedTime = elapsedTime + (timestamp - receivedTimestamp);

        return FLOOD_PACKET_ID + ARG_SEP
                + jumps + ARG_SEP
                + timestamp + ARG_SEP
                + newElapsedTime + ARG_SEP
                + String.join(LST_SEP, routeAddresses);
    }

    public static String createFloodPacket(String receivedTimestamp)
    {
        long timestamp = Instant.now().toEpochMilli();
        long newElapsedTime = (timestamp - Long.parseLong(receivedTimestamp));
        return FLOOD_PACKET_ID + ARG_SEP
                + 0 + ARG_SEP
                + timestamp + ARG_SEP
                + newElapsedTime + ARG_SEP
                + LST_SEP;
    }
}
