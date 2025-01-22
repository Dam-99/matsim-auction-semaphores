package org.matsim.contrib.smartcity.comunication;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

public class SemaphoreFlowMessage extends ComunicationMessage {

	private List<Tuple<List<Id<Link>>,Integer>>  agentsRouteAndBid;
	private int totalBidOfNotEquippedAgent;
	private double time;
	private Id<Link> actualLink;
	private Boolean equipped;
	public boolean is3640Message;
	
	/**
	 * Costruttore per messaggi di agenti equipaggiati
	 * @param sender
	 * @param agentRoute
	 * @param time
	 */
	public SemaphoreFlowMessage(ComunicationEntity sender, List<Tuple<List<Id<Link>>,Integer>> agentRoute,Id<Link> actualLink,double time) {
		super(sender);
		this.setAgentsRouteAndBid(agentRoute);
		this.setTime(time); 
		this.equipped = true;
		this.setActualLink(actualLink);
		this.is3640Message = false;
	}
	
	/**
	 * Costruttore per messaggi di agenti non equipaggiati
	 * @param sender
	 * @param agentRoute
	 * @param time
	 */
	public SemaphoreFlowMessage(ComunicationEntity sender, Id<Link> actualLink, int totalNotEquippedAgent, double time) {
		super(sender);
		this.setActualLink(actualLink);
		this.setTotalBidOfNotEquippedAgent(totalNotEquippedAgent);
		this.setTime(time);
		this.equipped = false;
		this.is3640Message = false;
	}

	public SemaphoreFlowMessage(ComunicationEntity sender, List<Tuple<List<Id<Link>>,Integer>> agentRoute,Id<Link> actualLink,double time,boolean is3640AgentMessage) {
        this(sender, agentRoute, actualLink, time);
        this.is3640Message = is3640AgentMessage;
	}

	public int getTotalBidOfNotEquippedAgent() {
		return totalBidOfNotEquippedAgent;
	}

	public void setTotalBidOfNotEquippedAgent(int totalNotEquippedAgent) {
		this.totalBidOfNotEquippedAgent = totalNotEquippedAgent;
	}

	public List<Tuple<List<Id<Link>>,Integer>> getAgentsRouteAndBid() {
		return agentsRouteAndBid;
	}

	public void setAgentsRouteAndBid(List<Tuple<List<Id<Link>>,Integer>> agentRoute) {
		this.agentsRouteAndBid = agentRoute;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public Id<Link> getActualLink() {
		return actualLink;
	}

	public void setActualLink(Id<Link> actualLink) {
		this.actualLink = actualLink;
	}
	
	public Boolean isEquipped() {
		return this.equipped;
	}

}
