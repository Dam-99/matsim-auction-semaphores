/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author Filippo Muzzini
 *
 */
public class TrafficFlow {
	
	private HashMap<Id<Link>, Double> flow = new HashMap<Id<Link>, Double>();
	
	public void addFlow(Id<Link> link, Double flow) {
		this.flow.put(link, flow);
	}
	
	public Double getFlow(Id<Link> link) {
		return this.flow.get(link);
	}

}
