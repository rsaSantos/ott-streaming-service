package org.server;

import org.json.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;


// Class that stores the config.json information
public class Overlay {

    private final List<Triplet<String, String, String>> origins; // nodeID, i#, address
    private String originsStr;
    private final Map<String, List<String>> neighbours; // nodeID, listOfNeighbours "ids:i#"
    private final List<String> criticalNodesID; // List of ids of critical nodes.

    private int criticalNodes;

    public Overlay(String configPathStr)
    {
        this.origins = new ArrayList<>();
        this.neighbours = new HashMap<>();
        this.criticalNodesID = new ArrayList<>();
        this.criticalNodes = 0;

        Path configPath = Path.of(configPathStr);
        if(Files.exists(configPath))
        {
            try{
                JSONArray config = new JSONArray(Files.readString(configPath));
                // Iterate all the nodes present in the configuration file.
                for(int i = 0; i < config.length(); ++i)
                {
                    JSONObject node = config.getJSONObject(i);

                    String id = node.getString("ID");

                    boolean critical = node.getBoolean("Critical");
                    if(critical)
                    {
                        this.criticalNodes++;
                        this.criticalNodesID.add(id);
                    }

                    JSONArray nodeOrigin = node.getJSONArray("Origin");
                    for(int j = 0; j < nodeOrigin.length(); ++j)
                    {
                        // Here, the origin field is: "interface2:0.0.0.0"
                        String[] itIP = nodeOrigin.getString(j).split(":");
                        this.origins.add(new Triplet<>(id, itIP[0], itIP[1]));
                    }

                    List<String> neighboursIdsAndBestI = new ArrayList<>();
                    JSONArray nodeNeighbours = node.getJSONArray("Neighbours");
                    for(int j = 0; j < nodeNeighbours.length(); ++j)
                        neighboursIdsAndBestI.add(nodeNeighbours.getString(j));

                    this.neighbours.put(id, neighboursIdsAndBestI);
                }

                // Build string that represents map of origins.
                // Send map as: idNode,interface,address;...
                StringBuilder _originsStr = new StringBuilder();
                for(Triplet<String, String, String> nodeInterface : this.origins)
                {
                    String id_node = nodeInterface.getFirst();
                    String _interface = nodeInterface.getSecond();
                    String address = nodeInterface.getThird();

                    _originsStr.append(id_node).append(",").append(_interface).append(",").append(address).append(";");
                }
                this.originsStr = _originsStr.substring(0, _originsStr.length() - 1);
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

    private String getIDFromAddress(String address)
    {
        String id = null;
        // nodeID, i#, address
        for(Triplet<String, String, String> triplet : this.origins)
        {
            if(triplet.getThird().equals(address))
            {
                id = triplet.getFirst();
                break;
            }
        }
        return id;
    }

    public DataOutputStream sendAdjacents(Socket nodeConnection)
    {
        String address = nodeConnection.getInetAddress().getHostAddress();
        String id = this.getIDFromAddress(address);
        DataOutputStream out = null;
        if(id != null)
        {
            String adjacents = getAdjacentsInfo(id);
            try
            {
                out  = new DataOutputStream(nodeConnection.getOutputStream());
                out.writeUTF(adjacents);
                out.flush();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            System.out.println("[" + LocalDateTime.now() + "]: Undefined at " + "[\033[0;31m" + address + "\u001B[0m].");
        }

        return out;
    }

    private String getAdjacentsInfo(String nodeID)
    {
        // Get node adjacents
        // k=nodeID, v=listOfNeighbours "ids:i#"
        List<String> adjacentsIDAndBestInt = this.neighbours.get(nodeID);

        // results = origins;adjacents
        // origins format = nodeID,i#,address...
        StringBuilder toSend = new StringBuilder();
        toSend.append(nodeID).append(";").append(this.originsStr).append(";");


        // Send list as: O1:it0,O2:it0,O3:it2...;
        for(String idAndBestInt : adjacentsIDAndBestInt)
            toSend.append(idAndBestInt).append(",");
        toSend.deleteCharAt(toSend.length() - 1);

        return toSend.toString();
    }

    public boolean isCritical(String address)
    {
        String id = this.getIDFromAddress(address);
        return id != null && this.criticalNodesID.contains(id);
    }
}