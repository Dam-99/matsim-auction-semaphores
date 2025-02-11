package org.matsim.contrib.smartcity.analisys;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.smartcity.agent.SmartAgentFactory;
import org.matsim.contrib.smartcity.agent.StaticDriverLogic;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SemaphorePredictionAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler,
        VehicleLeavesTrafficEventHandler, SemaphorePredictionEventHandler, ShutdownListener {

    private static final String SEMAPHORE_PREDICTION_ANALYZER_FILENAME = "semaphore_prediction.txt";

    protected HashMap<Id<Link>, List<Id<Vehicle>>> trafficMap;
    protected HashMap<Id<Link>, List<Id<Vehicle>>> beforeTrafficMap;
    protected HashMap<Id<Vehicle>, Id<Link>> beforeVehicleMapping;
    protected Scenario scenario;
    protected HashMap<Id<Vehicle>, Prediction> toFollow;
    protected HashMap<Tuple<Id<Lane>, Double>, Prediction> predictions;
    private HashMap<Id<Link>,SignalGroup> signalMap;

    @Inject
    private OutputDirectoryHierarchy controlerIO;

    @Inject
    public SemaphorePredictionAnalyzer(Network network, Scenario scenario) {
        this.scenario = scenario;
        this.toFollow = new HashMap<Id<Vehicle>, Prediction>();
        this.predictions = new HashMap<Tuple<Id<Lane>, Double>, Prediction>();
        this.trafficMap = new HashMap<Id<Link>, List<Id<Vehicle>>>();
        this.beforeTrafficMap = new HashMap<Id<Link>, List<Id<Vehicle>>>();
        for (Id<Link> id : network.getLinks().keySet()) {
            this.trafficMap.put(id, new ArrayList<Id<Vehicle>>());
            this.beforeTrafficMap.put(id, new ArrayList<>());
        }
        this.beforeVehicleMapping = new HashMap<>();
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Link> idLink = event.getLinkId();
        Id<Vehicle> idVehicle = event.getVehicleId();
        this.trafficMap.get(idLink).add(idVehicle);

        if (this.toFollow.containsKey(event.getVehicleId())){
            Prediction v = this.toFollow.get(event.getVehicleId());
            if (v.predicted.containsKey(event.getLinkId())){
                v.addArrived(Id.create(event.getLinkId().toString()+ ".ol", Lane.class));
                this.toFollow.remove(event.getVehicleId());
            }
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Link> idLink = event.getLinkId();
        Id<Vehicle> idVehicle = event.getVehicleId();
        this.trafficMap.get(idLink).remove(idVehicle);
        this.beforeTrafficMap.get(idLink).add(idVehicle);
        if (this.beforeVehicleMapping.containsKey(idVehicle)){
            this.beforeTrafficMap.get(this.beforeVehicleMapping.get(idVehicle)).remove(idVehicle);
        }
        this.beforeVehicleMapping.put(idVehicle, idLink);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent) {
        this.toFollow.remove(vehicleLeavesTrafficEvent.getVehicleId());
    }

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        try {
            PrintWriter writer = new PrintWriter(controlerIO.getOutputFilename(SEMAPHORE_PREDICTION_ANALYZER_FILENAME));
            writer.write("Time\tLinkStart\tLinkPred\tPredicted\tArrived\n");
            for (Tuple<Id<Lane>, Double> t : this.predictions.keySet()) {
                Prediction p = this.predictions.get(t);
                Set<Id<Lane>> keySet = new HashSet<Id<Lane>>(p.predicted.keySet());
                //keySet.addAll(p.arrived.keySet());
                for (Id<Lane> linkPred : keySet) {
                    writer.write(t.getSecond().toString() + '\t' + t.getFirst().toString() + '\t' + linkPred.toString() + '\t' + p.predicted.getOrDefault(linkPred, 0) + '\t' + p.arrived.getOrDefault(linkPred, 0) +  "\n");
                }
            }
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void handleEvent(SemaphorePredictionEvent event) {
        //take the NE vehicles in the road
        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
        List<Id<Vehicle>> neAgents = this.trafficMap.get(event.getActualLink()).stream()
                .filter((Id<Vehicle> v) -> persons.get(Id.create(v, Person.class)).getAttributes().getAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME).equals(StaticDriverLogic.class.getCanonicalName())).collect(Collectors.toList());
        List<Id<Vehicle>> alreadyFollowed = neAgents.stream().filter((Id<Vehicle> v) -> toFollow.containsKey(v)).collect(Collectors.toList());
        //se sto seguendo un agente e arriva in un altro semaforo allora segno che è arrivato in un punto e non lo seguo più
        for (Id<Vehicle> v : alreadyFollowed){
            Prediction p = this.toFollow.get(v);
            p.addArrived(event.getActualLink());
            this.toFollow.remove(v);
        }

        for (int i=0; i<event.getPrediction().size(); i++) {
            Id<Vehicle> agent;
            if (neAgents.size() > i) {
                agent = neAgents.get(i);
            }
            else {
                if (this.beforeTrafficMap.get(event.getActualLink()).size() == 0) {
                    return;
                }
                agent = this.beforeTrafficMap.get(event.getActualLink()).get(0);
            }
            Tuple<Id<Lane>, Double> key = new Tuple<>(event.getActualLink(), event.getTime());
            if (!predictions.containsKey(key)){
                predictions.put(key, new Prediction());
            }
            Prediction pred = predictions.get(key);
            pred.addPrediction(Id.create(event.getPrediction().get(i).toString() + ".ol", Lane.class));
            toFollow.put(agent, pred);
        }
    }

    public class Prediction {

        private HashMap<Id<Lane>, Integer> predicted = new HashMap<Id<Lane>, Integer>();
        private HashMap<Id<Lane>, Integer> arrived = new HashMap<Id<Lane>, Integer>();

        public synchronized void addPrediction(Id<Lane> link){
            if (!this.predicted.containsKey(link)){
                this.predicted.put(link, 0);
                this.arrived.put(link, 0);
            }
            this.predicted.put(link, this.predicted.get(link) + 1);
        }

        public synchronized void addArrived(Id<Lane> link){
            this.arrived.put(link, this.arrived.getOrDefault(link, 0)+1);
        }
    }
}
