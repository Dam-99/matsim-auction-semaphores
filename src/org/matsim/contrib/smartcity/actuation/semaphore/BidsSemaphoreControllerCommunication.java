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
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.ComunicationMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.contrib.smartcity.comunication.CrossedMessage;
import org.matsim.contrib.smartcity.comunication.DecriseBudget;
import org.matsim.contrib.smartcity.comunication.IncomingRequest;
import org.matsim.contrib.smartcity.comunication.IncomingResponse;
import org.matsim.contrib.smartcity.comunication.RideMessage;
import org.matsim.contrib.smartcity.comunication.SSBootUpMessage;
import org.matsim.contrib.smartcity.comunication.SemaphoreFlowMessage;
import org.matsim.contrib.smartcity.comunication.SemaphoreServer;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;
import org.matsim.contrib.smartcity.perception.camera.ActiveCamera;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.contrib.smartcity.perception.camera.CameraListener;
import org.matsim.contrib.smartcity.perception.camera.CameraStatus;
import org.matsim.contrib.smartcity.perception.wrapper.ActivePerceptionWrapper;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.lanes.Lane;

public class BidsSemaphoreControllerCommunication extends BidsSemaphoreController implements SignalController, ComunicationServer, CameraListener, ComunicationClient {

    public static final String IDENTIFIER = "SmartSemphoreController";
    private static final Logger log = Logger.getLogger(BidsSemaphoreControllerCommunication.class);

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
    private boolean isSignalSystem;
    /**
     * @TODO: change to log only when agent is in system
     */
    private boolean has3640;
    private boolean logActive;
    private HashMap<Id<Lane>, Integer> bidMap;
    private HashMap<Id<Lane>, SignalGroup> signalMap;
    private ConcurrentHashMap<Id<Lane>, List<Bid>> agentMap;
    private ConcurrentHashMap<Id<Lane>, Integer> totalAgents;
    private ConcurrentHashMap<Id<Lane>, List<Tuple<List<Id<Lane>>, Integer>>> agentsRouteAndBid;
    private Id<SignalGroup> actualGreen;
    private double lastChange;
    private float m = 0;
    private float M = 1.1f;
    private Boolean wait = false;
    private HashMap<Id<Lane>, Integer> incomingBid;
    private SemaphoreServer semaphoreServer;
    private HashMap<Id<Lane>, Double> lastGreen;
    private HashMap<Id<Lane>, Camera> camMap;
    private HashMap<Id<Lane>, Double> lastCameraUpdate;
    private HashMap<Id<Lane>, List<Tuple<Double, Integer>>> changeFromLastUpdate;
    private Lock changeFromLastUpdateLock = new ReentrantLock();

    @Override
    public void updateState(double timeSeconds) {
   
        if (timeSeconds - this.lastChange < this.minimunGreenTime)
            return;
        
        setBidForStaticAgents(timeSeconds);
        
        NextGreen nextGreen = getNextGreen(timeSeconds);
        Id<SignalGroup> nextGreenGroup = nextGreen.getGroup();
        List<Id<Lane>> nextGreenLink = this.system.getSignalGroups().get(nextGreenGroup).getSignals().values().stream()
                .flatMap(s -> s.getLaneIds().stream()).collect(Collectors.toList());

        this.system.scheduleOnset(timeSeconds, nextGreenGroup); // set the change to happen to the system
        if (this.actualGreen != null && this.actualGreen != nextGreenGroup)
            this.system.scheduleDropping(timeSeconds, this.actualGreen);

        // change the state
        this.actualGreen = nextGreenGroup;
        this.lastChange = timeSeconds;

        if (this.logActive && this.isSignalSystem) {
            log.error("green group for problematic system: " + nextGreenLink);
//            if (this.has3640) {
//                log.error("Agent 3640 __IS__ in system");
//            } else {
//                log.error("Agent 3640 __NOT__ in system");
//            }
//            this.logActive = false;
        }

        // TODO: chiedere se questo deve essere commentato o meno
        // azzero quello che ha vinto
        /*
         * bidMap.put(nextGreenLink, 0);
         * if (nextGreenLink != null && this.agentsRouteAndBid.get(nextGreenLink) !=
         * null)
         * this.agentsRouteAndBid.put(nextGreenLink, new
         * ArrayList<Tuple<List<Id<Link>>,Integer>>());
         * 
         * //comunico agli agenti che anno vinto di scalare il budget
         * if (nextGreenLink != null && this.agentMap.containsKey(nextGreenLink)) {
         * this.agentMap.get(nextGreenLink).forEach(b -> b.getAgent().sendToMe(new
         * DecriseBudget(this, b.getBid())));
         * //azzero gli agenti che hanno vinto e quindi sono passati
         * this.agentMap.put(nextGreenLink, new ArrayList<>());
         * }
         */
    }

    private void setBidForStaticAgents(double time) {
        for (Map.Entry<Id<Lane>, Integer> e : this.bidMap.entrySet()){
            this.updateTotalAgents(e.getKey());
            this.changeFromLastUpdateLock.lock();
            int changed = this.getChangedFromLastUpdate(e.getKey(), time);
            //this.changeFromLastUpdate.put(e.getKey(), new ArrayList<>());
            this.changeFromLastUpdateLock.unlock();
            if (this.totalAgents.get(e.getKey()) == null){
                continue;
            }
            int totalAgents = this.totalAgents.get(e.getKey()) + changed;
            for (int i = totalAgents
                    - (this.agentMap.get(e.getKey()) != null ? this.agentMap.get(e.getKey()).size() : 0); i > 0; i--) {

                int bid = 100;
                if (this.agentMap.get(e.getKey()) != null && this.agentMap.get(e.getKey()).size() != 0) {
                    bid = (int) Math.round(this.agentMap.values().stream().flatMap(Collection::stream)
                            .mapToInt(Bid::getBid).average()
                            .orElse(1L));
                }

                this.semaphoreServer.sendToMe(new SemaphoreFlowMessage(this, e.getKey(), bid, time));
                addBidToMap(e.getKey(), null, bid, null); // add to list of bids by agents on this link
                addOrRemoveBidToBidMap(true, bid, e.getKey()); // add bid to the total bid for the link
                if (this.agentsRouteAndBid.get(e.getKey()) == null)
                    this.agentsRouteAndBid.put(e.getKey(), new Vector<Tuple<List<Id<Lane>>, Integer>>());
                // placeholder per mantenere la coerenza tra le due mappe
                // add to the list of routes the agents who bid for the given link
                this.agentsRouteAndBid.get(e.getKey()).add(new Tuple<List<Id<Lane>>, Integer>(new ArrayList(), bid));
            }
        }
    }

    private int getChangedFromLastUpdate(Id<Lane> link, double actualTime) {
        Double lastUpdate = this.lastCameraUpdate.get(link);
        List<Tuple<Double, Integer>> list = this.changeFromLastUpdate.get(link);
        if (lastUpdate == null || list == null) {
            return 0;
        }
        int res = 0;
        List<Tuple<Double, Integer>> toRemove = new ArrayList<>();
        for (Tuple<Double, Integer> e : list) {
            if (e.getFirst() > lastUpdate && e.getFirst() <= actualTime) {
                res += e.getSecond();
                toRemove.add(e);
            }
        }
        list.removeAll(toRemove);
        return res;
    }

    private void updateTotalAgents(Id<Lane> link) {
        LinkTrafficStatus linkStatus = this.camMap.get(link).getCameraStatus().getLinkStatus();
        Tuple<Double, Integer> snap = linkStatus.getTotalSnap();
        int total = snap.getSecond();
        double lastUpdate = snap.getFirst();
        this.changeTotalAgent(link, total, lastUpdate);
    }

    private synchronized void changeTotalAgent(Id<Lane> link, int total, double lastUpdate) {
        this.totalAgents.put(link, total);
        this.lastCameraUpdate.put(link, lastUpdate);
    }

    private synchronized void changeFromLastUpdate(Id<Lane> link, boolean inc, double time) {
        this.changeFromLastUpdateLock.lock();
        List<Tuple<Double, Integer>> old = this.changeFromLastUpdate.get(link);
        if (inc)
            old.add(new Tuple<>(time, 1));
        else
            old.add(new Tuple<>(time, -1));
        this.changeFromLastUpdateLock.unlock();
    }

    private NextGreen getNextGreen(double timeSeconds) {
    	this.wait = true;
    	this.semaphoreServer.sendToMe(new IncomingRequest(this, this.signalMap.keySet(),timeSeconds));

    	while(this.wait) {} // continua fino a quando non riceve un IncomingResponse per la IncomingRequest inviata sopra

        int max = 0;
        Id<Lane> link = null;
        Id<Lane> foundAgentLink = null;
        boolean foundAgent = false;
        String bids = " and all bids are: ";
        int starvationBonus = 0;
        boolean debug_stop = false;
        for (Map.Entry<Id<Lane>, Integer> e : this.bidMap.entrySet()){
            if (e.getKey() != null && this.agentMap.get(e.getKey()) != null) {
                boolean curFoundAgent = this.agentMap.get(e.getKey()).stream().filter(bid -> bid.getAgent() != null && bid.getAgent().getPerson().getId()
                        .toString().equals("3640")).collect(Collectors.toList()).size() > 0;
                if (curFoundAgent) {
                    foundAgent = true;
                    foundAgentLink = e.getKey();
                    this.has3640 = true;
                    this.logActive = true;
                    log.error("getNextGreen: found bid by 3640 on link " + e.getKey() + ": " + e.getValue());
                }
                if (!debug_stop)
                    debug_stop = e.getKey().toString().equals("225");
            }
            if (e.getKey() != null
                    && this.agentMap.get(e.getKey()) != null
                    && timeSeconds > this.lastGreen.getOrDefault(e.getKey(), 0.0) + 600
                    && this.agentMap.get(e.getKey()).size() > 0) {
                starvationBonus = 30000;
                if (this.isSignalSystem)
                    log.error("getNextGreen: System is applying starvation bonus");
            }
            if (e.getValue() > 0) {
                if (e.getValue() + this.incomingBid.getOrDefault(e.getKey(), 0) + starvationBonus > max) {
                    bids += " " + e.getValue();
                    max = e.getValue() + this.incomingBid.getOrDefault(e.getKey(), 0) + starvationBonus;
                    link = e.getKey();
                }
            } else {
                bids += " " + e.getValue();
            }
            starvationBonus = 0;
        }
        if (link != null) {
            if (foundAgent) {
                log.error("getNextGreen: green link is " + link.toString() + " with bid " + max);
                log.error("getNextGreen: 3640 is on " + (foundAgentLink != null ? foundAgentLink.toString() : null)
                        + " " + bids);
                if (!link.toString().equals(foundAgentLink.toString())) {
                    String sgGroupIds = this.signalMap.get(link).getSignals().values().stream()
                            .map(Signal::getLinkId).map(Id::toString).map(s -> s + " ").reduce(String::concat)
                            .orElse("Empty SignalGroup").trim();
                    log.error("Signal group containing green link but not agent's link, all IDs: " + sgGroupIds);
                }
            }
            this.lastGreen.put(link, timeSeconds);
            debug_stop = false;
            return new NextGreen(this.signalMap.get(link).getId(), link);
        } else {
            if (foundAgent) {
                log.error("getNextGreen: green link is null with bid ");
                log.error("getNextGreen: 3640 is on " + (foundAgentLink != null ? foundAgentLink.toString() : null)
                        + " " + bids);
            }
            return new NextGreen(getRandomSignal(), null);
        }
    }

    private Id<SignalGroup> getRandomSignal() {
        List<Id<SignalGroup>> signals = this.signalMap.values().stream()
                .map(signal -> signal.getId())
                .filter(signalid -> signalid != this.actualGreen)
                .collect(Collectors.toList());
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
        this.isSignalSystem = false;
        this.has3640 = false;
        this.logActive = false;
        this.bidMap = new HashMap<>();
        this.signalMap = new HashMap<>();
        this.agentMap = new ConcurrentHashMap<>();
        this.totalAgents = new ConcurrentHashMap<>();
        this.agentsRouteAndBid = new ConcurrentHashMap<>();
        this.lastCameraUpdate = new HashMap<Id<Lane>, Double>();
        this.incomingBid = new HashMap<>();
        this.lastGreen = new HashMap<>();
        this.camMap = new HashMap<>();
        this.changeFromLastUpdate = new HashMap<>();
        Stream<Tuple<SignalGroup, Signal>> signals = signalSystem.getSignalGroups().values().stream().flatMap(
                o -> o.getSignals().values().stream().map(s -> new Tuple<SignalGroup, Signal>(o, s)));
        signals.forEach(tupla -> {
            Signal s = tupla.getSecond();
            SignalGroup sg = tupla.getFirst();
            Gbl.assertIf(s.getLaneIds().size() == 1);
            Id<Lane> link = s.getLaneIds().iterator().next();
            this.signalMap.put(link, sg);
            this.bidMap.put(link, 0);

            Coord pos = network.getLinks().get(Id.create(link.toString().split("\\.")[0], Link.class)).getCoord();
            commWrapper.addFixedComunicator(this, Collections.singleton(pos));
            ActiveCamera cam = new ActiveCamera(Id.create(link.toString(), Camera.class), link, percWrapper);
            cam.addCameraListener(this);
            this.camMap.put(link, cam);
            this.changeFromLastUpdate.put(link, new ArrayList<>());
            this.actualGreen = sg.getId();
        });

        List<SemaphoreServer> semaphoreServerList = this.discover()
                .stream()
                .filter(s -> s instanceof SemaphoreServer)
                .map(s -> (SemaphoreServer) s)
                .collect(Collectors.toList());

        this.semaphoreServer = semaphoreServerList.get(0);
        this.semaphoreServer.sendToMe(new SSBootUpMessage(this, this.signalMap));

        List<String> linksStringsIds = signalSystem.getSignals().values().stream().map(Signal::getLinkId)
                .map(Id::toString).collect(Collectors.toList());
        if (linksStringsIds.contains("225") && linksStringsIds.contains("436")) {
            this.isSignalSystem = true;
        }
    }

    @Override
    public void sendToMe(ComunicationMessage message) {
        if (message instanceof BidMessage) {
            Id<Lane> link = ((BidMessage) message).getLink();
            int bid = ((BidMessage) message).getBid();
            BidAgent agent = (BidAgent) ((BidMessage) message).getSender();
            if (agent.getPerson().getId().toString().equals("3640"))
                log.error("sendToMe(BidMessage): 3640 bids " + bid + " on link " + link);
            if (link == agent.getDestinationLinkId())
                return;
            double time = ((BidMessage) message).getTime();

            em.processEvent(new BidEvent(link, agent, bid, time));

            if (bid <= 0) {
                if (agent.getPerson().getId().toString().equals("3640"))
                    log.error("sendToMe(BidMessage): bid ignored (<=0)");
                return;
            }

            BidAgent.BidAgentMode mode = ((BidMessage) message).getMode();
            // this.bidMap.put(link, this.bidMap.get(link) + bid);
            addOrRemoveBidToBidMap(true, bid, link);
            addBidToMap(link, agent, bid, mode);
            // per evitare problemi di coerenza dati dal fatto che prima gli agenti mandando
            // le puntate
            // e poi viene notificato il LinkEnter e LinkLeave
            // TODO: chiedere se serve
            // this.updateTotalAgents(link);
            // if (this.lastCameraUpdate.get(link) <= ((BidMessage) message).getTime())
            // this.changeTotalAgent(link,this.totalAgents.getOrDefault(link,0) + 1);
            // Object a = new Object();
            this.changeFromLastUpdate(link, true, ((BidMessage) message).getTime());
        } else if (message instanceof RideMessage) { // ricevuto mess in cui agente comunica il suo percorso
            List<Id<Lane>> agentRoute = ((RideMessage) message).getRoute();
            int index = ((RideMessage) message).getIndex();
            BidAgent agent = (BidAgent) ((RideMessage) message).getSender();
            if (index <= agentRoute.size() && index > 0) {
                if (agent.getPerson().getId().toString().equals("3640"))
                    log.error("sendToMe(RideMessage): 3640 (on link " + agentRoute.get(index - 1)
                            + ") communicates path: " +
                            agentRoute.stream().map(Id::toString).map(s -> s + " ").reduce(String::concat)
                                    .orElse("empty path").trim());
                Id<Lane> actualLink = agentRoute.get(index - 1);
                List<Tuple<List<Id<Lane>>, Integer>> actual = this.agentsRouteAndBid.get(actualLink);
                if (actual == null) {
                    actual = new Vector<Tuple<List<Id<Lane>>, Integer>>();
                    this.agentsRouteAndBid.put(actualLink, actual);
                }
                actual.add(new Tuple<List<Id<Lane>>, Integer>(agentRoute.subList(index, agentRoute.size()),
                        agent.getBid()));
            }

        } else if (message instanceof IncomingResponse) { // server comunica quali agenti sono in arrivo
            this.incomingBid = ((IncomingResponse) message).getIncomingAgent(); // incomingAgent: map of the total bids
                                                                                // for every link
            this.wait = false; // sblocca while in getNextGreen (riga 187 circa)
        } else if (message instanceof CrossedMessage) { // agente ha attraversato l'incrocio di questo semaforo
            BidAgent senderAgent = (BidAgent) message.getSender();
            Id<Lane> link = ((CrossedMessage) message).getLink();
            boolean isFollowedAgent = senderAgent.getPerson().getId().toString().equals("3640");
            if (isFollowedAgent) {
                log.error("sendToMe(CrossedMessage): 3640 crossed intersection from " + link);
                this.has3640 = false;
                this.logActive = true;
            }
            double time = ((CrossedMessage) message).getTime();
            synchronized (this.agentMap.get(link)) {
                addOrRemoveBidToBidMap(false, ((CrossedMessage) message).getBid(), link);
                if (this.agentMap != null && this.agentMap.get(link) != null && this.agentMap.get(link).size() > 0) {
                    List<Tuple<List<Id<Lane>>, Integer>> flow = new ArrayList<Tuple<List<Id<Lane>>, Integer>>();
                    List<Tuple<List<Id<Lane>>, Integer>> actualRouteAndBid = this.agentsRouteAndBid.get(link);
                    if (this.agentMap.get(link).get(0).getAgent() == null) {
                        /*
                         * A causa della concorrenza può succedere che un agente statico abbia già
                         * attraversato
                         * il semaforo ma il thread che gestisce l'agente dinamico dietro a lui invece
                         * acquisisce
                         * prima del thread che gestisce l'agente statico il lock sulla lista e quindi
                         * andrebbe
                         * a rimuovere l'agente sbagliato.
                         * Questa soluzione è semplice ma funzionante
                         */
                        for (int i = 0; i < this.agentMap.get(link).size(); i++) {
                            if (this.agentMap.get(link).get(i).getAgent() != null) {
                                this.agentMap.get(link).remove(i);
                                flow.add(actualRouteAndBid.remove(i));
                                break;
                            }
                        }
                    } else {
                        Bid agent = this.agentMap.get(link).remove(0);
                        agent.getAgent().sendToMe(new DecriseBudget(this, agent.getBid()));
                        flow.add(actualRouteAndBid.remove(0));
                    }
                    // boolean before = this.has3640;
                    // this.has3640 = this.isSignalSystem &&
                    //     !(this.agentMap.get(link).stream()
                    //         .filter(bid -> bid.getAgent() != null
                    //                 && bid.getAgent().getPerson().getId().toString().equals("3640"))
                    //         .collect(Collectors.toList()).size() > 0);
                    // if (before != this.has3640) {
                    //     log.error("has3640 changed from " + before + " to " + this.has3640);
                    // }
                    // this.logActive = true;
                    this.semaphoreServer.sendToMe(new SemaphoreFlowMessage(this, flow, link, time, isFollowedAgent));
                }
                // per evitare problemi di coerenza dati dal fatto che prima gli agenti mandando
                // le puntate
                // e poi viene notificato il LinkEnter e LinkLeave
                // this.updateTotalAgents(link);
                // if (this.lastCameraUpdate.get(link) < ((CrossedMessage) message).getTime())
                // this.changeTotalAgent(link,this.totalAgents.getOrDefault(link,1) - 1);
                this.changeFromLastUpdate(link, false, ((CrossedMessage) message).getTime());
            }
        }
    }

    /**
     * Add bid to the list of bids made by agents for the given link
     * 
     * @param link  link where the agent is located and bids
     * @param agent agent making the bid
     * @param bid   amount of the bid
     */
    private void addBidToMap(Id<Lane> link, BidAgent agent, int bid, BidAgent.BidAgentMode mode) {
        List<Bid> bidForLink = this.agentMap.get(link);
        if (bidForLink == null) {
            bidForLink = new Vector<Bid>();
        }

        bidForLink.add(new Bid(agent, bid, mode));
        this.agentMap.put(link, bidForLink);
    }

    public boolean controlLink(Id<Lane> link) {
        return this.signalMap.containsKey(link);
    }

    @Override
    public void pushCameraStatus(CameraStatus status, double time) {
        Tuple<Double, Integer> snap = status.getLinkStatus().getTotalSnap();
        int newTotal = snap.getSecond();
        double lu = snap.getFirst();
        Id<Lane> link = status.getIdLink();
        int oldTotal = this.totalAgents.getOrDefault(link, 0);
        if (this.agentMap.get(link) != null) {
            synchronized (this.agentMap.get(link)) {
                if (oldTotal > newTotal && this.agentMap.get(link).size() > 0) {
                    /*
                     * Nel sendtome al CrossedMessage ho spiegato perché non posso
                     * eliminare in testa come sarebbe logico.
                     */
                    for (int i = 0; i < this.agentMap.get(link).size() && i < oldTotal - newTotal; i++) {
                        if (this.agentMap.get(link).get(i).getAgent() == null) {
                            Bid NEAgent = this.agentMap.get(link).remove(i);
                            this.agentsRouteAndBid.get(link).remove(i);
                            addOrRemoveBidToBidMap(false, NEAgent.getBid(), link);
                            // break;
                        }
                    }
                }
            }
        }
        this.totalAgents.put(link, newTotal);
        this.lastCameraUpdate.put(link, lu);
    }

    /*
     * Ho dovuto aggiungere questo metodo perché accessi concorrenti alla mappa
     * provocavano inconsistenze
     */
    /**
     * Add bid to the total bid for the link
     * 
     * @param add  sum (true) or subtract (false) bid from the total
     * @param bid  amount to change
     * @param link link for which the bid is being modified
     */
    public synchronized void addOrRemoveBidToBidMap(Boolean add, int bid, Id<Lane> link) {
        if (add)
            this.bidMap.put(link, this.bidMap.get(link) + bid);
        else
            this.bidMap.put(link, this.bidMap.get(link) - bid);
    }

    @Override
    public Set<ComunicationServer> discover() {
        return signalMap.entrySet().stream()
                .filter(e -> e.getValue().getId() == this.actualGreen)
                .map(Map.Entry::getKey)
                .map(lane -> this.commWrapper.discover(lane))
                .reduce(new HashSet<>(), (acc, newSet) -> {acc.addAll(newSet); return acc; });
    }

    public HashMap<Id<Lane>, SignalGroup> getSignalMap() {
        return this.signalMap;
    }

    private static class Bid {

        private BidAgent agent;
        private int bid;
        private BidAgent.BidAgentMode agentMode;

        Bid(BidAgent agent, int bid, BidAgent.BidAgentMode mode) {
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
        private Id<Lane> link;

        NextGreen(Id<SignalGroup> group, Id<Lane> link) {
            this.group = group;
            this.link = link;
        }

        public Id<SignalGroup> getGroup() {
            return group;
        }

        public void setGroup(Id<SignalGroup> group) {
            this.group = group;
        }

        public Id<Lane> getLink() {
            return link;
        }

        public void setLink(Id<Lane> link) {
            this.link = link;
        }
    }

}
