package de.mpi.ds.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class ConvertPersonsCsv {
    private static final Logger LOG = Logger.getLogger(ConvertPersonsCsv.class.getName());

    private static void convertPersonsFile(String[] args) {
        LOG.info("Starting conversion to new persons format");
        File oldPersons = new File(args[1]);
        Map<String, String> person2Score = new HashMap<>();
        try {
            InputStream fileStream = new FileInputStream(oldPersons.toString());
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader buffered = new BufferedReader(decoder);
            buffered.lines().forEach(l -> {
                String[] line = l.split(";");
                String key = line[0];
                String value = line[1];
                person2Score.put(key, value);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (oldPersons.delete()) {
//            LOG.info("Deleted " + args[1] + " successfully");
//        } else {
//            LOG.error("Failed to delete " + args[1] + " successfully");
//        }

        Population population = PopulationUtils.readPopulation(args[0]);

        LOG.info("Writing all Person and Attributes to " + Controler.DefaultFiles.personscsv);
        List<String> attributes = new ArrayList<>(population.getPersons().values().parallelStream()
                .flatMap(p -> p.getAttributes().getAsMap().keySet().stream()).collect(Collectors.toSet()));
        attributes.remove("vehicles");
        List<String> header = new ArrayList<>();
        header.add("person");
        header.add("executed_score");
        header.add("first_act_x");
        header.add("first_act_y");
        header.add("first_act_type");
        header.add("last_act_x");
        header.add("last_act_y");
        header.add("last_act_type");
        header.addAll(attributes);
        try (CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(args[1]),
                CSVFormat.DEFAULT.withDelimiter(';').withHeader(header.toArray(String[]::new)))) {
            for (Person p : population.getPersons().values()) {
                if (p.getSelectedPlan() == null) {
                    LOG.error("Found person without a selected plan: " + p.getId().toString() +
                            " will not be added to output_persons.csv");
                    continue;
                }
                List<String> line = new ArrayList<>();
                line.add(p.getId().toString());
                line.add(person2Score.get(p.getId().toString()));
                String firstX = "";
                String firstY = "";
                String firstActType = "";
                String lastX = "";
                String lastY = "";
                String lastActType = "";
                List<PlanElement> planElements = p.getSelectedPlan().getPlanElements();
                if (p.getSelectedPlan().getPlanElements().size() > 0) {
                    Activity firstAct = (Activity) planElements.get(0);
                    if (firstAct.getCoord() != null) {
                        firstX = Double.toString(firstAct.getCoord().getX());
                        firstY = Double.toString(firstAct.getCoord().getY());
                    }
                    firstActType = firstAct.getType();
                    for (int i = planElements.size() - 1; i >= 0; i--) {
                        if (planElements.get(i) instanceof Activity) {
                            Activity act = (Activity) planElements.get(i);
                            if (act.getCoord() != null) {
                                lastX = Double.toString(act.getCoord().getX());
                                lastY = Double.toString(act.getCoord().getY());
                            }
                            lastActType = act.getType();
                            break;
                        }
                    }
                }
                line.add(firstX);
                line.add(firstY);
                line.add(firstActType);
                line.add(lastX);
                line.add(lastY);
                line.add(lastActType);
                for (String attribute : attributes) {
                    Object value = p.getAttributes().getAttribute(attribute);
                    String result = value != null ? String.valueOf(value) : "";
                    line.add(result);
                }
                csvPrinter.printRecord(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("...done");
    }

}
