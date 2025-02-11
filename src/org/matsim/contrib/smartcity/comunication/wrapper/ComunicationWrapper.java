/**
 * 
 */
package org.matsim.contrib.smartcity.comunication.wrapper;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.lanes.Lane;

/**
 * Wrapper for comunications
 * @author Filippo Muzzini
 *
 */
public interface ComunicationWrapper {

	/**
	 * Return set of servers reachable in specified position
	 * @param position
	 * @return set of servers
	 */
	public Set<ComunicationServer> discover(Id<Lane> position);
}
