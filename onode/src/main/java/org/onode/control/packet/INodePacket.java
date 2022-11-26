package org.onode.control.packet;

public interface INodePacket
{
    public final static int INITIAL_FLOOD_PACKET_ID = 0;
    public final static int FLOOD_PACKET_ID = 1;

    // TODO:
    //  Flood - 0; 1
    //  Activate - 2; 3 - Streamer
    //  Refresh - 4

    public final static String ARG_SEP = ";";
    public final static String LST_SEP = ",";
}
