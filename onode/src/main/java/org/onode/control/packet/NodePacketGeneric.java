package org.onode.control.packet;

import org.exceptions.PacketFormatException;

public class NodePacketGeneric implements INodePacket
{
    private final String data;


    public NodePacketGeneric(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(ARG_SEP);
        int ID = Integer.parseInt(splited[0]);
        if ((ID == ACTIVATE_PACKET_ID || ID == DEACTIVATE_PACKET_ID) && splited.length == 2)
            this.data = splited[1];
        else
            throw new PacketFormatException("Wrong generic packet format!");
    }

    public String getData()
    {
        return this.data;
    }

    public static String createActivatePacket(String addressToActivate)
    {
        return ACTIVATE_PACKET_ID + ARG_SEP + addressToActivate;
    }

    public static String createDeactivatePacket(String addressToDeactivate)
    {
        return DEACTIVATE_PACKET_ID + ARG_SEP + addressToDeactivate;
    }
}
