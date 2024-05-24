/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.perception.camera.Camera;

/**
 * Class that represents the camera params
 * 
 * @author Filippo Muzzini
 *
 */
public class CameraData {
	
	private String className;
	private Id<Link> linkId;
	private Id<Camera> cameraId;
	
	/**
	 * @param className name of the camera class
	 * @param cameraLink link that the camera watch
	 * @param cameraId id of camera
	 */
	public CameraData(String className, String cameraLink, String cameraId) {
		super();
		this.className = className;
		if (cameraLink != null)
			this.linkId = Id.createLinkId(cameraLink);
		if (cameraId != null)
			this.cameraId = Id.create(cameraId, Camera.class);
	}
	
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public void setLinkId(Id<Link> linkId) {
		this.linkId = linkId;
	}

	public Id<Camera> getCameraId() {
		return cameraId;
	}

	public void setCameraId(Id<Camera> cameraId) {
		this.cameraId = cameraId;
	}
	
}
