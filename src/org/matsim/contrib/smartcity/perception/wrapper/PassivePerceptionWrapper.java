package org.matsim.contrib.smartcity.perception.wrapper;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

/**
 * Interface to a substrate for perception of network status.
 * This is the simpler interface to monitoring the roads status;
 * is a passive interface, so it doesn't notify the changes of the roads.
 * 
 * @author Filippo Muzzini
 * 
 * @see ActivePerceptionWrapper
 * @see PassivePerceptionWrapperImpl
 *
 */
public interface PassivePerceptionWrapper extends LinkEnterEventHandler, LinkLeaveEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	/**
	 * Return the status of Link
	 * 
	 * @param idLink Id of Link that want to get the status
	 * @return Status of Link
	 */
	public LinkTrafficStatus getLinkTrafficStatus(Id<Link> idLink);
	
	/**
	 * Return a complete map of flow
	 */
	public HashMap<Id<Link>, LinkTrafficStatus> getTrafficMap();
	/**
	 * Return the total number of vehicles on link
	 * 
	 * @param idLink Id of link that want to get the number of vehicles
	 * @return The total number of vehicles
	 */
	public int getTotalVehicleOnLink(Id<Link> idLink);
	
	/**
	 * Return the number of specified type vehicles on link 
	 * 
	 * @param idLink Id of link that want to get the number of vehicles
	 * @param idType Type of vehicles
	 * @return Number of vehicles
	 */
	public int getTypeVehicleOnLink(Id<Link> idLink, Id<VehicleType> idType);
	
}
