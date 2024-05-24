/**
 * 
 */
package org.matsim.contrib.smartcity.agent.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

/**
 * @author Filippo Muzzini
 *
 */
public class CameraPark {

	private Id<CameraPark> id;
	private Id<ActivityFacility> parking;
	private FacilityWithMoreEntranceParkingManager manager;

	/**
	 * @param idCameraStr
	 * @param linkId
	 * @param wrapper
	 */
	public CameraPark(Id<CameraPark> idCameraStr, Id<ActivityFacility> parking, FacilityWithMoreEntranceParkingManager manager) {
		this.id = idCameraStr;
		this.parking = parking;
		this.manager = manager;
	}
	
	public double getFreeSpace() {
		return manager.getNrOfFreeParkingSpaces(this.parking);
	}

	/**
	 * @return the id
	 */
	public Id<CameraPark> getId() {
		return id;
	}
}
