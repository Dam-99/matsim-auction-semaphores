/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.smartcity.agent.routing.KShortestPath;
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.ComunicationMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.contrib.smartcity.comunication.FlowRequest;
import org.matsim.contrib.smartcity.comunication.TrafficFlowMessage;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationWrapper;
import org.matsim.contrib.smartcity.perception.TrafficFlow;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class CLASSDriverLogicKShortest extends AbstractDriverLogic implements AutonomousSpeed, ComunicationClient {

	private static final int ALTERNATIVE = 5;
	private TrafficFlow flow;
	private KShortestPath router;
	private Id<Link> nextLink;
	private boolean recomputeNextLink = true;
	@Inject private ComunicationWrapper wrapper;
	private Network network;
	@Inject private QSim sim;
	@Inject private Config config;
	@Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	private List<Id<Node>> dest;
	private NetworkInverter inverter;
	private TravelDisutilityWithFlow travel;
	private Vehicle veh;
	
	@Inject
	public CLASSDriverLogicKShortest(Network network, NetworkTurnInfoBuilderI turn) {
		super();
		this.network = network;
		NetworkInverter inverter = NetworkInverterProvider.getInverted(network, turn);
		this.inverter = inverter;
		this.router = new KShortestPath(inverter, network, new TrafficFlow(), ALTERNATIVE);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.AutonomousSpeed#getSpeed()
	 */
	@Override
	public double getSpeed() {
		return network.getLinks().get(actualLink).getFreespeed();
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.AbstractDriverLogic#getNextLinkId()
	 */
	@Override
	public Id<Link> getNextLinkId() {
		if (recomputeNextLink == false) {
			return this.nextLink;
		}
		if (this.dest.contains(this.actualLink)) {
			return null;
		}
		
		double actualTime = sim.getSimTimer().getTimeOfDay();
		HashMap<Id<Link>, Double> links = this.router.getNextLinks(this.actualLink, network, actualTime);
		Double sumOfFlow = links.values().stream().mapToDouble(Double::doubleValue).sum();
		SortedMap<Double, Id<Link>> rouletteProb = new TreeMap<Double, Id<Link>>();
		double cumProb = 0.0;
		for (Entry<Id<Link>, Double> e : links.entrySet()) {
			Id<Link> link = e.getKey();
			double prob = 1 - (e.getValue()/sumOfFlow);
			cumProb += prob;
			rouletteProb.put(cumProb, link);
		}
		
		double r = MatsimRandom.getRandom().nextDouble();
		Double prob;
		Iterator<Double> iter = rouletteProb.keySet().iterator();
		do {
			prob = iter.next();	
		} while (r > prob && iter.hasNext());
		
		this.nextLink = rouletteProb.get(prob);
		this.recomputeNextLink = false;
		return rouletteProb.get(prob);
	}
	
	@Override
	public void setActualLink(Id<Link> linkId) {
		super.setActualLink(linkId);
		this.recomputeNextLink = true;
		
	}
	
	@Override
	public void setLeg(Leg leg) {
		super.setLeg(leg);
		Id<Node> startNode = Id.createNodeId(startLink);
		Id<Node> endNode = Id.createNodeId(endLink);
		Id<Node> oppositeStartNode = getOpposite(startLink);
		Id<Node> oppositeEndNode = getOpposite(endLink);
		double startTime = leg.getDepartureTime();
		String mode = leg.getMode();
		TravelTime time = TravelTimeCalculator.create(inverter.getInvertedNetwork(), ConfigUtils.addOrGetModule(config, TravelTimeCalculatorConfigGroup.class)).getLinkTravelTimes();
		TravelDisutility baseTravel = this.travelDisutilityFactories.get(mode).createTravelDisutility(time);
		this.travel = new TravelDisutilityWithFlow(baseTravel, new TrafficFlow());
		MobsimVehicle mobVeh = this.agent.getVehicle();
		Vehicle veh = null;;
		if (mobVeh != null) {
			veh = mobVeh.getVehicle();
		}
		this.veh = veh;
		//this.setActualLink(startLink);
		this.router.setTravelDisutility(this.travel);
		this.router.setTravelTime(time);
		ArrayList<Id<Node>> startNodes = new ArrayList<Id<Node>>();
		ArrayList<Id<Node>> endNodes = new ArrayList<Id<Node>>();
		startNodes.add(startNode);
		if (oppositeStartNode != null) {
			startNodes.add(oppositeStartNode);
		}
		endNodes.add(endNode);
		if (oppositeEndNode != null) {
			endNodes.add(oppositeEndNode);
		}
		this.dest = endNodes;
		this.router.route(startNodes, endNodes, startTime, this.person, veh);
		//this.agent.notifyArrivalOnLinkByNonNetworkMode(this.getNextLinkId());
		
		Set<ComunicationServer> servers = this.discover();
		if (servers.size() > 0) {
			ComunicationServer server = servers.iterator().next();
			server.sendToMe(new FlowRequest(this));
		}
	}

	/**
	 * @param startLink
	 * @return
	 */
	private Id<Node> getOpposite(Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);
		Link opposite = NetworkUtils.getConnectingLink(link.getToNode(), link.getFromNode());
		if (opposite == null) {
			return null;
		}
		if (link.getAttributes().getAttribute(NetworkUtils.ORIGID).equals(opposite.getAttributes().getAttribute(NetworkUtils.ORIGID))) {
			return Id.createNodeId(opposite.getId());
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationClient#discover()
	 */
	@Override
	public Set<ComunicationServer> discover() {
		if (actualLink != null) {
			return this.wrapper.discover(this.actualLink);
		}
		return this.wrapper.discover(this.startLink);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationClient#sendToMe(org.matsim.contrib.smartcity.comunication.ComunicationMessage)
	 */
	@Override
	public void sendToMe(ComunicationMessage message) {
		if (message instanceof TrafficFlowMessage) {
			this.flow = ((TrafficFlowMessage) message).getFlow();
			this.travel.setFlow(this.flow);
			Id<Node> source = actualLink != null ? Id.createNodeId(actualLink) : Id.createNodeId(startLink);
			double startTime = sim.getSimTimer().getTimeOfDay();
			ArrayList<Id<Node>> startNodes = new ArrayList<Id<Node>>();
			startNodes.add(source);
			this.router.route(startNodes, dest, startTime, this.person, this.veh);
		}
		
	}
	
}
