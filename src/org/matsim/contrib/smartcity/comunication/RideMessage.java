package org.matsim.contrib.smartcity.comunication;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class RideMessage extends ComunicationMessage {

	private final double time;
    private int index;
    private List<Id<Link>> route;
    
	public RideMessage(ComunicationEntity sender, int index, List<Id<Link>> route, double time) {
		super(sender);
		this.setIndex(index);
        this.time = time;
        this.setRoute(route);
	}

	public double getTime() {
		return time;
	}

	public List<Id<Link>> getRoute() {
		return route;
	}

	public void setRoute(List<Id<Link>> route) {
		this.route = route;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
