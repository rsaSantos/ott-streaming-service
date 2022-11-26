package org.onode.control.writer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import org.exceptions.PacketFormatException;
import org.onode.control.NodeController;
import org.onode.control.packet.INodePacket;
import org.onode.control.packet.NodePacketFlood;
import org.onode.utils.*;

import static org.onode.control.packet.INodePacket.ARG_SEP;

public class NodeTask implements Runnable
{
    public static int TASK_PACKET = 0;

    // TODO:
    //  Flood - 0; 1
    //  Activate - 2; 3 - Streamer
    //  Refresh - 4

    private final int taskType;
    private final String address;
    private final String data;
    private final LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue;
    private final List<String> adjacents;

    public NodeTask(
            int task_type,
            String address,
            String data,
            List<String> adjacents,
            LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue
    )
    {
        this.taskType = task_type;
        this.address = address;
        this.data = data;
        this.adjacents = adjacents;
        this.dataQueue = dataQueue;
    }

    private void flood(NodePacketFlood floodPacket)
    {
        try
        {
            // Get data
            int jumps = floodPacket.getJumps();
            long timestamp = floodPacket.getTimestamp();
            long elapsedTime = floodPacket.getElapsedTime();
            List<String> routeAddresses = floodPacket.getAddressRoute();

            // Add previous node address
            routeAddresses.add(address);

            // Create update state request
            // jumps, timestamp, elapsedTime, route
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_CHANGE_STATE,
                            Collections.singletonList(address),
                            Arrays.asList(jumps, timestamp, elapsedTime, routeAddresses)
                            ));

            // Increment jumps
            jumps++;

            // Create packet
            String payload = NodePacketFlood.createFloodPacket(timestamp, elapsedTime, jumps, routeAddresses);

            // Get list of nodes to send inside payload
            List<String> nodesToSend = new ArrayList<>(this.adjacents);
            nodesToSend.removeAll(routeAddresses);

            // Create write task (only if there is someone to write to)
            if(!nodesToSend.isEmpty())
                this.dataQueue.put(new Triplet<>(NodeController.OP_WRITE, nodesToSend, payload));
        }
        catch(InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error updating queue.");
        }
    }

    private void startFlood(NodePacketFlood floodPacket)
    {
        // Create flood packet payload
        String payload = NodePacketFlood.createFloodPacket(floodPacket.getTimestamp());

        try
        {
            // Create task to send to all adjacents
            this.dataQueue.put(new Triplet<>(NodeController.OP_WRITE, this.adjacents, payload));
        }
        catch (InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Error updating queue.");
        }
    }

    @Override
    public void run() 
    {
        if(taskType == TASK_PACKET)
        {
            try
            {
                int packetID = Integer.parseInt(this.data.split(ARG_SEP)[0]);
                if(packetID == INodePacket.FLOOD_PACKET_ID)
                    this.flood(new NodePacketFlood(this.data));
                else if (packetID == INodePacket.INITIAL_FLOOD_PACKET_ID)
                    this.startFlood(new NodePacketFlood(this.data));

                // TODO: More packets... (maybe use switch)
            }
            catch (PacketFormatException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: " + e.getMessage() + this.address + "].");
            }
        }
        else
        {
            // TODO: Other kinds of tasks...
        }
    }
}
