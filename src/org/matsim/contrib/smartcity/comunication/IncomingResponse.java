package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

public class IncomingResponse extends ComunicationMessage{

	private HashMap<Id<Lane>,Integer> incomingAgent;
	public IncomingResponse(ComunicationEntity sender, HashMap<Id<Lane>,Integer> incomingAgent) {
		super(sender);
		this.setIncomingAgent(incomingAgent);
	}
	public HashMap<Id<Lane>,Integer> getIncomingAgent() {
		return incomingAgent;
	}
	public void setIncomingAgent(HashMap<Id<Lane>,Integer> incomingAgent) {
		this.incomingAgent = incomingAgent;
	}
	

}
