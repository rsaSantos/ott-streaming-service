package org.onode.control.packet;

public interface INodePacket
{
    int FLOOD_PACKET_ID = 1;
    int ACTIVATE_PACKET_ID = 2;
    int DEACTIVATE_PACKET_ID = 3;

    String ARG_SEP = ";";
    String LST_SEP = ",";
}
