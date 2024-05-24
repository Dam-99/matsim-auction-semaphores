package org.matsim.contrib.smartcity.perception.camera;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;

/**
 * Class to wrap what camera watch and info about it.
 * 
 * @author Filippo Muzzini
 *
 */
public class CameraStatus {
	
	private Id<Camera> cameraId;
	private Id<Link> whatchedLink;
	private LinkTrafficStatus linkStatus;
	
	public CameraStatus(Id<Camera> cameraId, Id<Link> whatchedLink) {
		this.cameraId = cameraId;
		this.whatchedLink = whatchedLink;
		this.linkStatus = new LinkTrafficStatus();
	}
	
	public Id<Camera> getCameraId(){
		return this.cameraId;
	}
	
	public Id<Link> getIdLink(){
		return this.whatchedLink;
	}
	
	public LinkTrafficStatus getLinkStatus() {
		return this.linkStatus;
	}

	public void setTrafficStatus(LinkTrafficStatus status) {
		this.linkStatus = status;		
	}

}
