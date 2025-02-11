package org.matsim.contrib.smartcity.comunication;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

public class IncomingRequest extends ComunicationMessage  {

	private Set<Id<Lane>> lanes;
	private double time;
	
	public IncomingRequest(ComunicationEntity sender, Set<Id<Lane>> lanes,double time) {
		super(sender);
		this.setLinks(lanes);
		this.setTime(time);
	}

	public Set<Id<Lane>> getLinks() {
		return lanes;
	}

	public void setLinks(Set<Id<Lane>> lanes) {
		this.lanes = lanes;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
