package org.onode;

import org.exceptions.PacketFormatException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FloodPacket
{
    public static final String ID = "1";

    private final int jumps;
    private final long timestamp;
    private final List<String> ips;

    /**
     * Here we assume the packet has ID=1
     * The payload format is: 1;14;timestamp;ip1,ip2,ip3,
     *
     * TODO: Throw error of format!
     *
     * */
    public FloodPacket(String payload) throws PacketFormatException
    {
        String[] splited = payload.split(";");
        if(splited[0].equals(FloodPacket.ID) && splited.length == 4)
        {
            this.jumps = Integer.parseInt(splited[1]);
            this.timestamp = Long.parseLong(splited[2]);
            String[] ips = splited[3].split(",");
            this.ips = new ArrayList<>();
            this.ips.addAll(Arrays.asList(ips));
        }
        else
            throw new PacketFormatException("Wrong packet ID!");
    }

    // TODO: Getters as needed.
}
