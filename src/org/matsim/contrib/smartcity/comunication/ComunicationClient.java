/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.Set;

/**
 * Interface for client of cumunications system
 * @author Filippo Muzzini
 *
 */
public interface ComunicationClient extends ComunicationEntity {
	
	/**
	 * Return the set of reachable servers
	 * 
	 */
	public Set<ComunicationServer> discover();

}
