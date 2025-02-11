package org.matsim.contrib.smartcity.comunication;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

public class RideMessage extends ComunicationMessage {

	private final double time;
    private int index;
    private List<Id<Lane>> route;
    
	public RideMessage(ComunicationEntity sender, int index, List<Id<Lane>> route, double time) {
		super(sender);
		this.setIndex(index);
        this.time = time;
        this.setRoute(route);
	}

	public double getTime() {
		return time;
	}

	public List<Id<Lane>> getRoute() {
		return route;
	}

	public void setRoute(List<Id<Lane>> route) {
		this.route = route;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
