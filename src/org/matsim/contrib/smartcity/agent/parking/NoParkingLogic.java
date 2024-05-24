/**
 * 
 */
package org.matsim.contrib.smartcity.agent.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.vehicles.Vehicle;

/**
 * Simple class for ParkingSearchLogic.
 * This class always find the park in the actual link.
 * the behavior is the same of standard MATSim without parking module.
 * 
 * @author Filippo Muzzini
 *
 */
public class NoParkingLogic implements ParkingSearchLogic {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic#getNextLink(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic#reset()
	 */
	@Override
	public void reset() {

	}

}
