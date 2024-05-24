/**
 * 
 */
package org.matsim.contrib.smartcity.restriction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.router.NetworkWithSignalsTurnInfoBuilder;
import org.matsim.contrib.smartcity.actuation.semaphore.SmartSemaphoreModule;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class NetworkWithRestrictionTurnInfoBuilder implements NetworkTurnInfoBuilderI {
	
	public static final String RESTRICTION_ATT = "restriction";
	public static final String RESTRICTION_SEP = ";";
	
	private NetworkWithSignalsTurnInfoBuilder delegateSignal;
	private NetworkTurnInfoBuilder delegate;
	private Network network;
	private Scenario scenario;
	
	@Inject
	public NetworkWithRestrictionTurnInfoBuilder(Scenario scenario) {
		delegateSignal = new NetworkWithSignalsTurnInfoBuilder(scenario);
		delegate = new NetworkTurnInfoBuilder(scenario);
		network = scenario.getNetwork();
		this.scenario = scenario;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI#createAllowedTurnInfos()
	 */
	@Override
	public Map<Id<Link>, List<TurnInfo>> createAllowedTurnInfos() {
		SmartSemaphoreModule.createScenarioElement(scenario);
		Map<Id<Link>, List<TurnInfo>> signalTurn = delegateSignal.createAllowedTurnInfos();
		
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		Map<Id<Link>, List<TurnInfo>> restrictionTurn = new HashMap<Id<Link>, List<TurnInfo>>();
		for (Link l : network.getLinks().values()) {
			ArrayList<TurnInfo> turnList = new ArrayList<TurnInfo>();
			Set<Id<Link>> allowedLinks = new HashSet<Id<Link>>(l.getToNode().getOutLinks().keySet());
			Set<Id<Link>> notAllowedLinks = getRestrictions(l);
			allowedLinks.removeAll(notAllowedLinks);
			//removing U inversion
			Set<Id<Link>> notAllowedU = getNotAllowedU(l);
			allowedLinks.removeAll(notAllowedU);
			allowedLinks.stream().forEach(to -> turnList.add(new TurnInfo(l.getId(), to, modes)));
			
			restrictionTurn.put(l.getId(), turnList);
		}
		
		delegate.mergeTurnInfoMaps(signalTurn, restrictionTurn);
		return signalTurn;
	}
	
	/**
	 * @return
	 */
	private Set<Id<Link>> getNotAllowedU(Link fromLink) {
		Set<Id<Link>> notAllowedU = new HashSet<Id<Link>>();
		Collection<? extends Link> nextLinks = fromLink.getToNode().getOutLinks().values();
		if (nextLinks.size() == 1) {
			return notAllowedU;
		}
		
		for (Link link : nextLinks) {
			if (link.getToNode().equals(fromLink.getFromNode())) {
				notAllowedU.add(link.getId());
			}
		}
		
		return notAllowedU;
		
	}

	private Set<Id<Link>> getRestrictions(Link fromLink) {
		String inibStr = (String) fromLink.getAttributes().getAttribute(RESTRICTION_ATT);
		if (inibStr == null) {
			return new HashSet<Id<Link>>();
		}
		List<String> inibList = Arrays.asList(inibStr.split(RESTRICTION_SEP));
		return inibList.stream().map(Id::createLinkId).collect(Collectors.toSet());
	}

}
