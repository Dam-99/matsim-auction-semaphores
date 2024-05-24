package org.matsim.contrib.smartcity.actuation.semaphore;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Interface to communicate with semaphore
 * 
 * @author Filippo Muzzini
 *
 */
public interface SemaphoreComunicator {
	
	/**
	 * Ask to semaphore trying to free the link.
	 * The semaphore can try to free the link.
	 * 
	 * @param idLink id of Link
	 * @param urgent if the request is urgent
	 * @return true if the semaphore was able to satisfy the request
	 */
	public boolean needFreeLink(Id<Link> idLink, boolean urgent);
	
	/**
	 * Ask to semaphore trying to block the link.
	 * The semaphore can try to block the link.
	 * 
	 * @param idLink id of Link
	 * @param urgent if the request is urgent
	 * @return if the semaphore was able to satisfy the request
	 */
	public boolean closeLink(Id<Link> idLink, boolean urgent);

}
