package org.matsim.contrib.smartcity.comunication;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.smartcity.actuation.semaphore.BidsSemaphoreController;
import org.matsim.contrib.smartcity.actuation.semaphore.BidsSemaphoreControllerCommunication;
import org.matsim.contrib.smartcity.analisys.SemaphorePredictionEvent;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SemaphoreServerProportional extends SemaphoreServer implements ComunicationServer {
	private static final Logger log = Logger.getLogger(SemaphoreServerProportional.class);

	private ComunicationFixedWrapper wrapper;
	private ConcurrentHashMap<Id<SignalGroup>, PriorityQueue<IncomingAgent>> incomingBid;
	private HashMap<Id<Link>,SignalGroup> signalMap;
	@Inject
    private Network network;
	@Inject
	private EventsManager em;
	private HashMap<Id<Link>,SignalGroupCounter> SGCounter;
	private Random generator;

	//TODO rendere configurabile
	private int maxPropagation = 1;

	public SemaphoreServerProportional(ComunicationFixedWrapper wrapper, ComunicationServerFactory.ServerData data) {
		super(wrapper, data);
		Set<Coord> positions = data.coord;
		this.wrapper = wrapper;
		this.wrapper.addFixedComunicator(this, positions);
		this.signalMap = new HashMap<Id<Link>,SignalGroup>();
		this.incomingBid = new ConcurrentHashMap<Id<SignalGroup>, PriorityQueue<IncomingAgent>>();
		this.SGCounter = new HashMap<Id<Link>, SignalGroupCounter>();
		this.generator = new Random();
	}
	
	@Override
	public void sendToMe(ComunicationMessage message) {
		if (message instanceof SemaphoreFlowMessage) {
			double time = ((SemaphoreFlowMessage) message).getTime();
			List<Tuple<List<Id<Link>>,Integer>> agentsRouteAndBid = ((SemaphoreFlowMessage) message).getAgentsRouteAndBid();
			if (!((SemaphoreFlowMessage) message).isEquipped()) {
				int totalBidOfNEAgent = ((SemaphoreFlowMessage) message).getTotalBidOfNotEquippedAgent();
				Id<Link> actualLink = ((SemaphoreFlowMessage) message).getActualLink();
				SignalGroupCounter sgc = this.SGCounter.get(actualLink);
				if (sgc != null) {
					this.generator.nextInt(100);
					Id<SignalGroup> nextSignal = sgc.getPropNextSignal();
					ArrayList<Id<SignalGroup>> nextSignalArray = new ArrayList<Id<SignalGroup>>();
					nextSignalArray.add(nextSignal);
					this.incomingBid.get(nextSignal).add(new IncomingAgent(totalBidOfNEAgent, 0));
					SemaphorePredictionEvent ev = new SemaphorePredictionEvent(actualLink, nextSignalArray, time);
					em.processEvent(ev);
				}
				return;
			}
			boolean isFollowedAgent = ((SemaphoreFlowMessage) message).is3640Message;
			if(isFollowedAgent)
				log.error("sendToMe(SemaphoreFlow): " + ((SemaphoreFlowMessage) message).getActualLink() + " set flow of 3640");

			// compute propagation on the following signalsystems
			Id<Link> actualLink =  ((SemaphoreFlowMessage) message).getActualLink();
			if (agentsRouteAndBid != null) {
				for(Tuple<List<Id<Link>>,Integer> routeAndBid : agentsRouteAndBid) {
					double estimatedTripTime = 0;
					int propagationAdded = 0;
					if(isFollowedAgent)
						log.error("sendToMe(SemaphoreFlow): route is " + routeAndBid.getFirst().stream().map(Object::toString).collect(Collectors.joining(",")));
					for(Id<Link> link : routeAndBid.getFirst()) {
						SignalGroup sg = signalMap.get(link);
						double length = this.network.getLinks().get(link).getLength();
						double speed = this.network.getLinks().get(link).getFreespeed();
						double estimatedCrossingTime = length/speed ;
						estimatedTripTime += estimatedCrossingTime;
						if (sg != null && estimatedTripTime > 20) { // crea e aggiungilo se non esisteva
							PriorityQueue<IncomingAgent> actual = this.incomingBid.get(sg.getId());
							if (actual == null) {
								actual = new PriorityQueue<IncomingAgent>();
								this.incomingBid.put(sg.getId(),actual);
							}
							actual.add(new IncomingAgent((int) Math.round(routeAndBid.getSecond()*Math.pow(0.5,(propagationAdded-1))),estimatedTripTime + time));
							propagationAdded++;
							if (this.SGCounter.get(actualLink) == null)
								this.SGCounter.put(actualLink,new SignalGroupCounter());
							this.SGCounter.get(actualLink).add(sg.getId());
							if (propagationAdded >= maxPropagation)
								break;
						}
					}
				}
			}
		} else if (message instanceof SSBootUpMessage) {
			HashMap<Id<Link>, SignalGroup> localSignalMap = ((SSBootUpMessage)message).getSignalMap();
			localSignalMap.forEach((k, v) -> this.signalMap.put(k, v));
			
			
		} else if (message instanceof IncomingRequest) { // la propagazione è calcolata in sendToMe(SemaphoreFlow) e inserita in incomingBid[sg.Id]
			BidsSemaphoreController sender = (BidsSemaphoreController)((IncomingRequest) message).getSender();
			Set<Id<Link>> links = ((IncomingRequest) message).getLinks();
			HashMap<Id<Link>,Integer> incomingAgent = new HashMap<Id<Link>,Integer>();
			double time = ((IncomingRequest) message).getTime();
			for (Id<Link> link : links) {
				SignalGroup sg = this.signalMap.get(link);
				PriorityQueue<IncomingAgent> queue = this.incomingBid.get(sg.getId());
				int sum = 0;
				if (queue != null) {
					while(queue.peek() != null) {
						IncomingAgent agent = queue.peek();
						if (agent.getEstimatedArrivalTime() - time <= 40 || agent.getEstimatedArrivalTime() == 0) {
							if (agent.is3640agent) {
								log.error("sendToMe(IncomingRequest): sender is link among " + ((BidsSemaphoreControllerCommunication) sender).getSignalMap().keySet());
								log.error("sendToMe(IncomingRequest): propagated bid added to the IncomingResponse for link " + link + " (" + queue.peek().getBid() + ")");
							}
							sum += queue.poll().getBid();
						}
						else
							break;
					}
				}
				incomingAgent.put(link, sum);
			}
			sender.sendToMe(new IncomingResponse(this, incomingAgent));
		}
	}
	
	private static class IncomingAgent implements Comparable<IncomingAgent> {
		private Integer bid;
		private double estimatedArrivalTime;
		public boolean is3640agent;

		public IncomingAgent(Integer bid,double estimatedArrivalTime) {
			this.setBid(bid);
			this.setEstimatedArrivalTime(estimatedArrivalTime);
			this.is3640agent = false;
		}
		public IncomingAgent(Integer bid,double estimatedArrivalTime, boolean is3640agent) {
			this(bid, estimatedArrivalTime);
			this.is3640agent = is3640agent;
		}

		public Integer getBid() {
			return bid;
		}

		public void setBid(Integer bid) {
			this.bid = bid;
		}

		public double getEstimatedArrivalTime() {
			return estimatedArrivalTime;
		}

		public void setEstimatedArrivalTime(double estimatedArrivalTime) {
			this.estimatedArrivalTime = estimatedArrivalTime;
		}

		@Override
		public int compareTo(IncomingAgent arg0) {
			return Double.compare(this.estimatedArrivalTime,arg0.getEstimatedArrivalTime());
		}
	 }   

	private class SignalGroupCounter {
		private List<Id<SignalGroup>> signalGroups;
		private List<Integer> counter;
		private int maxSignalGroup;
		private int maxCounter;
		private Integer totalSum = 0;
		
		public SignalGroupCounter() {
			this.signalGroups = new ArrayList<Id<SignalGroup>>();
			this.counter = new ArrayList<Integer>();
		}
		
		public void add(Id<SignalGroup> sg) {
			if (!this.signalGroups.contains(sg)) {
				this.signalGroups.add(sg);
				this.counter.add(0);
			}
			this.increment(sg);
		}
		
		synchronized private void increment(Id<SignalGroup> sg) {
			int index = this.signalGroups.indexOf(sg);
			this.counter.set(index, this.counter.get(index) + 1);
			this.totalSum += 1;
			if (this.counter.get(index) > this.maxCounter) {
				this.maxCounter = this.counter.get(index);
				this.maxSignalGroup = index;
			}
		}
		
		public Id<SignalGroup> getMax() {
			return this.signalGroups.get(maxSignalGroup);
		}

		public Id<SignalGroup> getPropNextSignal() {
			int index = new Random().nextInt(totalSum);
			int sum = 0;
			int i=0;
			while(sum < index ) {
				sum = sum + counter.get(i++);
			}
			return signalGroups.get(Math.max(0,i-1));
		}


	}
}
