package de.mpi.ds.DrtTrajectoryAnalyzer;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class TrajectoryLinkLogger implements LinkEnterEventHandler, IterationEndsListener {
    private final static Logger LOG = Logger.getLogger(TrajectoryLinkLogger.class.getName());

    private class LinkTimestampContainer {
        private Id<Link> linkId;
        private double timestamp;

        LinkTimestampContainer(Id<Link> linkId, double timestamp) {
            this.linkId = linkId;
            this.timestamp = timestamp;
        }

        public double getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(double timestamp) {
            this.timestamp = timestamp;
        }

        public Id<Link> getLinkId() {
            return linkId;
        }

        public void setLinkId(Id<Link> linkId) {
            this.linkId = linkId;
        }
    }

    private static Map<Id<Vehicle>, List<LinkTimestampContainer>> trajectories = new HashMap<>();
//    private static Map<Id<Vehicle>, List<LinkTimestampContainer>> trajectories = new LinkedHashMap<>();

//    private void initializeVehicles(Vehicles vehicles) {
//        for (Vehicle vehicle: vehicles.getVehicles().values()) {
//            if (vehicle instanceof DvrpVehicle) {
//                trajectories.put(vehicle.getId(), new ArrayList<>());
//            }
//        }
//    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        List<LinkTimestampContainer> list = trajectories.get(linkEnterEvent.getVehicleId());
        if (list == null && linkEnterEvent.getVehicleId().toString().contains("drt")) {
            trajectories.put(linkEnterEvent.getVehicleId(), new ArrayList<>());
            LinkTimestampContainer data = new LinkTimestampContainer(linkEnterEvent.getLinkId(),
                    linkEnterEvent.getTime());
            trajectories.get(linkEnterEvent.getVehicleId()).add(data);
        } else if (list != null) {
            LinkTimestampContainer data = new LinkTimestampContainer(linkEnterEvent.getLinkId(),
                    linkEnterEvent.getTime());
            trajectories.get(linkEnterEvent.getVehicleId()).add(data);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        writeJSON(iterationEndsEvent, true);
    }

    private void writeJSON(IterationEndsEvent iterationEndsEvent, boolean compressed) {
        String filename = iterationEndsEvent.getServices().getControlerIO()
                .getIterationFilename(iterationEndsEvent.getIteration(),
                        "drt_trajectories.json" + (compressed ? ".gz" : ""));

        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<Id<Vehicle>, List<LinkTimestampContainer>> entry : trajectories.entrySet()) {
            Map<String, String> element = new LinkedHashMap<>();
            for (LinkTimestampContainer data : entry.getValue()) {
                element.put(String.valueOf(data.getTimestamp()), data.getLinkId().toString());
            }
            root.put(String.valueOf(entry.getKey()), element);
        }

        // write output
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            if (compressed) {
                FileOutputStream fStream = null;
                GZIPOutputStream zStream = null;
                try {
                    fStream = new FileOutputStream(filename);
                    zStream = new GZIPOutputStream(new BufferedOutputStream(fStream));
                    writer.writeValue(zStream, root);
                } finally {
                    if (zStream != null) {
                        zStream.flush();
                        zStream.close();
                    }
                    if (fStream != null) {
                        fStream.flush();
                        fStream.close();
                    }
                }
            } else {
                writer.writeValue(new File(filename), root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeXML(IterationEndsEvent iterationEndsEvent, boolean compressed) {
        String filename = iterationEndsEvent.getServices().getControlerIO()
                .getIterationFilename(iterationEndsEvent.getIteration(),
                        "drt_trajectories.xml" + (compressed ? ".gz" : ""));

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();

            Element root = document.createElement("trajectories");
            document.appendChild(root);
            for (Map.Entry<Id<Vehicle>, List<LinkTimestampContainer>> entry : trajectories.entrySet()) {
                Element el = document.createElement("trajectory");
                Attr attr = document.createAttribute("vehicle-id");
                attr.setValue(entry.getKey().toString());
                el.setAttributeNode(attr);
                root.appendChild(el);

                for (LinkTimestampContainer data : entry.getValue()) {
                    Element el2 = document.createElement("element");
                    Attr attr2 = document.createAttribute("time");
                    attr2.setValue(String.valueOf(data.getTimestamp()));
                    Attr attr3 = document.createAttribute("link-id");
                    attr3.setValue(data.getLinkId().toString());
                    el2.setAttributeNode(attr3);
                    el2.setAttributeNode(attr2);
                    el.appendChild(el2);
                }
            }

            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            if (compressed) {
                FileOutputStream fStream = null;
                GZIPOutputStream zStream = null;
                try {
                    fStream = new FileOutputStream(filename);
                    zStream = new GZIPOutputStream(new BufferedOutputStream(fStream));
                    tr.transform(new DOMSource(document), new StreamResult(zStream));
                } finally {
                    if (zStream != null) {
                        zStream.flush();
                        zStream.close();
                    }
                    if (fStream != null) {
                        fStream.flush();
                        fStream.close();
                    }
                }
            } else {
                tr.transform(new DOMSource(document), new StreamResult(new File(filename)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
