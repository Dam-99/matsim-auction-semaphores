package org.matsim.contrib.smartcity.comunication;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

public class RideMessage extends ComunicationMessage {

	private final double time;
    private int index;
	private int indexLanes;
    private List<Id<Lane>> route;
	private List<Id<Lane>> routeLanes;
    
	public RideMessage(ComunicationEntity sender, int index, List<Id<Lane>> route, int indexLanes, List<Id<Lane>> routeLanes, double time) {
		super(sender);
		this.setIndex(index);
		this.setIndexLanes(indexLanes);
        this.time = time;
        this.setRoute(route);
		this.setRouteLanes(routeLanes);
	}

	public double getTime() {
		return time;
	}

	public List<Id<Lane>> getRoute() {
		return route;
	}

	public List<Id<Lane>> getRouteLanes() {
		return routeLanes;
	}

	public void setRoute(List<Id<Lane>> route) {
		this.route = route;
	}

	public void setRouteLanes(List<Id<Lane>> routeLanes) {
		this.routeLanes = routeLanes;
	}

	public int getIndex() {
		return index;
	}

	public int getIndexLanes() {
		return indexLanes;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setIndexLanes(int indexLanes) {
		this.indexLanes = indexLanes;
	}

}
