package org.matsim.contrib.smartcity.comunication;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CrossedMessage extends ComunicationMessage{

	private int bid;
	private Id<Link> link;
	private double time;
	
	public CrossedMessage(ComunicationEntity sender,int bid,Id<Link> link,double time) {
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

	public Id<Link> getLink() {
		return link;
	}

	public void setLink(Id<Link> link) {
		this.link = link;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

}
