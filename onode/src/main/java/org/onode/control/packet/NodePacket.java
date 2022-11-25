package org.onode.control.packet;

public interface NodePacket
{
    public static int INITIAL_FLOOD_PACKET_ID = 0;
    public static int FLOOD_PACKET_ID = 1;

    // TODO:
    //  Flood - 0; 1
    //  Activate - 2; 3 - Streamer
    //  Refresh - 4

    public static String ARG_SEP = ";";
    public static String LST_SEP = ";";
}
