package org.server;

import org.json.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


// Class that stores the config.json information
public class Overlay {

    private static final String UNDEFINED_ID = "UNDEFINED";
    private List<Pair<String, List<String>>> origin;
    private Map<String, List<List<String>>> neighbours;
    private List<String> criticalNodesID;

    private int criticalNodes;
    private int nNodes;

    public Overlay(String configPathStr)
    {
        this.origin = new ArrayList<>();
        this.neighbours = new HashMap<>();
        this.criticalNodesID = new ArrayList<>();
        this.nNodes = 0;
        this.criticalNodes = 0;

        Path configPath = Path.of(configPathStr);
        if(Files.exists(configPath))
        {
            try{
                JSONArray config = new JSONArray(Files.readString(configPath));
                // Iterate all the nodes present in the configuration file.
                for(int i = 0; i < config.length(); ++i, this.nNodes++)
                {
                    JSONObject node = config.getJSONObject(i);

                    String id = node.getString("ID");
                    this.origin.add(new Pair<>(id, new ArrayList<>()));

                    boolean critical = node.getBoolean("Critical");
                    if(critical)
                    {
                        this.criticalNodes++;
                        this.criticalNodesID.add(id);
                    }
                    JSONArray nodeOrigin = node.getJSONArray("Origin");
                    for(int j = 0; j < nodeOrigin.length(); ++j)
                    {
                        this.origin.get(i).getSecond().add(nodeOrigin.getString(j));
                    }

                    this.neighbours.put(id, new ArrayList<>());
                    JSONArray nodeNeighbours = node.getJSONArray("Neighbours");
                    for(int j = 0; j < nodeNeighbours.length(); ++j)
                    {
                        List<String> interfaces = new ArrayList<>();
                        JSONArray nodeNeighbourInterfaces = nodeNeighbours.getJSONArray(j);
                        for(int k = 0; k < nodeNeighbourInterfaces.length(); ++k)
                        {
                            interfaces.add(nodeNeighbourInterfaces.getString(k));
                        }
                        this.neighbours.get(id).add(interfaces);
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public int getCriticalNodes() {
        return criticalNodes;
    }

    public int getnNodes() {
        return nNodes;
    }

    public DataOutputStream sendAdjacents(Socket nodeConnection)
    {
        String address = nodeConnection.getInetAddress().getHostAddress();
        String id = getNodeID(address);
        DataOutputStream out = null;
        if(!id.equals(UNDEFINED_ID))
        {
            String adjacents = getAdjacents(id);
            System.out.println(adjacents);

            try
            {
                out  = new DataOutputStream(nodeConnection.getOutputStream());
                out.writeUTF(adjacents);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            System.out.println(UNDEFINED_ID + " at " + address + ".");
        }

        return out;
    }

    private String getNodeID(String address)
    {
        String id = UNDEFINED_ID;
        for (Pair<String, List<String>> node : this.origin)
        {
            for (String nodeAddress : node.getSecond())
            {
                if (nodeAddress.equals(address))
                {
                    id = node.getFirst();
                    break;
                }
            }
        }
        return id;
    }

    private String getAdjacents(String id)
    {
        StringBuilder adjacents = new StringBuilder();
        List<List<String>> allAdjacents = this.neighbours.get(id);
        for (List<String> adjacentInterfaces : allAdjacents)
        {
            adjacents.append(adjacentInterfaces.get(0)).append(",");
        }
        
        return adjacents.toString().substring(0,adjacents.length() - 1);
    }

    public boolean isCritical(String address)
    {
        return this.criticalNodesID.contains(this.getNodeID(address));
    }
}