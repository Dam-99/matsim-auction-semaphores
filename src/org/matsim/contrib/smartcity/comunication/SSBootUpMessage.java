package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.Lane;

public class SSBootUpMessage extends ComunicationMessage{
	
	private HashMap<Id<Lane>, SignalGroup> signalMap;
	
	public SSBootUpMessage(ComunicationEntity sender, HashMap<Id<Lane>, SignalGroup> signalMap) {
		super(sender);
		this.setSignalMap(signalMap);
	}
	
	public HashMap<Id<Lane>, SignalGroup> getSignalMap() {
		return signalMap;
	}
	
	public void setSignalMap(HashMap<Id<Lane>, SignalGroup> signalMap) {
		this.signalMap = signalMap;
	}

}
