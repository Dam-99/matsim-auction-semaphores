package org.matsim.contrib.smartcity.perception.camera;

/**
 * Listener of ActiveCamera
 * 
 * @author Filippo Muzzini
 *
 */
public interface CameraListener {
	
	/**
	 * Invoked when there is a change
	 * 
	 * @param status
	 */
	public void pushCameraStatus(CameraStatus status, double time);

}
