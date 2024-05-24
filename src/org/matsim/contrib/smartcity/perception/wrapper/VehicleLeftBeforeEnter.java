package org.matsim.contrib.smartcity.perception.wrapper;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * Represents that a vehicles left a link before enter in itself.
 * 
 * @author Filippo Muzzini
 *
 */
public class VehicleLeftBeforeEnter extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Id<Vehicle> vehicle;
	private Id<Link> idLink;

	public VehicleLeftBeforeEnter(Id<Vehicle> vehicle, Id<Link> idLink) {
		this.vehicle = vehicle;
		this.idLink = idLink;
	}

	public Id<Vehicle> getVehicle() {
		return vehicle;
	}

	public Id<Link> getIdLink() {
		return idLink;
	}
	
	@Override
	public String toString() {
		return "vehicle: "+vehicle+" link: "+idLink;
	}

}
