package de.mpi.ds.osm_utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.mpi.ds.osm_utils.NetworkCreatorFromOsm.setLinkAttributes;
import static de.mpi.ds.utils.NetworkCreator.copyLinkProperties;

public class NetworkCleaner {
    private long linkCapacity;
    private double freeSpeedCar;
    private double numberOfLanes;

    public NetworkCleaner(long linkCapacity, double freeSpeedCar, double numberOfLanes) {
        this.linkCapacity = linkCapacity;
        this.freeSpeedCar = freeSpeedCar;
        this.numberOfLanes = numberOfLanes;
    }

    public Network cleanNetwork(String path) {

        Network net = null;
        Pattern p = Pattern.compile(".*/network_clean.xml");
        Matcher m = p.matcher(path);
        if (!m.matches()) {
            Network originNet = NetworkUtils.readNetwork(path);
            double minX = originNet.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
            double minY = originNet.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();

            net = centerAndRenameNodes(originNet, minX, minY);
            String[] filenameArray = path.split("/");
            File outFile = new File(path.replace(filenameArray[filenameArray.length - 1], "network_clean.xml"));
            NetworkUtils.writeNetwork(net, outFile.getAbsolutePath());
            return net;
        }
        return NetworkUtils.readNetwork(path);
    }

    private Network centerAndRenameNodes(Network net, double minX, double minY) {
        // No setId method available
        Network newNet = NetworkUtils.createNetwork();
        Map<Node, Node> oldNewMapping = new HashMap<>();
        ArrayList<Node> oldNodes = new ArrayList<>(net.getNodes().values());
        ArrayList<Link> oldLinks = new ArrayList<>(net.getLinks().values());
        for (int i = 0; i < oldNodes.size(); i++) {
            Node oldNode = oldNodes.get(i);
            Node newNode = net.getFactory().createNode(Id.createNodeId(i),
                    new Coord(oldNode.getCoord().getX() - minX, oldNode.getCoord().getY() - minY));
            oldNewMapping.put(oldNode, newNode);
        }
        for (Node n : oldNewMapping.values()) {
            newNet.addNode(n);
        }
        for (Link l : oldLinks) {
            Node newFrom = oldNewMapping.get(l.getFromNode());
            Node newTo = oldNewMapping.get(l.getToNode());
            Link newLink = net.getFactory()
                    .createLink(Id.createLinkId(newFrom.getId().toString() + "-" + newTo.getId().toString()), newFrom,
                            newTo);
            copyLinkProperties(l, newLink);
            setLinkAttributes(newLink, linkCapacity, freeSpeedCar, numberOfLanes, false);
            if (!netContainsLink(newNet, newLink)) {
                newNet.addLink(newLink);
            }
        }
        return newNet;
    }

    private boolean netContainsLink(Network newNet, Link newLink) {
        return newNet.getLinks().values().stream().map(Link::getId).anyMatch(lId -> lId.equals(newLink.getId()));
    }
}
