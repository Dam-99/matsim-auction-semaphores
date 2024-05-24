/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;

/**
 * Class that determinate if an agent can transit the link based on signals and priority.
 * @author Filippo Muzzini
 *
 */
public class SignalWithRightTurnAcceptanceLogic implements TurnAcceptanceLogic {
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic#isAcceptingTurn(org.matsim.api.core.v01.network.Link, org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI, org.matsim.api.core.v01.Id, org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle, org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork)
	 */
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh,
			QNetwork qNetwork, double now) {
		
		if ( (currentLane instanceof SignalizeableItem) && 
				(! ((SignalizeableItem)currentLane).hasGreenForToLink(nextLinkId)) ) {
			return AcceptTurn.WAIT;
		}
		
		//if signal is green consider the priority over roads with green
		Set<QLaneI> lanes = AccidentTurnAcceptanceLogic.getGreenQLane(currentLink.getToNode(), currentLink, qNetwork);
		List<Link> links = getLinksFromLanes(lanes);
		return PriorityTurnAcceptanceLogic.rightRule(currentLink, links, nextLinkId, qNetwork);
	}

	/**
	 * @param lanes
	 * @return
	 */
	private List<Link> getLinksFromLanes(Set<QLaneI> lanes) {
		ArrayList<Link> links = new ArrayList<Link>();
		for (QLaneI lane : lanes) {
			if (!lane.isNotOfferingVehicle()) {
				Link link = lane.getFirstVehicle().getCurrentLink();
				links.add(link);
			}
		}
		
		return links;
	}

}
