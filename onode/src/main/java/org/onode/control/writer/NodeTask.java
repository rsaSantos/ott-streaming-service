package org.onode.control.writer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.exceptions.PacketFormatException;
import org.onode.control.NodeController;
import org.onode.control.packet.INodePacket;
import org.onode.control.packet.NodePacketFlood;
import org.onode.control.packet.NodePacketGeneric;
import org.onode.utils.*;

import static org.onode.control.NodeController.IGNORE;
import static org.onode.control.packet.INodePacket.ARG_SEP;

public class NodeTask implements Runnable
{
    public static int TASK_PACKET = 0;
    private final int taskType;
    private final String address;
    private final String data;
    private final LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue;
    private final List<Pair<String, String>> adjacent_ID_IP;
    private final String myID;

    public NodeTask(
            int task_type,
            String address,
            String data,
            String myID,
            List<Pair<String, String>> adjacent_ID_IP,
            LinkedBlockingQueue<Triplet<Integer, List<String>, Object>> dataQueue
    )
    {
        this.myID = myID;
        this.taskType = task_type;
        this.address = address;
        this.data = data;
        this.dataQueue = dataQueue;
        this.adjacent_ID_IP = new ArrayList<>();
        for(Pair<String, String> idIP : adjacent_ID_IP)
            this.adjacent_ID_IP.add(new Pair<>(idIP.first(), idIP.second()));
    }

    private void flood(NodePacketFlood floodPacket)
    {
        try
        {
            // Get data
            String serverID = floodPacket.getServerID();
            int jumps = floodPacket.getJumps();
            long serverTimestamp = floodPacket.getServerTimestamp();
            List<String> nodeIDRoute = floodPacket.getNodeIDRoute();

            // Add our node ID.
            nodeIDRoute.add(myID);

            // Calculate time to server
            long elapsedTime = Instant.now().toEpochMilli() - serverTimestamp;

            // Create update state request
            // jumps, timestamp, elapsedTime, route
            // Do not create this entry if the size of the routeAddresses list is 1.
            //  This means that the server is our parent "node".
            if(nodeIDRoute.size() > 1)
                this.dataQueue.put(
                        new Triplet<>(
                                NodeController.OP_CHANGE_STATE,
                                Collections.singletonList(this.address),
                                Arrays.asList(serverID, jumps, elapsedTime, nodeIDRoute)
                                ));

            // Increment jumps (even if the server is on the same container as the node, it counts as a jump).
            jumps++;

            // Create packet
            String payload = NodePacketFlood.createFloodPacket(serverID, jumps, serverTimestamp, nodeIDRoute);

            // Get list of nodes to send inside payload
            List<String> nodesToSend = new ArrayList<>();
            for(Pair<String, String> idIP : adjacent_ID_IP)
            {
                if(!nodeIDRoute.contains(idIP.first()))
                    nodesToSend.add(idIP.second());
            }

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
