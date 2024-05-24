package org.matsim.contrib.smartcity.actuation.semaphore;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.*;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.analisys.BidEvent;
import org.matsim.contrib.smartcity.comunication.BidMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.contrib.smartcity.comunication.DecriseBudget;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;
import org.matsim.contrib.smartcity.perception.camera.ActiveCamera;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.contrib.smartcity.perception.camera.CameraListener;
import org.matsim.contrib.smartcity.perception.camera.CameraStatus;
import org.matsim.contrib.smartcity.perception.wrapper.ActivePerceptionWrapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.stream.Stream;

public class BidsSemaphoreController implements SignalController, ComunicationServer, CameraListener {

    public static final String IDENTIFIER = "SmartSemphoreController";

    private double minimunGreenTime = 20;

    @Inject
    private ComunicationFixedWrapper commWrapper;
    @Inject
    private ActivePerceptionWrapper percWrapper;
    @Inject
    private Network network;
    @Inject
    private EventsManager em;

    private SignalSystem system;
    private HashMap<Id<Link>, Integer> bidMap;
    private HashMap<Id<Link>, SignalGroup> signalMap;
    private HashMap<Id<Link>, List<Bid>> agentMap;
    private HashMap<Id<Link>, Integer> totalAgents;
    private Id<SignalGroup> actualGreen;
    private double lastChange;
    private float m = 0;
    private float M = 1.1f;

    @Override
    public void updateState(double timeSeconds) {
        if (timeSeconds - this.lastChange < this.minimunGreenTime)
            return;
        //trovo che semaforo mettere
        setBidForStaticAgents();
        NextGreen nextGreen = getNextGreen();
        Id<SignalGroup> nextGreenGroup = nextGreen.getGroup();
        Id<Link> nextGreenLink = nextGreen.getLink();

        this.system.scheduleOnset(timeSeconds, nextGreenGroup);
        if (this.actualGreen != null && this.actualGreen != nextGreenGroup)
            this.system.scheduleDropping(timeSeconds, this.actualGreen);

        this.actualGreen = nextGreenGroup;
        this.lastChange = timeSeconds;

        //azzero quello che ha vinto
        bidMap.put(nextGreenLink, 0);

        //comunico agli agenti che anno vinto di scalare il budget
        if (nextGreenLink != null && this.agentMap.containsKey(nextGreenLink)) {
            this.agentMap.get(nextGreenLink).forEach(b -> b.getAgent().sendToMe(new DecriseBudget(this, b.getBid())));
            //azzero gli agenti che hanno vinto e quindi sono passati
            this.agentMap.put(nextGreenLink, new ArrayList<>());
        }
    }

    private void setBidForStaticAgents() {
        for (Map.Entry<Id<Link>, Integer> e : this.bidMap.entrySet()){
            if (this.totalAgents.get(e.getKey()) == null){
                continue;
            }
            for (int i = this.totalAgents.get(e.getKey())
                    - (this.agentMap.get(e.getKey()) != null ? this.agentMap.get(e.getKey()).size() : 0); i > 0; i--) {

                int bid = 1;
                if (this.agentMap.get(e.getKey()) != null && this.agentMap.get(e.getKey()).size() != 0) {
//                    List<Integer> bids = this.agentMap.get(e.getKey()).stream().map(Bid::getBid).collect(Collectors.toList());
//                    int min = Math.round(this.m * Collections.min(bids));
//                    int max = Math.round(this.M * Collections.max(bids));
//
//                    if (max - min <= 0){
//                        bid = min;
//                    } else {
//                        bid = min + new Random().nextInt(max - min);
//                    }
                    bid = (int) Math.round(this.agentMap.values().stream().flatMap(Collection::stream)
                            .mapToInt(Bid::getBid).average()
                            .orElse(1L));
                }

                int actual = e.getValue();
                this.bidMap.put(e.getKey(), actual + bid);
            }
        }
    }

    private NextGreen getNextGreen() {
        int max = 0;
        Id<Link> link = null;
        for (Map.Entry<Id<Link>, Integer> e : this.bidMap.entrySet()){
            if (e.getValue() > max){
                max = e.getValue();
                link = e.getKey();
            }
        }

        if (link != null){
            return new NextGreen(this.signalMap.get(link).getId(), link);
        } else {
            return new NextGreen(getRandomSignal(), null);
        }
    }

    private Id<SignalGroup> getRandomSignal() {
        List<Id<SignalGroup>> signals = new ArrayList<>();
        for (SignalGroup s : this.signalMap.values()) {
            if (!s.getId().equals(this.actualGreen)) {
                signals.add(s.getId());
            }
        }

        return signals.get(MatsimRandom.getRandom().nextInt(signals.size()));
    }

    @Override
    public void addPlan(SignalPlan plan) {

    }

    @Override
    public void reset(Integer iterationNumber) {

    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {

    }

    @Override
    public void setSignalSystem(SignalSystem signalSystem) {
        this.system = signalSystem;
        this.bidMap = new HashMap<>();
        this.signalMap = new HashMap<>();
        this.agentMap = new HashMap<>();
        this.totalAgents = new HashMap<>();

        Stream<Tuple<SignalGroup, Signal>> signals = signalSystem.getSignalGroups().values().stream().flatMap(
                o -> o.getSignals().values().stream().map( s -> new Tuple<SignalGroup, Signal>(o, s)));
        signals.forEach(tupla -> {
            Signal s = tupla.getSecond();
            SignalGroup sg = tupla.getFirst();
            Id<Link> link = s.getLinkId();
            this.signalMap.put(link, sg);
            this.bidMap.put(link, 0);

            Coord pos = network.getLinks().get(link).getCoord();
            commWrapper.addFixedComunicator(this, Collections.singleton(pos));
            ActiveCamera cam = new ActiveCamera(Id.create(link.toString(), Camera.class), link, percWrapper);
            cam.addCameraListener(this);

            this.actualGreen = sg.getId();
        });
    }

    @Override
    public void sendToMe(ComunicationMessage message) {
        if (message instanceof BidMessage) {
            Id<Link> link = ((BidMessage) message).getLink();
            int bid = ((BidMessage) message).getBid();
            BidAgent agent = (BidAgent) ((BidMessage) message).getSender();
            double time = ((BidMessage) message).getTime();

            em.processEvent(new BidEvent(link, agent, bid, time));

            if (bid <= 0) {
                return;
            }

            BidAgent.BidAgentMode mode = ((BidMessage) message).getMode();
            int actual = this.bidMap.get(link);

            //Scalare la puntata se è richiesta per una strada attualmente verde
            if (this.signalMap.get(link).getId().equals(this.actualGreen)){
                bid = bid / 10;
            }
            this.bidMap.put(link, actual + bid);

            addBidToMap(link, agent, bid, mode);
        }
    }

    private void addBidToMap(Id<Link> link, BidAgent agent, int bid, BidAgent.BidAgentMode mode) {
        List<Bid> bidForLink = this.agentMap.get(link);
        if (bidForLink == null) {
            bidForLink = new ArrayList<Bid>();
        }

        bidForLink.add(new Bid(agent, bid, mode));
        this.agentMap.put(link, bidForLink);
    }

    public boolean controlLink(Id<Link> link) {
        return this.signalMap.containsKey(link);
    }

    @Override
    public void pushCameraStatus(CameraStatus status, double time) {
        this.totalAgents.put(status.getIdLink(), status.getLinkStatus().getTotal());
    }

    private static class Bid {

        private BidAgent agent;
        private int bid;
        private BidAgent.BidAgentMode agentMode;

        Bid(BidAgent agent, int bid, BidAgent.BidAgentMode mode){
            this.agent = agent;
            this.bid = bid;
            this.agentMode = mode;
        }

        public BidAgent getAgent() {
            return agent;
        }

        public void setAgent(BidAgent agent) {
            this.agent = agent;
        }

        public Integer getBid() {
            return bid;
        }

        public void setBid(int bid) {
            this.bid = bid;
        }

        public BidAgent.BidAgentMode getMode() {
            return this.agentMode;
        }
    }

    private static class NextGreen {

        private Id<SignalGroup> group;
        private Id<Link> link;

        NextGreen(Id<SignalGroup> group, Id<Link> link) {
            this.group = group;
            this.link = link;
        }

        public Id<SignalGroup> getGroup() {
            return group;
        }

        public void setGroup(Id<SignalGroup> group) {
            this.group = group;
        }

        public Id<Link> getLink() {
            return link;
        }

        public void setLink(Id<Link> link) {
            this.link = link;
        }
    }
}
