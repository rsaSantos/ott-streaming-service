package org.onode.control;

import org.exceptions.UpdateNodeStateException;
import org.onode.utils.Pair;

import java.time.LocalDateTime;
import java.util.*;


public class NodeState
{

    static class NodeStateComparator implements Comparator<Pair<String, List<Object>>>
    {
        private boolean equalsWithError(int compare, long val1, long val2)
        {
            // 10% error!!
            return compare < 0 ?
                    (double) val1 / val2 > (1 - 0.1)
                    :
                    (double) val2 / val1 > (1 - 0.1)
            ;
        }

        @Override
        public int compare(
                Pair<String, List<Object>> floodInfo_1,
                Pair<String, List<Object>> floodInfo_2)
        {
            // Get data
            // (String)serverID, (int)jumps, (long)elapsedTime, (List<String>)route
            int jumps_1 = (int) floodInfo_1.second().get(1);
            int jumps_2 = (int) floodInfo_2.second().get(2);


            long elapsedTime_1 = (long) floodInfo_1.second().get(3);
            long elapsedTime_2 = (long) floodInfo_2.second().get(3);


            int elapsedTimeCompare = Long.compare(elapsedTime_1, elapsedTime_2);
            boolean elapsedTimeRelation = equalsWithError(elapsedTimeCompare, elapsedTime_1, elapsedTime_2);

            // Means we assume that timestamps are equal!
            if(elapsedTimeRelation)
                    return Integer.compare(jumps_1, jumps_2);
            else
                return elapsedTimeCompare;
        }
    }

    // [pair (address "pai", [serverID, jumps, elapsedTime, route])]
    // Sorted by fastest!
    private final List<Pair<String, List<Object>>> streamingState;
    
    public NodeState()
    {
        streamingState = new ArrayList<>();
    }

    public void update(Pair<String, Object> floodInfo) throws UpdateNodeStateException
    {
        String address = floodInfo.first();

        if(floodInfo.second() instanceof List<?>)
        {
            List<Object> data = (List<Object>) floodInfo.second();
            if(data.size() == 4)
            {
                if(data.get(3) instanceof List<?>)
                {
                    this.streamingState.add(new Pair<>(address, data));
                    this.streamingState.sort(new NodeStateComparator());
                }
                else
                    throw new UpdateNodeStateException("[" + LocalDateTime.now() + "]: Object not of type List<?>. (origin at [" + address + "])");
            }
            else
                throw new UpdateNodeStateException("[" + LocalDateTime.now() + "]: List<Object> not of right size! (origin at [" + address + "])");
        }
        else
            throw new UpdateNodeStateException("[" + LocalDateTime.now() + "]: Object not of type List<?> (origin at [" + address + "])");
    }

    public String getBestToReceive()
    {
        return this.streamingState.isEmpty() ? null : this.streamingState.get(0).first();
    }

    public String getBestToReceiveServerID()
    {
        return this.streamingState.isEmpty() ? null : (String) this.streamingState.get(0).second().get(0);
    }

    public List<String> getBestRoute()
    {
        List<String> bestRoute = null;
        if(!this.streamingState.isEmpty())
        {
            bestRoute = new ArrayList<>();
            List<Object> route = (List<Object>) this.streamingState.get(0).second().get(3);
            for(Object address : route)
                bestRoute.add((String) address);
        }

        return bestRoute;
    }

    public Set<String> getParentAddresses()
    {
        Set<String> parents = new HashSet<>();
        for(Pair<String, List<Object>> info : this.streamingState)
            parents.add(info.first());

        return parents;
    }
}
