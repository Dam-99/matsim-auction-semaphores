/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

/**
 * @author Filippo Muzzini
 *
 */
public class TrafficFlow {
	
	private HashMap<Id<Lane>, Double> flow = new HashMap<Id<Lane>, Double>();
	
	public void addFlow(Id<Lane> link, Double flow) {
		this.flow.put(link, flow);
	}
	
	public Double getFlow(Id<Lane> link) {
		return this.flow.get(link);
	}

}
