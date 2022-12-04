package org.onode.control.packet;

import org.exceptions.PacketFormatException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodePacketFlood implements INodePacket
{
    private final String serverID;
    private final int jumps;
    private final long serverTimestamp;
    private final List<String> addressRoute;
    // ----------------------------


    /**
     * Here we assume the packet has ID=1
     * The payload format is: 1;serverID;14;timestamp;elapsedTime;ip1,ip2,ip3,
     * 1;s1;0;1669476686020;8;,
     */
    public NodePacketFlood(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        // Flood packet
        // ----------------------------
        int ID = Integer.parseInt(splited[0]);
        if(ID == NodePacketFlood.FLOOD_PACKET_ID && splited.length == 5)
        {
            this.serverID = splited[1];
            this.jumps = Integer.parseInt(splited[2]);
            this.serverTimestamp = Long.parseLong(splited[3]);
            String[] ips = splited[4].split(LST_SEP);
            this.addressRoute = new ArrayList<>();
            this.addressRoute.addAll(Arrays.asList(ips));
        }
        else
            throw new PacketFormatException("Wrong Flood Packet format!");
    }

    public String getServerID() {
        return serverID;
    }

    public int getJumps() {
        return jumps;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public List<String> getAddressRoute()
    {
        return addressRoute;
    }


    public static String createFloodPacket(
            String serverID,
            long receivedTimestamp,
            long newElapsedTime,
            int jumps,
            List<String> routeAddresses)
    {
        return FLOOD_PACKET_ID + ARG_SEP
                + serverID + ARG_SEP +
                + jumps + ARG_SEP
                + newElapsedTime + ARG_SEP
                + String.join(LST_SEP, routeAddresses);
    }
}
