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
    private final int ID;
    private final String serverID;
    private final int jumps;
    private final long timestamp;
    private final long elapsedTime;
    private final List<String> addressRoute;
    // ----------------------------


    /**
     * Here we assume the packet has ID=1
     * The payload format is: 1;serverID;14;timestamp;elapsedTime;ip1,ip2,ip3,
     * 1;s1;0;1669476686020;8;,
     * or
     * 0;s2;timestamp
     */
    public NodePacketFlood(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        this.ID = Integer.parseInt(splited[0]);
        if(this.ID == NodePacketFlood.FLOOD_PACKET_ID && splited.length == 6)
        {
            this.serverID = splited[1];
            this.jumps = Integer.parseInt(splited[2]);
            this.timestamp = Long.parseLong(splited[3]);
            this.elapsedTime = Long.parseLong(splited[4]);
            String[] ips = splited[5].split(LST_SEP);
            this.addressRoute = new ArrayList<>();
            this.addressRoute.addAll(Arrays.asList(ips));
        }
        else if (ID == NodePacketFlood.INITIAL_FLOOD_PACKET_ID && splited.length == 3)
        {
            this.serverID = splited[1];
            this.jumps = 0;
            this.timestamp = Long.parseLong(splited[2]);
            this.elapsedTime = 0;
            this.addressRoute = new ArrayList<>();
        }
        else
            throw new PacketFormatException("Wrong Flood Packet format!");
    }

    public int getID() {
        return ID;
    }

    public String getServerID() {
        return serverID;
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
            String serverID,
            long receivedTimestamp,
            long elapsedTime,
            int jumps,
            List<String> routeAddresses)
    {
        long timestamp = Instant.now().toEpochMilli();
        long newElapsedTime = elapsedTime + (timestamp - receivedTimestamp);

        return FLOOD_PACKET_ID + ARG_SEP
                + serverID + ARG_SEP
                + jumps + ARG_SEP
                + timestamp + ARG_SEP
                + newElapsedTime + ARG_SEP
                + String.join(LST_SEP, routeAddresses);
    }

    public static String createFloodPacket(String serverID, long receivedTimestamp)
    {
        long timestamp = Instant.now().toEpochMilli();
        long newElapsedTime = (timestamp - receivedTimestamp);
        return FLOOD_PACKET_ID + ARG_SEP
                + serverID + ARG_SEP
                + 0 + ARG_SEP
                + timestamp + ARG_SEP
                + newElapsedTime + ARG_SEP
                + LST_SEP;
    }
}
