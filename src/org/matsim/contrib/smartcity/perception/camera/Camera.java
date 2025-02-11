package org.matsim.contrib.smartcity.perception.camera;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;
import org.matsim.lanes.Lane;

/**
 * Generic camera that watch a link
 * 
 * @author Filippo Muzzini
 *
 */
public abstract class Camera {
	
	private CameraStatus status;
	
	/**
	 * Construct a camera.
	 * 
	 * @param idCameraStr arbitrary id for camera
	 * @param linkId id of link that camera watch
	 */
	public Camera(Id<Camera> idCamera, Id<Lane> linkId) {
		this.status = new CameraStatus(idCamera, linkId);
	}
	
	/**
	 * set the status of link.
	 * this emulate the action of watching
	 * @param status
	 */
	public void setStatus(LinkTrafficStatus status) {
		this.status.setTrafficStatus(status);
	}
	
	public CameraStatus getCameraStatus() {
		return this.status;
	}
	
	public Id<Lane> getLinkId(){
		return this.status.getIdLink();
	}

}
