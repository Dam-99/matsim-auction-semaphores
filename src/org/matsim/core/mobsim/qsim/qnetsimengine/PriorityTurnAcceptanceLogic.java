/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.smartcity.analisys.AttendingIntersectionTime;

/**
 * Class that determinate if an agent can travers the link based on priority roads.
 * @author Filippo Muzzini
 *
 */
public class PriorityTurnAcceptanceLogic implements TurnAcceptanceLogic {
	
	public static final String PRIORITY_ATT = "priority";
	public static final String GIVE_WAY = "give_way";
	public static final String PRIORITY_ROAD = "designated";
	
	public static final String WAY_TYPE = "way_type";
	
	private HashMap<String, Integer> wayLevels;

	@Inject
	private AttendingIntersectionTime attendingIntersectionTime;

	public PriorityTurnAcceptanceLogic() {
		wayLevels = new HashMap<String, Integer>();
		wayLevels.put("motorway", 0);
		wayLevels.put("motorway_link", 1);
		wayLevels.put("trunk", 2);
		wayLevels.put("trunk_link", 3);
		wayLevels.put("primary", 4);
		wayLevels.put("primary_link", 5);
		wayLevels.put("secondary", 6);
		wayLevels.put("secondary_link", 7);
		wayLevels.put("tertiary", 8);
		wayLevels.put("tertiary_link", 9);
		wayLevels.put("unclassified", 10);
		wayLevels.put("residential", 11);
		wayLevels.put("service", 12);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic#isAcceptingTurn(org.matsim.api.core.v01.network.Link, org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI, org.matsim.api.core.v01.Id, org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle, org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork)
	 */
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh,
			QNetwork qNetwork, double now) {
		AcceptTurn res;
		attendingIntersectionTime.addStart(veh, now);

		Collection<? extends Link> links = currentLink.getToNode().getInLinks().values();
		
		String priority = (String) currentLink.getAttributes().getAttribute(PRIORITY_ATT);
		if (priority != null && priority.equals(PRIORITY_ROAD)) {
			List<Link> priorityRoads = getPriorityRoads(links);
			priorityRoads.remove(currentLink);
			res = rightRule(currentLink, priorityRoads, nextLinkId, qNetwork);
			makeEvent(res, veh, now);
			return res;
		}
		
		if (priority != null && priority.equals(GIVE_WAY)) {
			List<Link> priorityRoads = getNoGiveWayRoads(links);
			if (!areFree(priorityRoads, qNetwork)) {
				res = AcceptTurn.WAIT;
				makeEvent(res, veh, now);
				return res;
			}
			
			List<Link> giveWayRoads = getGiveWayRoads(links);
			giveWayRoads.remove(currentLink);
			res = rightRule(currentLink, giveWayRoads, nextLinkId, qNetwork);
			makeEvent(res, veh, now);
			return res;
		}
		
		Integer currentWayLevel = wayLevels.get(currentLink.getAttributes().getAttribute(WAY_TYPE));
		currentWayLevel = currentWayLevel != null ? currentWayLevel : Integer.MAX_VALUE;
		List<Link> upperRoads = getUpperRoads(currentWayLevel, links);
		upperRoads.remove(currentLink);
		if (!areFree(upperRoads, qNetwork)) {
			res = AcceptTurn.WAIT;
			makeEvent(res, veh, now);
			return res;
		}
		
		List<Link> myLevelRoads = getMyLevelRoads(currentLink, currentWayLevel, links);
		res = rightRule(currentLink, myLevelRoads, nextLinkId, qNetwork);
		makeEvent(res, veh, now);

		return res;
	}

	private void makeEvent(AcceptTurn res, QVehicle veh, double now) {
		if (AcceptTurn.GO.equals(res)){
			attendingIntersectionTime.addEnd(veh, now);
		}
	}

	/**
	 * @param currentWayLevel
	 * @param links
	 * @return
	 */
	private List<Link> getMyLevelRoads(Link currentLink, int currentWayLevel, Collection<? extends Link> links) {
		ArrayList<Link> egualRoads = new ArrayList<Link>();
		for (Link link : links) {
			Object wayType = link.getAttributes().getAttribute(WAY_TYPE);
			int level = Integer.MAX_VALUE;
			if (wayType != null){
				level = wayLevels.get(wayType);
			}
			if (level == currentWayLevel) {
				egualRoads.add(link);
			}
		}
		
		egualRoads.remove(currentLink);
		return egualRoads;
	}

	/**
	 * @param currentWayLevel
	 * @param links
	 * @return
	 */
	private List<Link> getUpperRoads(int currentWayLevel, Collection<? extends Link> links) {
		ArrayList<Link> upperRoads = new ArrayList<Link>();
		for (Link link : links) {
			Object wayType = link.getAttributes().getAttribute(WAY_TYPE);
			int level = Integer.MAX_VALUE;
			if (wayType != null){
				level = wayLevels.get(wayType);
			}
			if (level < currentWayLevel) {
				upperRoads.add(link);
			}
		}
		
		return upperRoads;
	}

	/**
	 * @param links
	 * @return
	 */
	private List<Link> getGiveWayRoads(Collection<? extends Link> links) {
		return links.stream().filter(l -> {
			String priority = (String) l.getAttributes().getAttribute(PRIORITY_ATT);
			if (priority == null)
				return false;
			return priority.equals(GIVE_WAY);
			}).collect(Collectors.toList());
	}

	/**
	 * @param priorityRoads
	 * @return
	 */
	private static boolean areFree(List<Link> links, QNetwork qNetwork) {
		for (Link link : links) {
			QLinkI qLink = qNetwork.getNetsimLink(link.getId());
			if (!qLink.isNotOfferingVehicle()) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @param links
	 * @return
	 */
	private List<Link> getNoGiveWayRoads(Collection<? extends Link> links) {
		return links.stream().filter(l -> {
			String priority = (String) l.getAttributes().getAttribute(PRIORITY_ATT);
			if (priority == null)
				return true;
			return !priority.equals(GIVE_WAY);
			}).collect(Collectors.toList());
	}

	/**
	 * Return if the agents on current link can transit based on the rule which the roads on the right
	 * have the priority.
	 * @param currentLink
	 * @param priorityRoads
	 * @return
	 */
	protected static AcceptTurn rightRule(Link currentLink, List<Link> inRoads, Id<Link> nextLinkId, QNetwork qNetwork) {
		if (areFree(inRoads, qNetwork)) {
			return AcceptTurn.GO;
		}
		
		//at this point there is at least one other vehicle
		Network network = qNetwork.getNetwork();
		Link nextLink = network.getLinks().get(nextLinkId);
		Node nextLinkEndNode = nextLink.getToNode();
		//if i want to do a U inversion i must wait others vehicles
		if (nextLinkEndNode.equals(currentLink.getFromNode())) {
			return AcceptTurn.WAIT;
		}
		
		//wait vehicles on link with less angle
		double nextLinkAngle = getAngle(currentLink, nextLink);
		ArrayList<Link> priorityLinks = new ArrayList<Link>();
		for (Link link : inRoads) {
			double angle = getAngle(currentLink, link);
			if (angle < nextLinkAngle && angle < Math.PI*2) {
				priorityLinks.add(link);
				continue;
			}
			
			//actual implementetion of QLinkImpl offering the same qland as accepting
			QVehicle other = qNetwork.getNetsimLink(link.getId()).getAcceptingQLane().getFirstVehicle();
			if (other == null) {
				continue;
			}
			Id<Link> otherNextLinkId = other.getDriver().chooseNextLinkId();
			Link otherNextLink = network.getLinks().get(otherNextLinkId);
			if (angle > Math.PI*2 && angle < nextLinkAngle && !intersect(currentLink, nextLink, link, otherNextLink)) {
				priorityLinks.add(link);
			}
		}
		
		
		if (areFree(priorityLinks, qNetwork)) {
			return AcceptTurn.GO;
		} else {
			//if others that i must do priority must give me priority there is a deadlock
			Stack<Link> toCheck = new Stack<Link>();
			toCheck.addAll(priorityLinks);
			while (!toCheck.isEmpty()) {
				Link link = toCheck.pop();
				QVehicle other = qNetwork.getNetsimLink(link.getId()).getAcceptingQLane().getFirstVehicle();
				if (other == null) {
					continue;
				}
				Link nextLinkOther = network.getLinks().get(other.getDriver().chooseNextLinkId());
				if (nextLinkOther == null) continue;
				double nextLinkAngleOther = getAngle(link, nextLinkOther);
				double angle = getAngle(link, currentLink);
				if (angle < nextLinkAngleOther && angle < Math.PI*2) {
					return AcceptTurn.GO;
				}
				for (Link pLink : inRoads) {
					if (pLink == link) continue;
					double pAngle = getAngle(link, pLink);
					if (pAngle < nextLinkAngleOther && pAngle < Math.PI*2) {
						toCheck.add(pLink);
					}
				}
			}
			
			return AcceptTurn.WAIT;
		}
	}

	/**
	 * @param currentLink
	 * @param link
	 * @param otherNextLink 
	 * @param link2 
	 * @return
	 */
	private static boolean intersect(Link from1, Link to1, Link from2, Link to2) {
		double angle = getAngle(from2, to2);
		
		//if turn left no intersection because we assume that veh 1 turn left.
		//can be generalized
		if (angle >= Math.PI) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param links
	 * @return
	 */
	private List<Link> getPriorityRoads(Collection<? extends Link> links) {
		return links.stream().filter(l -> {
			String priority = (String) l.getAttributes().getAttribute(PRIORITY_ATT);
			return priority != null ? priority.equals(PRIORITY_ROAD) : false;
			}).collect(Collectors.toList());
	}

	private static double getAngle(Link link1, Link link2) {
		if (link2.getToNode().equals(link1.getFromNode())) {
			return 2*Math.PI;
		}
		Coord coordInLink = getInVector(link1);
		double thetaInLink = getPositiveAngle(coordInLink);

		Coord coordOutLink;
		if (link2.getToNode().equals(link1.getToNode())) {
			coordOutLink = getInVector(link2);
		} else {
			coordOutLink = getOutVector(link2);
		}
		double thetaOutLink = getPositiveAngle(coordOutLink);
		double thetaDiff = thetaOutLink - thetaInLink;
		if (thetaDiff < 0) {
			thetaDiff = 2*Math.PI + thetaDiff;
		}
		//want the right angle
		//thetaDiff = (2*Math.PI-thetaDiff);
		/*if (thetaDiff < -Math.PI){
			thetaDiff += 2 * Math.PI;
		} else if (thetaDiff > Math.PI){
			thetaDiff -= 2 * Math.PI;
		}*/
		return thetaDiff;
		
	}
	
	private static double getPositiveAngle(Coord coord) {
		double angle = Math.atan2(coord.getY(), coord.getX());
		if (angle < 0) {
			angle = 2*Math.PI + angle;
		}
		return angle;
	}
	
	private static Coord getOutVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
		return new Coord(x, y);
	}
	
	private static Coord getInVector(Link link){
		double x = link.getFromNode().getCoord().getX() - link.getToNode().getCoord().getX();
		double y = link.getFromNode().getCoord().getY() - link.getToNode().getCoord().getY();
		return new Coord(x, y);
	}

}
