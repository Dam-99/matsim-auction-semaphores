package org.matsim.contrib.smartcity.perception.wrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Listener for ActivePerceptionWrapper
 * 
 * @author Filippo Muzzini
 * 
 * @see ActivePerceptionWrapper
 *
 */
public interface LinkChangedListener {
	
	/**
	 * Method invoked when there is a change
	 * 
	 * @param idLink id of link
	 * @param status state of link
	 */
	public void publishLinkChanged(Id<Link> idLink, LinkTrafficStatus status, double time);

}
