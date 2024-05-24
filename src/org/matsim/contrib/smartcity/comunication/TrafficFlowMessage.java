/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import org.matsim.contrib.smartcity.perception.TrafficFlow;

/**
 * @author Filippo Muzzini
 *
 */
public class TrafficFlowMessage extends ComunicationMessage {

	private TrafficFlow flow;
	
	/**
	 * @param sender
	 */
	public TrafficFlowMessage(ComunicationEntity sender, TrafficFlow flow) {
		super(sender);
		this.flow = flow;
	}
	
	public TrafficFlow getFlow() {
		return this.flow;
	}

}
