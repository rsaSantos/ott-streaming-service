package org.onode.control.packet;

import org.exceptions.PacketFormatException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodePacketRefresh implements INodePacket
{
    private final String serverID;
    private final double averageTTS;
    private final long timestamp;
    private final List<String> route;

    public NodePacketRefresh(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        int ID = Integer.parseInt(splited[0]);
        if(ID == NodePacketFlood.FLOOD_PACKET_ID && splited.length == 5)
        {
            this.serverID = splited[1];
            this.averageTTS = Double.parseDouble(splited[2]);
            this.timestamp = Long.parseLong(splited[3]);
            String[] ips = splited[4].split(LST_SEP);
            this.route = new ArrayList<>();
            this.route.addAll(Arrays.asList(ips));
        }
        else
            throw new PacketFormatException("Wrong A_REFRESH Packet format!");
    }

    public static String createRequestRefreshPacket()
    {
        return REQUEST_REFRESH_PACKET_ID + "";
    }

    public static String createQRequestPacket()
    {
        return Q_REFRESH_PACKET_ID + "";
    }

    public static String createARequestPacket(String serverID, double tts, List<String> route)
    {
        return A_REFRESH_PACKET_ID + ARG_SEP
                + serverID + ARG_SEP
                + tts + ARG_SEP
                + Instant.now().toEpochMilli() + ARG_SEP
                + String.join(LST_SEP, route);
    }

    public double getAverageTTS() {
        return averageTTS;
    }

    public List<String> getRoute() {
        return route;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getServerID() {
        return serverID;
    }
}
