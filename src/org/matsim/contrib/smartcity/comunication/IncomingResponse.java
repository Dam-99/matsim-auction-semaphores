package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class IncomingResponse extends ComunicationMessage{

	private HashMap<Id<Link>,Integer> incomingAgent;
	public IncomingResponse(ComunicationEntity sender, HashMap<Id<Link>,Integer> incomingAgent) {
		super(sender);
		this.setIncomingAgent(incomingAgent);
	}
	public HashMap<Id<Link>,Integer> getIncomingAgent() {
		return incomingAgent;
	}
	public void setIncomingAgent(HashMap<Id<Link>,Integer> incomingAgent) {
		this.incomingAgent = incomingAgent;
	}
	

}
