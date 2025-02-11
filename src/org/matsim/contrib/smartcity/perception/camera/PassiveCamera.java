package org.matsim.contrib.smartcity.perception.camera;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;
import org.matsim.contrib.smartcity.perception.wrapper.PassivePerceptionWrapper;
import org.matsim.lanes.Lane;

/**
 * A passive camera that watch only when a generic system call its methods
 * 
 * @author Filippo Muzzini
 *
 */
public class PassiveCamera extends Camera {

	private PassivePerceptionWrapper wrapper;

	public PassiveCamera(Id<Camera> idCameraStr, Id<Lane> linkId, PassivePerceptionWrapper wrapper) {
		super(idCameraStr, linkId);
		this.wrapper = wrapper;
	}
	
	@Override
	public CameraStatus getCameraStatus() {
		LinkTrafficStatus status = this.wrapper.getLinkTrafficStatus(this.getLinkId());
		this.setStatus(status);
		return this.getCameraStatus();
	}

}
