package org.onode.control;

import org.exceptions.UpdateNodeStateException;
import org.onode.utils.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class NodeState implements Runnable
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
            // (int)jumps, (long)timestamp, (long)elapsedTime, (List<String>)route

            int jumps_1 = (int) floodInfo_1.second().get(0);
            int jumps_2 = (int) floodInfo_2.second().get(0);

            long timestamp_1 = (long) floodInfo_1.second().get(1);
            long timestamp_2 = (long) floodInfo_2.second().get(1);

            long elapsedTime_1 = (long) floodInfo_1.second().get(2);
            long elapsedTime_2 = (long) floodInfo_2.second().get(2);

            int timestampCompare = Long.compare(timestamp_1, timestamp_2);
            boolean timestampRelation = equalsWithError(timestampCompare, timestamp_1, timestamp_2);

            int elapsedTimeCompare = Long.compare(elapsedTime_1, elapsedTime_2);
            boolean elapsedTimeRelation = equalsWithError(elapsedTimeCompare, elapsedTime_1, elapsedTime_2);

            // Means we assume that timestamps are equal!
            if(elapsedTimeRelation)
                if(timestampRelation)
                    return Integer.compare(jumps_1, jumps_2);
                else
                    return timestampCompare;
            else
                return elapsedTimeCompare;
        }
    }

    // [pair (address "pai", [jumps, timestamp, elapsedTime, route])]
    // Sorted by fastest!
    private final List<Pair<String, List<Object>>> streamingState;
    
    public NodeState()
    {
        streamingState = new ArrayList<>();
    }

    @Override
    public void run() {

    }

    public void update(Pair<String, Object> floodInfo) throws UpdateNodeStateException {
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
}
