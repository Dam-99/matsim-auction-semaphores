package org.matsim.contrib.smartcity.perception.wrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.lanes.Lane;

/**
 * This is the active version of PassivePerceptionWrapper.
 * Class that implements this interface can notify that a road status is changed
 * 
 * @author Filippo Muzzini
 * 
 * @see ActivePerceptionWrapperImpl
 * @see PassivePerceptionWrapper
 *
 */
public interface ActivePerceptionWrapper extends PassivePerceptionWrapper {
	
	/**
	 * Add a listener to notify the link change
	 * 
	 * @param listener the listener
	 * @param linkId the link
	 */
	public void addLinkChangedListener(LinkChangedListener listener, Id<Lane> linkId);

	/**
	 * Add a listener to notify a change of every link
	 * 
	 * @param listener the listener
	 */
	public void addLinkChangedListener(LinkChangedListener listener);

	/**
	 * Add a listener to notify the links change
	 * 
	 * @param listener listener
	 * @param links list of links
	 */
	void addLinkChangedListener(LinkChangedListener listener, Iterable<Id<Lane>> links);

}
