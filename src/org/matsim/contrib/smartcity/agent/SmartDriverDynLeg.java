/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * Class that implements the agent driver behavior using SmartDriverLogic
 * 
 * @author Filippo Muzzini
 *
 */
public class SmartDriverDynLeg implements DriverDynLeg {

	private Leg leg;
	private SmartDriverLogic logic;
	private Id<Vehicle> plannedVehicleId;

	/**
	 * @param leg
	 * @param smartDriverLogic
	 */
	public SmartDriverDynLeg(Leg leg, SmartDriverLogic smartDriverLogic) {
		this.leg = leg;
		this.logic = smartDriverLogic;
		this.logic.setLeg(this.leg);
		this.plannedVehicleId = ((NetworkRoute) leg.getRoute()).getVehicleId();
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynAction#finalizeAction(double)
	 */
	@Override
	public void finalizeAction(double now) {
		this.logic.finalizeAction(now);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynLeg#getMode()
	 */
	@Override
	public String getMode() {
		return this.leg.getMode();
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynLeg#arrivedOnLinkByNonNetworkMode(org.matsim.api.core.v01.Id)
	 */
	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		this.logic.setActualLink(Id.create(linkId.toString() + ".ol", Lane.class));
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynLeg#getDestinationLinkId()
	 */
	@Override
	public Id<Link> getDestinationLinkId() {
		return Id.create(this.logic.getDestinationLinkId().toString().split("\\.")[0], Link.class);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynLeg#getExpectedTravelTime()
	 */
	@Override
	public Double getExpectedTravelTime() {
		return this.logic.getTravelTime();
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynLeg#getExpectedTravelDistance()
	 */
	@Override
	public Double getExpectedTravelDistance() {
		return this.logic.getDistance();
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DriverDynLeg#getNextLinkId()
	 */
	@Override
	public Id<Link> getNextLinkId() {
		// TODO: check if this works when only one lane exists
		Id<Link> id = Id.create(this.logic.getNextLinkId().toString().split("\\.")[0], Link.class);
		return id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DriverDynLeg#movedOverNode(org.matsim.api.core.v01.Id)
	 */
	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		// TODO: check if this works when only one lane exists
		this.movedOverNodeWithLane(Id.create(newLinkId.toString() + ".ol", Lane.class));
	}

	public void movedOverNodeWithLane(Id<Lane> newLinkId) {
		this.logic.setActualLink(newLinkId);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DriverDynLeg#getPlannedVehicleId()
	 */
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return this.plannedVehicleId;
	}

}
