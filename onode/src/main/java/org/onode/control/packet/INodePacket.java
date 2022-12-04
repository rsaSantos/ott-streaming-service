package org.onode.control.packet;

public interface INodePacket
{
    int FLOOD_PACKET_ID = 1;
    int ACTIVATE_PACKET_ID = 2;
    int DEACTIVATE_PACKET_ID = 3;
    int REQUEST_REFRESH_PACKET_ID = 4;
    int Q_REFRESH_PACKET_ID = 5;
    int A_REFRESH_PACKET_ID = 6;

    // TODO:
    //  Flood - 0; 1
    //  Activate - 2; 3 - Streamer
    //  Refresh - 4

    String ARG_SEP = ";";
    String LST_SEP = ",";
}
