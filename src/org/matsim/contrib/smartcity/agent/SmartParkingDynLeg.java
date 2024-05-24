/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * Interface that define the method that can be called for using an agent parking logic.
 * This class use parking manager to ask if a parking is free and available.
 * 
 * @author Filippo Muzzini
 *
 */
public class SmartParkingDynLeg implements DriverDynLeg {

	private Id<Link> currentLinkId;
	private ParkingSearchManager parkingManager;
	private ParkingSearchLogic logic;
	private boolean hasFoundParking;
	private Tuple<Id<Link>, Id<Link>> currentAndNextParkLink;
	private String mode;
	private Id<Vehicle> plannedVehicleId;

	/**
	 * Take the logic, parking manager, current position and the last Leg
	 * to determinate the behavior. In particular the last leg is necessary to determinate
	 * the vehicle used in the previous leg.
	 * 
	 * @param parkingLogic parking logic
	 * @param parkingManager parking manager
	 * @param lastLeg the last leg
	 * @param currentLinkId current position
	 */
	public SmartParkingDynLeg(ParkingSearchLogic parkingLogic, ParkingSearchManager parkingManager, Leg lastLeg, Id<Link> currentLinkId) {
		this.parkingManager = parkingManager;
		this.logic = parkingLogic;
		this.mode = lastLeg.getMode();
		this.plannedVehicleId = ((NetworkRoute) lastLeg.getRoute()).getVehicleId();
		this.currentLinkId = currentLinkId;
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkId = newLinkId;
		hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(this.getPlannedVehicleId(), currentLinkId);
	}

	@Override
	public Id<Link> getNextLinkId() {
		if (hasFoundParking) {
			// easy, we can just park where at our destination link
			return null;
		} else {
			if (this.currentAndNextParkLink != null) {
				if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
					// we already calculated this
					return currentAndNextParkLink.getSecond();
				}
			}
			// need to find the next link
			Id<Link> nextLinkId = this.logic.getNextLink(currentLinkId, this.getPlannedVehicleId());
			currentAndNextParkLink = new Tuple<Id<Link>, Id<Link>>(currentLinkId, nextLinkId);
			return nextLinkId;

		}
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return null;
	}

	@Override
	public void finalizeAction(double now) {
	}

	@Override
	public String getMode() {
		return this.mode;
	}

	
	@Override
	public Id<Vehicle> getPlannedVehicleId()
	{
	    return this.plannedVehicleId;
	}
	
	
	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		currentLinkId = linkId;
	}

	@Override
	public Double getExpectedTravelTime() {
		// ci si aspetta un parcheggio veloce
		return new Double(0);
	}

	public Double getExpectedTravelDistance() {
		// ci si aspetta un parcheggio veloce
		return new Double(0);
	}
}
