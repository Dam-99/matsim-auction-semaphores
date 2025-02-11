package org.matsim.contrib.smartcity.comunication;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

public class CrossedMessage extends ComunicationMessage{

	private int bid;
	private Id<Lane> lane;
	private double time;
	
	public CrossedMessage(ComunicationEntity sender,int bid,Id<Lane> link,double time) {
		super(sender);
		this.setBid(bid);
		this.setLink(link);
		this.setTime(time);
	}

	public int getBid() {
		return bid;
	}

	public void setBid(int bid) {
		this.bid = bid;
	}

	public Id<Lane> getLink() {
		return lane;
	}

	public void setLink(Id<Lane> lane) {
		this.lane = lane;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
