/**
 * 
 */
package org.matsim.contrib.smartcity.agent.parking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * @author Filippo Muzzini
 *
 */
public class ParksContainer {
	
	private HashMap<Id<CameraPark>, CameraPark> cameras = new HashMap<Id<CameraPark>, CameraPark>();
	
	public void addCamera(Id<CameraPark> id, CameraPark camera) {
		cameras.put(id, camera);
	}
	
	public CameraPark getCamera(Id<CameraPark> camera) {
		return cameras.get(camera);
	}

	/**
	 * @return 
	 * @return
	 */
	public Collection<CameraPark> getAllCameras() {
		return cameras.values();
	}
	
	public Set<Id<CameraPark>> getAllId() {
		return cameras.keySet();
	}

}
