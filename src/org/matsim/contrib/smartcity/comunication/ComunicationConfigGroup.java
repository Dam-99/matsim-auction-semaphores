/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Filippo Muzzini
 *
 */
public class ComunicationConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUPNAME = "comunication";
	public static final String WRAPPER = "wrapper";
	public static final String SERVERLIST = "serverlist";
	public static final String RANGE = "range";
	
	private String wrapper;
	private String serverList;
	private String range;

	public ComunicationConfigGroup() {
		super(GROUPNAME);
	}
	
	@StringSetter(WRAPPER)
	public void setWrapper(String wrapper) {
		this.wrapper = wrapper;
	}
	
	@StringGetter(WRAPPER)
	public String getWrapper() {
		return this.wrapper;
	}
	
	@StringSetter(SERVERLIST)
	public void setServerList(String serverList) {
		this.serverList = serverList;
	}
	
	@StringGetter(SERVERLIST)
	public String getServerList() {
		return this.serverList;
	}
	
	@StringSetter(RANGE)
	public void setRange(String range) {
		this.range = range;
	}
	
	@StringGetter(RANGE)
	public String getRange() {
		return this.range;
	}

}
