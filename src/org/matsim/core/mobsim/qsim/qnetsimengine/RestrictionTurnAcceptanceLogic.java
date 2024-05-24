/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.restriction.NetworkWithRestrictionTurnInfoBuilder;

/**
 * Class that determinate if an agent can travers the link based on restrictions.
 * @author Filippo Muzzini
 *
 */
public class RestrictionTurnAcceptanceLogic implements TurnAcceptanceLogic {

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic#isAcceptingTurn(org.matsim.api.core.v01.network.Link, org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI, org.matsim.api.core.v01.Id, org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle, org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork)
	 */
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh,
			QNetwork qNetwork, double now) {
		Link nextLink = qNetwork.getNetwork().getLinks().get(nextLinkId);
		
		//U inversion allowed only when there isn't another way
		boolean isU = currentLink.getFromNode() == nextLink.getToNode();
		if (isU && currentLink.getToNode().getOutLinks().size() > 1) {
			return AcceptTurn.ABORT;
		}
		
		String restrStr = (String) currentLink.getAttributes().getAttribute(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_ATT);
		if (restrStr == null ) {
			return AcceptTurn.GO;
		}
		List<String> restrictions = Arrays.asList(restrStr.split(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_SEP));
		List<Id<Link>> restrictionLinks = restrictions.stream().map(Id::createLinkId).collect(Collectors.toList());
		if (restrictionLinks.contains(nextLinkId)) {
			return AcceptTurn.ABORT;
		} else {
			return AcceptTurn.GO;
		}
	}

}
