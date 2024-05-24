/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class TurnNorthLogic extends AbstractDriverLogic {

	private static final int MAX_TRAVERSED = 10;
	
	@Inject private Network network;
	private Id<Link> nextLinkId;
	private int traversed = 0;
	
	@Override
	public Id<Link> getNextLinkId() {
		if (traversed >= MAX_TRAVERSED) {
			return null;
		}
		return nextLinkId;
	}
	
	@Override
	public void setActualLink(Id<Link> linkId) {
		super.setActualLink(linkId);
		traversed ++;
		doRight();
	}

	/**
	 * 
	 */
	private void doRight() {
		Id<Link> currentLinkId = this.actualLink;
		// Where do I want to move next?
				Link currentLink = network.getLinks().get(currentLinkId);
				//Node fromNode = currentLink.getFromNode();
				//Node toNode = currentLink.getToNode();
				Map<Id<Link>, ? extends Link> possibleNextLinks = currentLink.getToNode().getOutLinks();
				
				double maxNord = currentLink.getToNode().getCoord().getY();
				Link maxLink = null;
				for (Link link : possibleNextLinks.values()) {
					double nord = link.getToNode().getCoord().getY();
					if (nord >= maxNord) {
						maxLink = link;
						maxNord = nord;
					}
				}
				

				if (maxLink != null) {
					nextLinkId = maxLink.getId();
				} else {
					nextLinkId = possibleNextLinks.values().iterator().next().getId();
				}
		
	}

}
