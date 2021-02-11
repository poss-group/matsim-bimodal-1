package de.mpi.ds.my_analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class ServabilityListener implements IterationStartsListener, IterationEndsListener {
    private final static Logger LOG = Logger.getLogger(ServabilityListener.class.getName());
    private MyActivityStartHandler myActivityStartHandler;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        myActivityStartHandler = new MyActivityStartHandler(event.getServices().getScenario());
        event.getServices().getEvents().addHandler(myActivityStartHandler);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        String eol = System.getProperty("line.separator");
        String outPath = event.getServices().getControlerIO().getOutputPath();
        writeMapsCompressed(myActivityStartHandler.getSuccsessfullTrips(), myActivityStartHandler.getLastCoords(),
                outPath, eol);
    }

    private void writeMapsCompressed(Map<Id<Person>, Boolean> map, Map<Id<Person>, Coord> map2, String outPath,
                                     String eol) {
        try {
            FileOutputStream outputStream = new FileOutputStream(Paths.get(outPath, "trip_success.csv.gz").toString());
            Writer writer = new OutputStreamWriter(new GZIPOutputStream(outputStream));
            try {
                writer.append("personId;tripSuccess;lastCoordX;lastCoordY\n");
                for (Id<Person> key : map.keySet()) {
                    String xCoord = "";
                    String yCoord = "";
                    if (map2.get(key) != null) {
                        xCoord = String.valueOf(map2.get(key).getX());
                        yCoord = String.valueOf(map2.get(key).getY());
                    }
                    writer.append(key.toString())
                            .append(';')
                            .append(map.get(key).toString())
                            .append(';')
                            .append(xCoord)
                            .append(';')
                            .append(yCoord)
                            .append(eol);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                writer.close();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
