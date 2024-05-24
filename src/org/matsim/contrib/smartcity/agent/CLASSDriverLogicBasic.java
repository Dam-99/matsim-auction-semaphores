/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.ComunicationMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.contrib.smartcity.comunication.TrafficFlowMessage;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationWrapper;
import org.matsim.contrib.smartcity.perception.TrafficFlow;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class CLASSDriverLogicBasic extends StaticDriverLogic implements ComunicationClient {

	
	private NetworkInverter inverter;
	private Network invertedNet;
	@Inject private ComunicationWrapper wrapper;
	@Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject private QSim sim;
	@Inject private Config config;
	private TravelDisutility travel;
	private TravelTime time;

	@Inject
	public CLASSDriverLogicBasic(Network network, NetworkTurnInfoBuilderI turn) {
		super();
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
			int actualIndex = this.getActualIndex();
			List<Id<Link>> actualList = this.getLinksList();
			//double totalFlow = 0.0;
			for (Id<Link> l : actualList.subList(actualIndex, actualList.size())) {
				Double linkFlow = flow.getFlow(l);
				if (linkFlow != null) {
					remove(l);
					//totalFlow += linkFlow;
				}
			}
			List<Id<Link>> newList = reRoute();
			if (newList == null) {
				return;
			}
			this.setLinksList(newList);
			this.setActualLink(actualLink);
			
//			double r = MatsimRandom.getRandom().nextDouble();
//			if (r < totalFlow) {
//				List<Id<Link>> newList = reRoute();
//				if (newList == null) {
//					return;
//				}
//				this.setLinksList(newList);
//				this.setActualLink(actualLink);
//			}
		}
		
	}

	/**
	 * @return
	 */
	private List<Id<Link>> reRoute() {
		if (time == null) {
			this.time = TravelTimeCalculator.create(inverter.getInvertedNetwork(), ConfigUtils.addOrGetModule(config, TravelTimeCalculatorConfigGroup.class)).getLinkTravelTimes();
		}
		if (travel == null) {
			this.travel = this.travelDisutilityFactories.get(TransportMode.car).createTravelDisutility(time);
		}
		double actualTime = sim.getSimTimer().getTimeOfDay();
		Id<Node> fromNodeId = Id.createNodeId(actualLink);
		Id<Node> toNodeId = Id.createNodeId(endLink);
		Node fromNode = this.invertedNet.getNodes().get(fromNodeId);
		Node toNode = this.invertedNet.getNodes().get(toNodeId);
		if (fromNode == null || toNode == null) {
			return null;
		}
		Dijkstra dijkstra = (Dijkstra) new DijkstraFactory().createPathCalculator(invertedNet, travel, time);
		Path p = dijkstra.calcLeastCostPath(fromNode, toNode, actualTime, person, null);
		if (p == null) {
			return null;
		}
		return this.inverter.convertInvertedNodesToLinks(p.nodes).stream().map(l->l.getId()).collect(Collectors.toList());
	}

	/**
	 * @param l
	 */
	private void remove(Id<Link> l) {
		Id<Node> invNode = Id.createNodeId(l);	
		this.invertedNet.removeNode(invNode);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationClient#discover()
	 */
	@Override
	public Set<ComunicationServer> discover() {
		return this.wrapper.discover(actualLink);
	}

}
