package org.matsim.contrib.smartcity.analisys;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class CalcSemaphoreCrossingTime implements LinkEnterEventHandler, LinkLeaveEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, ShutdownListener, SignalGroupStateChangedEventHandler {

    private static final String SEMAPHORE_CROSSING_TIME_FILENAME = "semaphore_time.txt.gz";
    private static final String GREENWAVE_FILENAME = "greenwave.txt.gz";

    private Set<Id<Link>> links;
    private HashMap<Id<Vehicle>, Double>  enteredTime = new HashMap<>();
    private List<AttendingTime> times = new ArrayList<>();
    private HashMap<Id<Link>, ArrayList<Id<Vehicle>>> possibleGreenWave = new HashMap<>();
    private HashMap<Id<Vehicle>, Integer> greenWaveCounter = new HashMap<>();
    private HashMap<Id<Vehicle>, Integer> trafficLightCounter = new HashMap<>();
    private Map<Id<Link>, SignalGroup> linkSignalMap = new HashMap<>();

    private Map<Id<SignalGroup>, SignalGroup> signalGroupMap;

    //for caching
    private HashMap<Id<Vehicle>, Person> personCache = new HashMap<>();

    @Inject
    private Scenario scenario;
    @Inject
    private OutputDirectoryHierarchy controlerIO;
    @Inject
    Injector inj;

    private SignalSystemsManager signalSystemsManager;

    private void setLink() {
        this.links = new HashSet<>(scenario.getNetwork().getLinks().keySet());
        this.signalSystemsManager = inj.getInstance(SignalSystemsManager.class);
        this.linkSignalMap = signalSystemsManager.getSignalSystems().values().stream().flatMap(s -> s.getSignalGroups().values().stream())
                .flatMap(g -> g.getSignals().values().stream().map(Signal::getLinkId).map(l -> new Tuple<>(l, g)))
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
        this.signalGroupMap = signalSystemsManager.getSignalSystems().values().stream().flatMap(s -> s.getSignalGroups().entrySet().stream())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        if (this.links == null){
            setLink();
        }
        vehicleEntered(linkEnterEvent.getVehicleId(), linkEnterEvent.getLinkId(), linkEnterEvent.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if (this.links == null){
            setLink();
        }
        vehicleLeaved(linkLeaveEvent.getVehicleId(), linkLeaveEvent.getLinkId(), linkLeaveEvent.getTime());
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
        if (this.links == null){
            setLink();
        }
        vehicleEntered(vehicleEntersTrafficEvent.getVehicleId(), vehicleEntersTrafficEvent.getLinkId(), vehicleEntersTrafficEvent.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent) {
        if (this.links == null){
            setLink();
        }
        //vehicleLeaved(vehicleLeavesTrafficEvent.getVehicleId(), vehicleLeavesTrafficEvent.getLinkId(), vehicleLeavesTrafficEvent.getTime());
        this.enteredTime.remove(vehicleLeavesTrafficEvent.getVehicleId());
    }

    private void vehicleLeaved(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        if (this.enteredTime.containsKey(vehicleId)){
        	double start = this.enteredTime.get(vehicleId);
            double totalTime = time - start;
            Person p = this.personCache.get(vehicleId);
            Id<Person> personId = p.getId();
            this.times.add(new AttendingTime(linkId, personId, totalTime, start));
            this.enteredTime.remove(vehicleId);
        }
        if (this.possibleGreenWave.containsKey(linkId)) {
            ArrayList<Id<Vehicle>> array = this.possibleGreenWave.get(linkId);
            if (array.contains(vehicleId)) {
                incrementVehicleGreenWave(vehicleId);
            }
        }
    }

    private void incrementVehicleGreenWave(Id<Vehicle> vehicleId) {
        this.greenWaveCounter.put(vehicleId, this.greenWaveCounter.getOrDefault(vehicleId, 0) + 1);
    }

    private void vehicleEntered(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        if (this.links.contains(linkId)){
            this.enteredTime.put(vehicleId, time);
            updateCache(vehicleId);
        }
        if (linkHasTrafficLight(linkId)) {
            incrementTrafficLightCounter(vehicleId);
            if (isTrafficLightGreen(linkId)) {
                addVehicleToPossibleGreenWave(vehicleId, linkId);
            }
        }
    }

    private boolean isTrafficLightGreen(Id<Link> linkId) {
        return this.linkSignalMap.get(linkId).getState() == SignalGroupState.GREEN;
    }

    private boolean linkHasTrafficLight(Id<Link> linkId) {
        return this.linkSignalMap.containsKey(linkId);
    }

    private void addVehicleToPossibleGreenWave(Id<Vehicle> vehicleId, Id<Link> linkId) {
        ArrayList<Id<Vehicle>> oldArray = this.possibleGreenWave.getOrDefault(linkId, new ArrayList<>());
        oldArray.add(vehicleId);
        this.possibleGreenWave.put(linkId, oldArray);
    }

    private void incrementTrafficLightCounter(Id<Vehicle> vehicleId) {
        this.trafficLightCounter.put(vehicleId, this.trafficLightCounter.getOrDefault(vehicleId, 0) + 1);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            GZIPOutputStream writer = new GZIPOutputStream(Files.newOutputStream(Paths.get(controlerIO.getOutputFilename(SEMAPHORE_CROSSING_TIME_FILENAME))));
            //PrintWriter writer = new PrintWriter();
            writer.write("Link\tTime\tAgent\tStart\tAgentAttrs\n".getBytes());
            for (AttendingTime t : this.times) {
                //Assumento che il veicolo abbia lo stesso id dell'agente
                writer.write((t.getLink()+"\t"+t.getTime()+"\t"+t.getPerson()+"\t"+t.getStart()+"\t"
                        + String.join(",", t.getAttrs())
                        +"\n").getBytes());
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            GZIPOutputStream writer = new GZIPOutputStream(Files.newOutputStream(Paths.get(controlerIO.getOutputFilename(GREENWAVE_FILENAME))));
            writer.write("Agent\tRatio\n".getBytes());
            for (Id<Vehicle> v : this.trafficLightCounter.keySet()) {
                Integer total = this.trafficLightCounter.get(v);
                Integer green = this.greenWaveCounter.getOrDefault(v, 0);
                writer.write((v.toString()+"\t" + green.doubleValue() / total +"\n").getBytes());
            }
            writer.close();
        } catch (IOException e) {

        }
    }

    private void updateCache(Id<Vehicle> vehicleId) {
        if (!this.personCache.containsKey(vehicleId)) {
            Person p = scenario.getPopulation().getPersons().get(Id.create(vehicleId, Person.class));
            this.personCache.put(vehicleId, p);
        }
    }

    @Override
    public void handleEvent(SignalGroupStateChangedEvent event) {
        if (this.signalGroupMap == null){
            setLink();
        }
        if (event.getNewState() == SignalGroupState.RED) {
            this.signalGroupMap.get(event.getSignalGroupId()).getSignals().values().stream().map(Signal::getLinkId)
                    .forEach(l -> this.possibleGreenWave.remove(l));
        }
    }


    private class AttendingTime {

        private final Double start;
        private Id<Link> link;
        private Double time;
        private Id<Person> person;
        private Set<String> attrs;

        public AttendingTime(Id<Link> link, Id<Person> person, Double time, Double start) {
            this.link = link;
            this.time = time;
            this.person = person;
            this.start = start;

            Person p = scenario.getPopulation().getPersons().get(person);
            this.attrs = p.getAttributes().getAsMap().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toSet());
            
        }

        public Id<Link> getLink() {
            return link;
        }

        public void setLink(Id<Link> link) {
            this.link = link;
        }

        public Double getTime() {
            return time;
        }

        public void setTime(Double time) {
            this.time = time;
        }

        public Id<Person> getVehicle() {
            return person;
        }

        public void setVehicle(Id<Person> person) {
            this.person = person;
        }

        public Set<String> getAttrs() {
            return this.attrs;
        }

        public Object getPerson() {
            return this.person;
        }

        public Double getStart() {
            return start;
        }
    }
}
