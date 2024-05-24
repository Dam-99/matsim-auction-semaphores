/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
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
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class CLASSDriverLogicGreedyRoute extends AbstractDriverLogic implements ComunicationClient  {

	private Network network;
	private NetworkInverter inverter;
	private TravelDisutilityWithFlow travel;
	@Inject private Config config;
	@Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject private ComunicationWrapper wrapper;
	@Inject private QSim sim;
	private ArrayList<Id<Node>> dest;
	private LeastCostPathCalculator router;
	private Vehicle veh;
	private boolean recomputeNextLink;
	private Id<Link> nextLink;
	private Network invertedNet;

	@Inject
	public CLASSDriverLogicGreedyRoute(Network network, NetworkTurnInfoBuilderI turn) {
		super();
		this.network = network;
		NetworkInverter inverter = NetworkInverterProvider.getInverted(network, turn);
		this.inverter = inverter;
		this.invertedNet = inverter.getInvertedNetwork();
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationEntity#sendToMe(org.matsim.contrib.smartcity.comunication.ComunicationMessage)
	 */
	@Override
	public void sendToMe(ComunicationMessage message) {
		if (message instanceof TrafficFlowMessage) {
			TrafficFlow flow = ((TrafficFlowMessage) message).getFlow();
			this.travel.setFlow(flow);
		}		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationClient#discover()
	 */
	@Override
	public Set<ComunicationServer> discover() {
		return this.wrapper.discover(actualLink);
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
		
		HashMap<Id<Link>, Double> links = new HashMap<Id<Link>, Double>();
		if (actualLink == null) {
			Id<Node> startNode = Id.createNodeId(startLink);
			Id<Node> oppositeStartNode = getOpposite(startLink);
			Path path1 = computePath(startNode);
			Path path2 = computePath(oppositeStartNode);
			if (path1 != null) {
				Link link1 = this.inverter.convertInvertedNodesToLinks(path1.nodes).get(0);
				links.put(link1.getId(), path1.travelCost);
			}
			if (path2 != null) {
				Link link2 = this.inverter.convertInvertedNodesToLinks(path2.nodes).get(0);
				links.put(link2.getId(), path2.travelCost);
			}
			
		} else {
			Link actual = this.network.getLinks().get(actualLink);
			List<Link> possibleNext = possibleNextLinks(actual);
			for (Link next : possibleNext) {
				Id<Node> startNode = Id.createNodeId(next.getId());
				Path path1 = computePath(startNode);
				if (path1 != null) {
					List<Link> route = this.inverter.convertInvertedNodesToLinks(path1.nodes);
					if (possibleNext.size() == 1 || cicleControl(route, actual)) {
						Link link1 = route.get(0);
						links.put(link1.getId(), path1.travelCost);
					}
					
				}
			}
			
		}
		
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
		return this.nextLink;
	}
	
	/**
	 * @param route
	 * @return
	 */
	private boolean cicleControl(List<Link> route, Link link) {
		if (route.contains(link)) {
			return false;
		}
		
		Node node = link.getToNode();
		for (int i=0; i<route.size(); i++) {
			Link l = route.get(i);
			if (l.getToNode().getId().equals(node.getId())) {
				if (i+1 != route.size() && !route.get(i+1).getToNode().getId().equals(link.getFromNode().getId())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param actual
	 * @return
	 */
	private List<Link> possibleNextLinks(Link actual) {
		Id<Node> actualInvId = Id.createNodeId(actual.getId());
		Node actualInv = this.invertedNet.getNodes().get(actualInvId);
		List<Node> nextInv = actualInv.getOutLinks().values().stream().map(l -> l.getToNode()).collect(Collectors.toList());
		return this.inverter.convertInvertedNodesToLinks(nextInv);
//		String strRes = (String) actual.getAttributes().getAttribute(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_ATT);
//		if (strRes != null) {
//			List<String> res = Arrays.asList(strRes.split(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_SEP));
//			return actual.getToNode().getOutLinks().values().stream().filter(l -> !res.contains(l.getId().toString())).collect(Collectors.toSet());
//		} else {
//			return new HashSet<Link>(actual.getToNode().getOutLinks().values());
//		}
	}

	/**
	 * @param startNode
	 * @param oppositeStartNode
	 * @return
	 */
	private Path computePath(Id<Node> startNode) {
		Node fromNode = this.invertedNet.getNodes().get(startNode);
		if (fromNode == null) {
			return null;
		}
		double startTime = sim.getSimTimer().getTimeOfDay();
		TreeSet<Path> paths = new TreeSet<Path>((p1, p2) -> Double.compare(p1.travelCost, p2.travelCost));
		for (Id<Node> toNodeId : this.dest) {
			Node toNode = this.invertedNet.getNodes().get(toNodeId);
			Path p = this.router.calcLeastCostPath(fromNode, toNode, startTime, person, veh);
			if (p != null) {
				paths.add(p);
			}
		}
		
		if (paths.size() > 0) {
			return paths.first();
		} else {
			return null;
		}
		
	}

	@Override
	public void setLeg(Leg leg) {
		super.setLeg(leg);
		this.recomputeNextLink = true;
		Id<Node> endNode = Id.createNodeId(endLink);
		Id<Node> oppositeEndNode = getOpposite(endLink);
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
		ArrayList<Id<Node>> endNodes = new ArrayList<Id<Node>>();
		endNodes.add(endNode);
		if (oppositeEndNode != null) {
			endNodes.add(oppositeEndNode);
		}
		this.dest = endNodes;
		
		this.router = new DijkstraFactory().createPathCalculator(invertedNet, travel, time);
		
		Set<ComunicationServer> servers = this.discover();
		if (servers.size() > 0) {
			ComunicationServer server = servers.iterator().next();
			server.sendToMe(new FlowRequest(this));
		}
	}
	
	@Override
	public void setActualLink(Id<Link> linkId) {
		super.setActualLink(linkId);
		this.recomputeNextLink = true;
		
	}
	
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

}
