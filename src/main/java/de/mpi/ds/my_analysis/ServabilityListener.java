package de.mpi.ds.my_analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

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
        writeMapCompressed(myActivityStartHandler.getSuccsessfullTrips(), outPath, eol);
    }

    private void writeMapCompressed(Map<Id<Person>, Boolean> map, String outPath, String eol) {
        try {
            FileOutputStream outputStream = new FileOutputStream(Paths.get(outPath, "trip_success.csv.gz").toString());
            Writer writer = new OutputStreamWriter(new GZIPOutputStream(outputStream));
            try {
                for (Map.Entry<Id<Person>, Boolean> entry : map.entrySet()) {
                    writer.append(entry.getKey().toString())
                            .append(';')
                            .append(entry.getValue().toString())
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
