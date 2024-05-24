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
public class ParkData {
	
	private String className;
	private Id<ActivityFacility> parkId;
	private Id<CameraPark> cameraId;
	
	public ParkData(String className, String cameraPark, String cameraId) {
		super();
		this.className = className;
		if (cameraPark != null)
			this.parkId = Id.create(cameraPark, ActivityFacility.class);
		if (cameraId != null) {
			this.cameraId = Id.create(cameraId, CameraPark.class);
		}
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Id<ActivityFacility> getParkId() {
		return parkId;
	}

	public void setParkId(Id<ActivityFacility> parkId) {
		this.parkId = parkId;
	}

	public Id<CameraPark> getCameraId() {
		return cameraId;
	}

	public void setCameraId(Id<CameraPark> cameraId) {
		this.cameraId = cameraId;
	}
	
	

}
