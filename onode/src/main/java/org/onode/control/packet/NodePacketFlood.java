package org.onode.control.packet;

import org.exceptions.PacketFormatException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodePacketFlood implements INodePacket
{
    private final String serverID;
    private final int jumps;
    private final long serverTimestamp;
    private final List<String> nodeIDRoute;
    private final int floodID;
    // ----------------------------


    /**
     * Here we assume the packet has ID=1
     * The payload format is: 1;serverID;jumps;serverTimestamp;ip1,ip2,ip3,
     */
    public NodePacketFlood(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        // Flood packet
        // ----------------------------
        int ID = Integer.parseInt(splited[0]);
        if(ID == NodePacketFlood.FLOOD_PACKET_ID && splited.length == 6)
        {
            this.serverID = splited[1];
            this.jumps = Integer.parseInt(splited[2]);
            this.serverTimestamp = Long.parseLong(splited[3]);
            this.floodID = Integer.parseInt(splited[4]);
            String[] ips = splited[5].split(LST_SEP);
            this.nodeIDRoute = new ArrayList<>();
            this.nodeIDRoute.addAll(Arrays.asList(ips));
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

    public List<String> getNodeIDRoute()
    {
        return nodeIDRoute;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public int getFloodID() {
        return floodID;
    }

    public static String createFloodPacket(
            String serverID,
            int jumps,
            long serverTimestamp,
            int floodID,
            List<String> routeAddresses)
    {

        return FLOOD_PACKET_ID + ARG_SEP
                + serverID + ARG_SEP
                + jumps + ARG_SEP
                + serverTimestamp + ARG_SEP
                + floodID + ARG_SEP
                + String.join(LST_SEP, routeAddresses);
    }
}
