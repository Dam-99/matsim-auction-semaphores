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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BidsSemaphoreControllerCommunication extends BidsSemaphoreController implements SignalController, ComunicationServer, CameraListener, ComunicationClient {

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
    private ConcurrentHashMap<Id<Link>, List<Bid>> agentMap;
    private ConcurrentHashMap<Id<Link>, Integer> totalAgents;
    private ConcurrentHashMap<Id<Link>, List<Tuple<List<Id<Link>>,Integer>>> agentsRouteAndBid;
    private Id<SignalGroup> actualGreen;
    private double lastChange;
    private float m = 0;
    private float M = 1.1f;
    private Boolean wait = false;
    private HashMap<Id<Link>,Integer> incomingBid;
    private SemaphoreServer semaphoreServer;
    private HashMap<Id<Link>,Double> lastGreen;
    private HashMap<Id<Link>, Camera> camMap;
    private HashMap<Id<Link>, Double> lastCameraUpdate;
    private HashMap<Id<Link>, List<Tuple<Double, Integer>>> changeFromLastUpdate;
    private Lock changeFromLastUpdateLock = new ReentrantLock();

    @Override
    public void updateState(double timeSeconds) {
   
        if (timeSeconds - this.lastChange < this.minimunGreenTime)
            return;
        
        setBidForStaticAgents(timeSeconds);
        
        NextGreen nextGreen = getNextGreen(timeSeconds);
        Id<SignalGroup> nextGreenGroup = nextGreen.getGroup();
        Id<Link> nextGreenLink = nextGreen.getLink();
        
        this.system.scheduleOnset(timeSeconds, nextGreenGroup);
        if (this.actualGreen != null && this.actualGreen != nextGreenGroup)
            this.system.scheduleDropping(timeSeconds, this.actualGreen);

        this.actualGreen = nextGreenGroup;
        this.lastChange = timeSeconds;

        //azzero quello che ha vinto
       /* bidMap.put(nextGreenLink, 0);
        if (nextGreenLink != null && this.agentsRouteAndBid.get(nextGreenLink) != null)
			this.agentsRouteAndBid.put(nextGreenLink, new ArrayList<Tuple<List<Id<Link>>,Integer>>());

        //comunico agli agenti che anno vinto di scalare il budget
        if (nextGreenLink != null && this.agentMap.containsKey(nextGreenLink)) {
            this.agentMap.get(nextGreenLink).forEach(b -> b.getAgent().sendToMe(new DecriseBudget(this, b.getBid())));
            //azzero gli agenti che hanno vinto e quindi sono passati
            this.agentMap.put(nextGreenLink, new ArrayList<>());
        }*/
    }

    private void setBidForStaticAgents(double time) {
        for (Map.Entry<Id<Link>, Integer> e : this.bidMap.entrySet()){
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
                addBidToMap(e.getKey(), null, bid, null);
                addOrRemoveBidToBidMap(true, bid, e.getKey());
                if (this.agentsRouteAndBid.get(e.getKey()) == null)
                	this.agentsRouteAndBid.put(e.getKey(),  new Vector<Tuple<List<Id<Link>>,Integer>>() );
                //placeholder per mantenere la coerenza tra le due mappe
                this.agentsRouteAndBid.get(e.getKey()).add(new Tuple<List<Id<Link>>,Integer>(new ArrayList(),bid));
            }
        }
    }

    private int getChangedFromLastUpdate(Id<Link> link, double actualTime) {
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

    private void updateTotalAgents(Id<Link> link) {
        LinkTrafficStatus linkStatus = this.camMap.get(link).getCameraStatus().getLinkStatus();
        Tuple<Double, Integer> snap = linkStatus.getTotalSnap();
        int total = snap.getSecond();
        double lastUpdate = snap.getFirst();
        this.changeTotalAgent(link, total, lastUpdate);
    }

    private synchronized void changeTotalAgent(Id<Link> link, int total, double lastUpdate) {
        this.totalAgents.put(link, total);
        this.lastCameraUpdate.put(link, lastUpdate);
    }

    private synchronized void changeFromLastUpdate(Id<Link> link, boolean inc, double time) {
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
    	
    	while(this.wait) {}
        
        int max = 0;
        Id<Link> link = null;
        int starvationBonus = 0;
        for (Map.Entry<Id<Link>, Integer> e : this.bidMap.entrySet()){    
        	if (e.getKey() != null 
        			&& this.agentMap.get(e.getKey()) != null 
        			&& timeSeconds > this.lastGreen.getOrDefault(e.getKey(),0.0) + 600 
        			&& this.agentMap.get(e.getKey()).size() > 0)
        		starvationBonus = 30000;        	
        	if(e.getValue() > 0) {
	            if (e.getValue() + this.incomingBid.getOrDefault(e.getKey(),0) + starvationBonus > max){
	                max = e.getValue() + this.incomingBid.getOrDefault(e.getKey(),0) + starvationBonus;
	                link = e.getKey();
	            }
        	}
            starvationBonus = 0;
        }
        
        if (link != null){
        	this.lastGreen.put(link, timeSeconds);
            return new NextGreen(this.signalMap.get(link).getId(), link);
        } else {
            return new NextGreen(getRandomSignal(), null);
        }
    }

    private Id<SignalGroup> getRandomSignal() {
        List<Id<SignalGroup>> signals  = this.signalMap.values().stream()
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
        this.bidMap = new HashMap<>();
        this.signalMap = new HashMap<>();
        this.agentMap = new ConcurrentHashMap<>();
        this.totalAgents = new ConcurrentHashMap<>();
        this.agentsRouteAndBid = new ConcurrentHashMap<>();
        this.lastCameraUpdate = new HashMap<Id<Link>, Double>();
        this.incomingBid = new HashMap<>();
        this.lastGreen = new HashMap<>();
        this.camMap = new HashMap<>();
        this.changeFromLastUpdate = new HashMap<>();
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
		this.semaphoreServer.sendToMe(new SSBootUpMessage(this,this.signalMap));
        
    }

    @Override
    public void sendToMe(ComunicationMessage message) {
        if (message instanceof BidMessage) {
            Id<Link> link = ((BidMessage) message).getLink();
            int bid = ((BidMessage) message).getBid();
            BidAgent agent = (BidAgent) ((BidMessage) message).getSender();
            if (link == agent.getDestinationLinkId())
            	return;
            double time = ((BidMessage) message).getTime();
            
            em.processEvent(new BidEvent(link, agent, bid, time));

            if (bid <= 0) {
                return;
            }

            BidAgent.BidAgentMode mode = ((BidMessage) message).getMode();
            //this.bidMap.put(link, this.bidMap.get(link) + bid);
            addOrRemoveBidToBidMap(true,bid,link);
            addBidToMap(link, agent, bid, mode); 
            //per evitare problemi di coerenza dati dal fatto che prima gli agenti mandando le puntate 
        	//e poi viene notificato il LinkEnter e LinkLeave
//            this.updateTotalAgents(link);
//            if (this.lastCameraUpdate.get(link) <= ((BidMessage) message).getTime())
//                this.changeTotalAgent(link,this.totalAgents.getOrDefault(link,0) + 1);
//            Object a = new Object();
            this.changeFromLastUpdate(link, true, ((BidMessage) message).getTime());
        } else if (message instanceof RideMessage) {
        	List<Id<Link>> agentRoute = ((RideMessage) message).getRoute();
        	int index = ((RideMessage) message).getIndex();
        	BidAgent agent = (BidAgent) ((RideMessage) message).getSender();
        	if (index <= agentRoute.size() && index > 0 ) {
	        	Id<Link> actualLink = agentRoute.get(index-1);
	        	List<Tuple<List<Id<Link>>,Integer>> actual = this.agentsRouteAndBid.get(actualLink);
	        	if (actual == null) {
	        		actual = new Vector<Tuple<List<Id<Link>>,Integer>>();
	        		this.agentsRouteAndBid.put(actualLink, actual);
	        	}
	        	actual.add(new Tuple<List<Id<Link>>,Integer>(agentRoute.subList(index, agentRoute.size()),agent.getBid()));
        	}
        	
        } else if (message instanceof IncomingResponse) {
        	this.incomingBid = ((IncomingResponse)message).getIncomingAgent();
        	this.wait = false;
        } else if (message instanceof CrossedMessage) {
        	Id<Link> link = ((CrossedMessage)message).getLink();
        	double time = ((CrossedMessage)message).getTime();
        	synchronized(this.agentMap.get(link)) {
	        	addOrRemoveBidToBidMap(false,((CrossedMessage)message).getBid(),link);
	        	if(this.agentMap != null && this.agentMap.get(link) != null && this.agentMap.get(link).size() > 0) {
	        		List<Tuple<List<Id<Link>>,Integer>> flow = new ArrayList<Tuple<List<Id<Link>>,Integer>>();
	        		List<Tuple<List<Id<Link>>,Integer>> actualRouteAndBid = this.agentsRouteAndBid.get(link);
	        		if (this.agentMap.get(link).get(0).getAgent() == null) {
	        			/*
	        			 * A causa della concorrenza può succedere che un agente statico abbia già attraversato 
	        			 * il semaforo ma il thread che gestisce l'agente dinamico dietro a lui invece acquisisce
	        			 * prima del thread che gestisce l'agente statico il lock sulla lista e quindi andrebbe 
	        			 * a rimuovere l'agente sbagliato.
	        			 * Questa soluzione è semplice ma funzionante
	        			 */
	        			for (int i = 0; i < this.agentMap.get(link).size(); i++) {
	        				if(this.agentMap.get(link).get(i).getAgent() != null) {
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
	        		this.semaphoreServer.sendToMe(new SemaphoreFlowMessage(this, flow, link, time));
	        	} 
	        	//per evitare problemi di coerenza dati dal fatto che prima gli agenti mandando le puntate 
	        	//e poi viene notificato il LinkEnter e LinkLeave
//                this.updateTotalAgents(link);
//                if (this.lastCameraUpdate.get(link) < ((CrossedMessage) message).getTime())
//	        	    this.changeTotalAgent(link,this.totalAgents.getOrDefault(link,1) - 1);
                this.changeFromLastUpdate(link, false, ((CrossedMessage) message).getTime());
        	}
        }
    }

    private void addBidToMap(Id<Link> link, BidAgent agent, int bid, BidAgent.BidAgentMode mode) {
        List<Bid> bidForLink = this.agentMap.get(link);
        if (bidForLink == null) {
            bidForLink = new Vector<Bid>();
        }

        bidForLink.add(new Bid(agent, bid, mode));
        this.agentMap.put(link, bidForLink);
    }

    public boolean controlLink(Id<Link> link) {
        return this.signalMap.containsKey(link);
    }

    @Override
    public void pushCameraStatus(CameraStatus status, double time) {
        Tuple<Double, Integer> snap = status.getLinkStatus().getTotalSnap();
    	int newTotal = snap.getSecond();
        double lu = snap.getFirst();
    	Id<Link> link = status.getIdLink();
    	int oldTotal = this.totalAgents.getOrDefault(link,0);
    	if (this.agentMap.get(link) != null) {
	    	synchronized(this.agentMap.get(link)) {
	    		if(oldTotal > newTotal && this.agentMap.get(link).size() > 0) {
	    			/*
	    			 * Nel sendtome al CrossedMessage ho spiegato perchè non posso
	    			 * eliminare in testa come sarebbe logico.
	    			 */
	    			for (int i = 0; i < this.agentMap.get(link).size() && i < oldTotal - newTotal; i++) {
	    				if(this.agentMap.get(link).get(i).getAgent() == null) {
	    					Bid NEAgent = this.agentMap.get(link).remove(i);
				    		this.agentsRouteAndBid.get(link).remove(i);
				    		addOrRemoveBidToBidMap(false, NEAgent.getBid(), link);
				    		//break;
	    				}
	    			}
		    	}
	    	}
    	}
        this.totalAgents.put(link,newTotal);
        this.lastCameraUpdate.put(link, lu);
    }
    
    /*
     * Ho dovuto aggiungere questo metodo perchè accessi concorrenti alla mappa provocavano inconsistenze
     */
    public synchronized void addOrRemoveBidToBidMap(Boolean add, int bid,Id<Link> link) {
    	if (add)
    		this.bidMap.put(link, this.bidMap.get(link) + bid);
    	else
    		this.bidMap.put(link, this.bidMap.get(link) - bid);
    }
    
    @Override
	public Set<ComunicationServer> discover() {
		return this.commWrapper.discover(this.network.getLinks().get(this.actualGreen).getId());
	}
    
    public HashMap<Id<Link>, SignalGroup> getSignalMap(){
    	return this.signalMap;
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
