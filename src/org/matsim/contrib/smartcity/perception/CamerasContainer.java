/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.smartcity.perception.camera.Camera;

/**
 * @author Filippo Muzzini
 *
 */
public class CamerasContainer {
	
	private HashMap<Id<Camera>, Camera> cameras = new HashMap<Id<Camera>, Camera>();
	
	public void addCamera(Id<Camera> id, Camera camera) {
		cameras.put(id, camera);
	}
	
	public Camera getCamera(Id<Camera> camera) {
		return cameras.get(camera);
	}

	/**
	 * @return 
	 * @return
	 */
	public Collection<Camera> getAllCameras() {
		return cameras.values();
	}
	
	public Set<Id<Camera>> getAllId() {
		return cameras.keySet();
	}

}
