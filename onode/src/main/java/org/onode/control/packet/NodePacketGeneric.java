package org.onode.control.packet;

import org.exceptions.PacketFormatException;

public class NodePacketGeneric implements INodePacket
{
    private final int ID;
    private final String data;


    public NodePacketGeneric(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        this.ID = Integer.parseInt(splited[0]);
        if (this.ID == INITIAL_FLOOD_PACKET_ID && splited.length == 2)
            this.data = splited[1];
        else if (this.ID == ACTIVATE_PACKET_ID)
            this.data = null;
        else
            throw new PacketFormatException("Wrong generic packet format!");
    }

    public String getData()
    {
        return this.data;
    }

    public static String createActivatePacket()
    {
        return ACTIVATE_PACKET_ID + ARG_SEP;
    }
}
