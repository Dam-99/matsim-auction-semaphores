/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.OsmWay;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.PriorityTurnAcceptanceLogic;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Containers that extract the priority from osm storage
 * @author Filippo Muzzini
 *
 */
public class Priorities {	

	private static final String HIGHWAY_KEY = "highway";
	private static final String STOP = "stop";
	private static final String GIVE_WAY = "give_way";
	private static final String DIRECTION_TAG = "direction";
	private static final String FORWARD = "forward";
	private static final String PRIORITY_TAG = "priority_road";
	private static final String PRIORITY_FORWARD_TAG = "priority_road:forward";
	private static final String PRIORITY_BACKWARD_TAG = "priority_road:backward";
	private static final String PRIORITY_END = "end";
	private static final String DEFAULT_OUTPUT_DIR = ".";
	private static final String DEFAULT_OUTPUT_NAME = "networkWithPriority.xml";
	private static final String JUNCTION_TAG = "junction";
	private static final Object ROUNDABOUT = "roundabout";
	
	
	private MemoryStorage storage;
	private Network network;
	private short highway_tag;
	private short prority_tag;
	private short priority_forward_tag;
	private short priority_backward_tag;
	private short junction_tag;


	public Priorities(MemoryStorage storage, Network network) {
		this.storage = storage;
		this.network = network;
		this.highway_tag = storage.getTagsPack().getTagCode(HIGHWAY_KEY);
		this.prority_tag = storage.getTagsPack().getTagCode(PRIORITY_TAG);
		this.junction_tag = storage.getTagsPack().getTagCode(JUNCTION_TAG);
		this.priority_backward_tag = storage.getTagsPack().getTagCode(PRIORITY_BACKWARD_TAG);
		this.priority_forward_tag = storage.getTagsPack().getTagCode(PRIORITY_FORWARD_TAG);
		processStopAndGiveWay();
	}
	
	public void write() {
		String filename = DEFAULT_OUTPUT_DIR + File.separator + DEFAULT_OUTPUT_NAME;
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(filename);
	}

	/**
	 * 
	 */
	private void processStopAndGiveWay() {
		storage.byTag(HIGHWAY_KEY, o -> o.isWay(),
				o -> processWay(o));	
		
	}

	/**
	 * @param o
	 * @return
	 */
	private void processWay(IOsmObject o) {
		OsmWay way = (OsmWay) o;
		long[] nodesId = way.getNodeIds();
		boolean stop = false;
		String direction = "";
		for (long id : nodesId) {
			IOsmNode osmNode = storage.getNodeById(id);
			String priority = osmNode.extractTags(storage).get(HIGHWAY_KEY);
			if (priority != null && (priority.equals(GIVE_WAY) || priority.equals(STOP))) {
				stop = true;
				direction = osmNode.extractTags(storage).get(DIRECTION_TAG);
				if (direction == null) {
					direction = FORWARD;
				}
				break;
			}
		}
		
		if (stop) {
			long lastNodeId;
			if (direction.equals(FORWARD)) {
				lastNodeId = nodesId[nodesId.length-1];
			} else {
				lastNodeId = nodesId[0];
			}
			
			createGiveWay(way.getId(), lastNodeId);
		}
		
		List<Id<Link>> links = getWayLinks(way.getId());
		
		//priority way
		String priority = way.getTag(prority_tag);
		String junction = way.getTag(junction_tag);
		if ((priority != null && !priority.equals(PRIORITY_END))
				|| (junction !=null && junction.equals(ROUNDABOUT))) {
			setAttributeToLinks(links, PriorityTurnAcceptanceLogic.PRIORITY_ATT, PriorityTurnAcceptanceLogic.PRIORITY_ROAD);
		}
		
		String priority_forward = way.getTag(priority_forward_tag);
		List<Id<Link>> forwardLinks = getForwardLinks(links);
		if (priority_forward != null && !priority_forward.equals(PRIORITY_END)) {
			setAttributeToLinks(forwardLinks, PriorityTurnAcceptanceLogic.PRIORITY_ATT, PriorityTurnAcceptanceLogic.PRIORITY_ROAD);
		}
		
		String priority_backward = way.getTag(priority_backward_tag);
		List<Id<Link>> backwardLinks = getBackwardLinks(links);
		if (priority_backward != null && !priority_backward.equals(PRIORITY_END)) {
			setAttributeToLinks(backwardLinks, PriorityTurnAcceptanceLogic.PRIORITY_ATT, PriorityTurnAcceptanceLogic.PRIORITY_ROAD);
		}

		//way type
		String type = way.getTag(highway_tag);
		setAttributeToLinks(links, PriorityTurnAcceptanceLogic.WAY_TYPE, type);
	}

	/**
	 * @param links
	 * @param wayType
	 * @param type
	 */
	private void setAttributeToLinks(List<Id<Link>> linksId, String att, String value) {
		for (Id<Link> linkId : linksId) {
			Link link = network.getLinks().get(linkId);
			link.getAttributes().putAttribute(att, value);
		}
	}

	/**
	 * @param way
	 * @return
	 */
	private List<Id<Link>> getWayLinks(long wayId) {
		String wayIdStr = new Long(wayId).toString();
		return network.getLinks().entrySet().stream().filter(
				e -> e.getValue().getAttributes().getAttribute(NetworkUtils.ORIGID).equals(wayIdStr)).map(
						e -> e.getKey()).collect(Collectors.toList());
	}
	
	/**
	 * @param links
	 * @return
	 */
	private List<Id<Link>> getBackwardLinks(List<Id<Link>> linksId) {
		Stream<Link> links = linksId.stream().map(id -> network.getLinks().get(id));
		return links.filter(l -> l.getAttributes().getAttribute(OsmNetworkReaderWithReverse.DIRECTION_ATT).equals(OsmNetworkReaderWithReverse.BACKWARD)).
				map(l -> l.getId()).collect(Collectors.toList());
	}
	
	/**
	 * @param links
	 * @return
	 */
	private List<Id<Link>> getForwardLinks(List<Id<Link>> linksId) {
		Stream<Link> links = linksId.stream().map(id -> network.getLinks().get(id));
		return links.filter(l -> l.getAttributes().getAttribute(OsmNetworkReaderWithReverse.DIRECTION_ATT).equals(OsmNetworkReaderWithReverse.FORWARD)).
				map(l -> l.getId()).collect(Collectors.toList());
	}
		

	/**
	 * @param o 
	 * @param id
	 * @return
	 */
	private void createGiveWay(long wayId, long nodeId) {
		Id<Node> matsimNodeId = Id.createNodeId(nodeId);
		List<Id<Link>> matsimLinkId = getWayLinks(wayId);
		HashMap<Id<Node>, List<Id<Link>>> finalNode = new HashMap<Id<Node>, List<Id<Link>>>();
		for (Id<Link> linkId : matsimLinkId) {
			Link link = network.getLinks().get(linkId);
			Node node = link.getToNode();
			Id<Node> nodeId_ = node.getId();
			List<Id<Link>> links = new ArrayList<Id<Link>>();
			if (finalNode.containsKey(nodeId_)) {
				links = finalNode.get(nodeId_);
			}
			
			links.add(linkId);
			finalNode.put(node.getId(), links);
		}
		
		List<Id<Link>> links = finalNode.get(matsimNodeId);
		setAttributeToLinks(links, PriorityTurnAcceptanceLogic.PRIORITY_ATT, PriorityTurnAcceptanceLogic.GIVE_WAY);
				
	}

}
