package org.onode.control.writer;

import java.time.Instant;
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
import org.onode.control.packet.NodePacketGeneric;
import org.onode.control.packet.NodePacketRefresh;
import org.onode.utils.*;

import static org.onode.control.NodeController.IGNORE;
import static org.onode.control.packet.INodePacket.ARG_SEP;

public class NodeTask implements Runnable
{
    public static int TASK_PACKET = 0;

    // TODO:
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
            String serverID = floodPacket.getServerID();
            int jumps = floodPacket.getJumps();
            long serverTimestamp = floodPacket.getServerTimestamp();
            long elapsedTime = Instant.now().toEpochMilli() - serverTimestamp;
            List<String> routeAddresses = floodPacket.getAddressRoute();

            // Add previous node address
            routeAddresses.add(this.address);

            // Create update state request
            // jumps, timestamp, elapsedTime, route
            // Do not create this entry if the size of the routeAddresses list is 1.
            //  This means that the server is our parent "node".
            if(routeAddresses.size() > 1)
                this.dataQueue.put(
                        new Triplet<>(
                                NodeController.OP_CHANGE_STATE,
                                Collections.singletonList(this.address),
                                Arrays.asList(serverID, jumps, elapsedTime, routeAddresses)
                                ));

            // Increment jumps (even if the server is on the same container as the node, it counts as a jump).
            jumps++;

            // Create packet
            String payload = NodePacketFlood.createFloodPacket(serverID, serverTimestamp, elapsedTime, jumps, routeAddresses);

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

    private void activate(NodePacketGeneric activationPacket)
    {
        try
        {
            String addressToActivate = activationPacket.getData();
            if(addressToActivate.equals(IGNORE))
                addressToActivate = this.address;

            // Create task to active stream to given address.
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_ACTIVATE_STREAM,
                            Collections.singletonList(addressToActivate),
                            null
                    ));
        }
        catch (InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to insert activate stream data into queue for host [" + this.address + "].");
        }
    }

    private void deactivate(NodePacketGeneric deactivationPacket)
    {
        try
        {
            String addressToDeactivate = deactivationPacket.getData();
            if(addressToDeactivate.equals(IGNORE))
                addressToDeactivate = this.address;

            // Create task to active stream to given address.
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_DEACTIVATE_STREAM,
                            Collections.singletonList(addressToDeactivate),
                            null
                    ));
        }
        catch (InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to insert deactivate stream data into queue for host [" + this.address + "].");
        }
    }

    private void requestRefresh()
    {
        // Create task to handle the refresh request.
        try {
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_REQUEST_REFRESH,
                            null,
                            null
                    )
            );
        }
        catch (InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to put request refresh in data queue.");
        }
    }

    private void q_refresh()
    {
        // Create task to handle the question refresh.
        try {
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_QUESTION_REFRESH,
                            Collections.singletonList(this.address),
                            null
                    )
            );
        }
        catch (InterruptedException e)
        {
            System.err.println("[" + LocalDateTime.now() + "]: Failed to put request refresh in data queue.");
        }
    }

    private void a_refresh(NodePacketRefresh nodePacketRefresh)
    {
        String serverID = nodePacketRefresh.getServerID();
        double nodeAverageTTS = nodePacketRefresh.getAverageTTS();
        long timestamp = nodePacketRefresh.getTimestamp();
        List<String> route = nodePacketRefresh.getRoute();

        double averageTimeToServer = Instant.now().toEpochMilli() - timestamp + nodeAverageTTS;
        route.add(this.address);
        int jumps = route.size();

        // Update state
        try
        {
            this.dataQueue.put(
                    new Triplet<>(
                            NodeController.OP_CHANGE_STATE_WITH_ACTION,
                            Collections.singletonList(this.address),
                            Arrays.asList(serverID, jumps, averageTimeToServer, route)
                    ));
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
                if (packetID == INodePacket.FLOOD_PACKET_ID)
                    this.flood(new NodePacketFlood(this.data));
                else if (packetID == INodePacket.ACTIVATE_PACKET_ID)
                    this.activate(new NodePacketGeneric(this.data));
                else if (packetID == INodePacket.DEACTIVATE_PACKET_ID)
                    this.deactivate(new NodePacketGeneric(this.data));
                else if (packetID == INodePacket.REQUEST_REFRESH_PACKET_ID)
                    this.requestRefresh();
                else if(packetID == INodePacket.Q_REFRESH_PACKET_ID)
                    this.q_refresh();
                else if(packetID == INodePacket.A_REFRESH_PACKET_ID)
                    this.a_refresh(new NodePacketRefresh(this.data));

                // TODO: More packets... (maybe use switch)
            }
            catch (PacketFormatException e)
            {
                System.err.println("[" + LocalDateTime.now() + "]: " + e.getMessage() + "(for host [" + this.address + "]).");
            }
        }
        else
        {
            // TODO: Other kinds of tasks...
        }
    }
}
