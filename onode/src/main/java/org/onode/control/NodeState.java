package org.onode.control;

import org.exceptions.UpdateNodeStateException;
import org.onode.utils.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class NodeState
{
    private static final int OBJECT_LIST_SIZE = 6;
    private static final int OBJECT_LIST_IDXofLIST = OBJECT_LIST_SIZE - 1;
    private static final int SERVER_ID_IDX = 0;
    private static final int JUMPS_IDX = 1;
    private static final int ELAPSED_TIME_IDX = 2;
    private static final int FLOOD_ID_IDX = 4;


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
            // (String)serverID, (int)jumps, (long)elapsedTime, (long)serverTimestamp (int)floodID, (List<String>)route
            int jumps_1 = (int) floodInfo_1.second().get(JUMPS_IDX);
            int jumps_2 = (int) floodInfo_2.second().get(JUMPS_IDX);

            long elapsedTime_1 = (long) floodInfo_1.second().get(ELAPSED_TIME_IDX);
            long elapsedTime_2 = (long) floodInfo_2.second().get(ELAPSED_TIME_IDX);

            int elapsedTimeCompare = Long.compare(elapsedTime_1, elapsedTime_2);
            boolean elapsedTimeRelation = equalsWithError(elapsedTimeCompare, elapsedTime_1, elapsedTime_2);

            // Means we assume that timestamps are equal!
            if(elapsedTimeRelation)
                return Integer.compare(jumps_1, jumps_2);
            else
                return elapsedTimeCompare;
        }
    }

    // [pair (address "pai", [(String)serverID, (int)jumps, (long)elapsedTime, (long)serverTimestamp, (int)floodID, (List<String>)route])]
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
            if(data.size() == OBJECT_LIST_SIZE)
            {
                if(data.get(OBJECT_LIST_IDXofLIST) instanceof List<?>)
                {
                    // Remove old flood info (of given server ID and floodID).
                    this.removeOldFloodInfo((String) data.get(SERVER_ID_IDX), (int) data.get(FLOOD_ID_IDX));

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

    private void removeOldFloodInfo(String serverID, int floodID)
    {
        List<Pair<String, List<Object>>> toRemove = new ArrayList<>();
        for (Pair<String, List<Object>> stringListPair : this.streamingState)
        {
            // [(String)serverID, (int)jumps, (long)elapsedTime, (int)floodID, (List<String>)route]
            List<Object> data = stringListPair.second();
            String i_serverID = (String) data.get(SERVER_ID_IDX);
            int i_floodID = (int) data.get(FLOOD_ID_IDX);
            if (serverID.equals(i_serverID) && floodID > i_floodID)
                toRemove.add(stringListPair);
        }

        this.streamingState.removeAll(toRemove);
        toRemove.clear();
    }

    public String getBestToReceive()
    {
        return this.streamingState.isEmpty() ? null : this.streamingState.get(0).first();
    }
}
