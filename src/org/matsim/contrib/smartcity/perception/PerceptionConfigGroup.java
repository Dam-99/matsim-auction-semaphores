/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config params for readeing xml
 * @author Filippo Muzzini
 *
 */
public class PerceptionConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GRUOPNAME = "perception";
	public static final String WRAPPER_CLASS = "wrapper";
	public static final String CAMERA_FILE = "camerafile";
	
	private String wrapperClass;
	private String cameraFile;


	public PerceptionConfigGroup() {
		super(GRUOPNAME);
	}

	@StringGetter(WRAPPER_CLASS)
	public String getWrapperClass() {
		return wrapperClass;
	}

	@StringSetter(WRAPPER_CLASS)
	public void setWrapperClass(String wrapperClass) {
		this.wrapperClass = wrapperClass;
	}

	@StringGetter(CAMERA_FILE)
	public String getCameraFile() {
		return cameraFile;
	}

	@StringSetter(CAMERA_FILE)
	public void setCameraFile(String cameraFile) {
		this.cameraFile = cameraFile;
	}
	
	

}
