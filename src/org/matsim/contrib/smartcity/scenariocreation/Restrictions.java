/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmRelation;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.smartcity.restriction.NetworkWithRestrictionTurnInfoBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Container that extract the restriction from osm storage
 * @author Filippo Muzzini
 *
 */
public class Restrictions {

	private static final String RESTRICTION_TAG = "restriction";
	private static final String FROM_ROLE = "from";
	private static final String TO_ROLE = "to";
	private static final String VIA_ROLE = "via";
	private static final String DEFAULT_OUTPUT_DIR = ".";
	private static final String DEFAULT_OUTPUT_NAME = "NetworkWithRestriction";
	private static final String ONLY_PREFIX = "only";
	private static final String NO_PREFIX = "no";
	private static final String JUNCTION_TAG = "junction";
	private static final String ROUNDABOUND = "roundabout";
	
	private MemoryStorage storage;
	private Network network;
	private Set<IOsmWay> rounds = new HashSet<IOsmWay>();
	private HashMap<Long, List<IOsmWay>> fromNode = new HashMap<Long, List<IOsmWay>>();
	private HashMap<IOsmWay, Long> toNode = new HashMap<IOsmWay, Long>();
	private short junctionTag;

	public Restrictions(MemoryStorage storage, Network network) {
		this.storage = storage;
		this.network = network;
		process();
	}

	/**
	 * 
	 */
	private void process() {
		junctionTag = storage.getTagsPack().getTagCode(JUNCTION_TAG);
		storage.byTag(RESTRICTION_TAG, o -> o.isRelation(), o -> processRestriction(o));
		storage.all(o -> o.isWay(), o -> addWay(o));
		storage.byTag(JUNCTION_TAG, o -> o.getTag(junctionTag).equals(ROUNDABOUND), o -> addRound(o));
		processRounds();
	}

	/**
	 * @param o
	 * @return
	 */
	private void addWay(IOsmObject o) {
		IOsmWay way = (IOsmWay) o;
		long[] nodes = way.getNodeIds();
		long fromNode = nodes[0];
		long toNode = nodes[nodes.length-1];
		
		List<IOsmWay> fromWays = this.fromNode.get(fromNode);
		if (fromWays == null) {
			fromWays = new ArrayList<IOsmWay>();
		}
		fromWays.add(way);
		
		this.fromNode.put(fromNode, fromWays);
		this.toNode.put(way, toNode);
	}

	/**
	 * 
	 */
	private void processRounds() {
		for (IOsmWay way : this.rounds) {
			Set<Long> thisRound = getThisRound(way);
			List<IOsmWay> proxs = thisRound.stream().flatMap(n -> {
				List<IOsmWay> ways = this.fromNode.get(n);
				if (ways != null) {
					return ways.stream();
				} else {
					return null;
				}
				}).collect(Collectors.toList());
			proxs.removeAll(this.rounds);
			for (IOsmWay nl : proxs) {
				if (thisRound.contains(this.toNode.get(nl))) {
					specialCaseSplit(nl);
					continue;
				}
				List<IOsmWay> nextLinks = this.fromNode.get(this.toNode.get(nl));
				if (nextLinks == null) {
					continue;
				}
				for (IOsmWay roundEntrance : nextLinks) {
					Long lastNode = this.toNode.get(roundEntrance);
					//List<IOsmWay> possibleRounsAgain = this.fromNode.get(this.toNode.get(roundEntrance));
					if (thisRound.contains(lastNode)){
						setRoundProibition(nl, roundEntrance);
					}
				}
			}
		}
	}

	/**
	 * @param nl
	 */
	private void specialCaseSplit(IOsmWay nl) {
		Id<Node> firstNodeId = Id.createNodeId(nl.getNodeIds()[0]);
		Id<Node> mediumNodeId = Id.createNodeId(nl.getNodeIds()[1]);
		Id<Node> lastNodeId = Id.createNodeId(nl.getNodeIds()[2]);
		Node firstNode = network.getNodes().get(firstNodeId);
		Node mediumNode = network.getNodes().get(mediumNodeId);
		Node lastNode = network.getNodes().get(lastNodeId);
		
		Link firstLink = NetworkUtils.getConnectingLink(firstNode, mediumNode);
		Link lastLink = NetworkUtils.getConnectingLink(mediumNode, lastNode);
		
		setAttributeRestriction(firstLink.getId(), lastLink.getId());
	}

	/**
	 * @param nl
	 * @param roundEntrance
	 */
	private void setRoundProibition(IOsmWay from, IOsmWay to) {
		long nodeId = from.getNodeIds()[from.getNodeIds().length-1];
		Node matsimNode = network.getNodes().get(Id.createNodeId(nodeId));
		Map<Id<Link>, ? extends Link> inLinks = matsimNode.getInLinks();
		Map<Id<Link>, ? extends Link> outLinks = matsimNode.getOutLinks();
		Set<Id<Link>> fromLinks = getWayLinks(from); 
		Set<Id<Link>> toLinks = getWayLinks(to);
		
		fromLinks.retainAll(inLinks.keySet());
		toLinks.retainAll(outLinks.keySet());
		
		if(fromLinks.size()==0 || toLinks.size()==0) {
			return;
		}
		Id<Link> fromLink = fromLinks.iterator().next();
		Id<Link> toLink = toLinks.iterator().next();
		
		setAttributeRestriction(fromLink, toLink);
	}

	/**
	 * @param way
	 * @return
	 */
	private Set<Long> getThisRound(IOsmWay way) {
		IOsmWay tmpWay = way;
		Set<IOsmWay> res = new HashSet<IOsmWay>();
		do {
			List<IOsmWay> nextWays = this.fromNode.get(this.toNode.get(tmpWay));
			if (nextWays != null){
				List<IOsmWay> nextWays_ = nextWays.stream().filter(w -> {
					String jun = w.getTag(this.junctionTag);
					return jun != null && jun.equals(ROUNDABOUND);
				}).collect(Collectors.toList());
				IOsmWay nextWay = nextWays_.get(0);
				tmpWay = nextWay;
				res.add(nextWay);
			} else {
				break;
			}
		} while (!way.equals(tmpWay));
		
		Set<Long> nodes = new HashSet<Long>();
		for (IOsmWay w : res) {
			long[] wayNodes = w.getNodeIds();
			for (long node : wayNodes) {
				nodes.add(node);
			}
		}
		return nodes;
	}

	/**
	 * @param o
	 * @return
	 */
	private void addRound(IOsmObject o) {
		IOsmWay way = (IOsmWay) o;
		this.rounds.add(way);	
	}

	/**
	 * @param o
	 * @return
	 */
	private void processRestriction(IOsmObject o) {
		IOsmRelation r = (IOsmRelation) o;
		int n = r.getMembersCount();
		IOsmWay from = null;
		IOsmWay to = null;
		IOsmNode node = null;
		for (int i = 0; i<n; i++) {
			String memberRole = r.getMemberRole(storage, i);
			IOsmWay way = null;
			IOsmNode node_ = null;
			if (r.getMemberObject(storage, i) == null) {
				continue;
			}
			if (r.getMemberObject(storage, i).isWay()) {
				way = (IOsmWay) r.getMemberObject(storage, i);
			} else if (r.getMemberObject(storage, i).isNode()) {
				node_ = (IOsmNode) r.getMemberObject(storage, i);
			} else {
				continue;
			}
			if (memberRole.equals(FROM_ROLE)) {
				from = way;
			}
			
			if (memberRole.equals(TO_ROLE)){
				to = way;
			}
			
			if (memberRole.equals(VIA_ROLE)) {
				node = node_;
			}
		}
		
		if(from == null || to == null || node == null) {
			System.out.println("DIVIETO OMESSO PERCHE' NON PRESENTI INFORMAZIONI NECESSARIE!!! ("+r.getId()+")");
			return;
		}

		Node matsimNode = network.getNodes().get(Id.createNodeId(node.getId()));
		if (matsimNode == null)
			return;

		Map<Id<Link>, ? extends Link> inLinks = matsimNode.getInLinks();
		Map<Id<Link>, ? extends Link> outLinks = matsimNode.getOutLinks();
		Set<Id<Link>> fromLinks = getWayLinks(from); 
		Set<Id<Link>> toLinks = getWayLinks(to);
		
		fromLinks.retainAll(inLinks.keySet());
		toLinks.retainAll(outLinks.keySet());
		
		Id<Link> fromLink;
		Id<Link> toLink;
		try {
			fromLink = fromLinks.iterator().next();
			toLink = toLinks.iterator().next();
		} catch (RuntimeException e) {
			return;
		}
		
		String restrictionType = r.getTag(RESTRICTION_TAG, storage);
		if (restrictionType.startsWith(NO_PREFIX)) {
			setAttributeRestriction(fromLink, toLink);
		} else if (restrictionType.startsWith(ONLY_PREFIX)) {
			HashMap<Id<Link>, Link> outL = new HashMap<Id<Link>, Link>(outLinks);
			outL.remove(toLink);
			for (Id<Link> l : outL.keySet()) {
				setAttributeRestriction(fromLink, l);
			}
		}
		
	}

	/**
	 * @param fromLink
	 * @param toLink
	 */
	private void setAttributeRestriction(Id<Link> fromLink, Id<Link> toLink) {
		Link from = network.getLinks().get(fromLink);
		String restrs = (String) from.getAttributes().getAttribute(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_ATT);
		List<String> restSplit = null;
		if (restrs != null) {
			restSplit = Arrays.asList(restrs.split(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_SEP));

		}
		if (restSplit != null && restSplit.contains(toLink.toString())) {
			return;
		}
		
		restrs = restrs != null ? restrs+NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_SEP : "";
		restrs = restrs + toLink;
		from.getAttributes().putAttribute(NetworkWithRestrictionTurnInfoBuilder.RESTRICTION_ATT, restrs);
	}

	/**
	 * @param from
	 * @return
	 */
	private Set<Id<Link>> getWayLinks(IOsmWay wayId) {
		if (wayId == null) {
			return new HashSet<Id<Link>>();
		}
		String wayIdStr = new Long(wayId.getId()).toString();
		return network.getLinks().entrySet().stream().filter(
				e -> e.getValue().getAttributes().getAttribute(NetworkUtils.ORIGID).equals(wayIdStr)).map(
						e -> e.getKey()).collect(Collectors.toSet());
	}

	/**
	 * @param outputFile 
	 * 
	 */
	public void write(String outputFile) {
		if (outputFile == null) {
			outputFile = DEFAULT_OUTPUT_DIR + File.separator + DEFAULT_OUTPUT_NAME;
		}
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(outputFile);
		
	}

}
