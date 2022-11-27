package org.onode.control.packet;

public interface INodePacket
{
    int INITIAL_FLOOD_PACKET_ID = 0;
    int FLOOD_PACKET_ID = 1;
    int ACTIVATE_PACKET_ID = 2;

    // TODO:
    //  Flood - 0; 1
    //  Activate - 2; 3 - Streamer
    //  Refresh - 4

    String ARG_SEP = ";";
    String LST_SEP = ",";
}
