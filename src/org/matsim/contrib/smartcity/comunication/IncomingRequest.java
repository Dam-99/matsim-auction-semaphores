package org.matsim.contrib.smartcity.comunication;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class IncomingRequest extends ComunicationMessage  {

	private Set<Id<Link>> links;
	private double time;
	
	public IncomingRequest(ComunicationEntity sender, Set<Id<Link>> links,double time) {
		super(sender);
		this.setLinks(links);
		this.setTime(time);
	}

	public Set<Id<Link>> getLinks() {
		return links;
	}

	public void setLinks(Set<Id<Link>> links) {
		this.links = links;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
