package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.SignalGroup;

public class SSBootUpMessage extends ComunicationMessage{
	
	private HashMap<Id<Link>, SignalGroup> signalMap;
	
	public SSBootUpMessage(ComunicationEntity sender, HashMap<Id<Link>, SignalGroup> signalMap) {
		super(sender);
		this.setSignalMap(signalMap);
	}
	
	public HashMap<Id<Link>, SignalGroup> getSignalMap() {
		return signalMap;
	}
	
	public void setSignalMap(HashMap<Id<Link>, SignalGroup> signalMap) {
		this.signalMap = signalMap;
	}

}
