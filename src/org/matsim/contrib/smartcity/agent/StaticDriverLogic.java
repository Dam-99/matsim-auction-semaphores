/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.lanes.Lane;

/**
 * This class implements a static logic without smart behavior.
 * The agent with this logic take the planned road.
 * 
 * @author Filippo Muzzini
 *
 */
public class StaticDriverLogic extends AbstractDriverLogic {

	public static final String N_NODES_ATT = "signedCross";

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.AbstractDriverLogic#chooseNextLinkId()
	 */
	private List<Id<Link>> linksList;
	protected List<Id<Lane>> lanesList;
	private int index;
	private int indexLane;
	private int skipLaneIdxValue;
	protected boolean isDirectedLanes;
	protected long nNodes;

	@Inject
	private Scenario scenario;

	@Override
	public Id<Link> getNextLinkId() {
		if (this.actualLink == Id.create(this.getDestinationLinkId().toString().split("\\.")[0], Lane.class)) {
			return null;
		}

		if (this.index == this.linksList.size()) {
			//last link
			return Id.create(this.getDestinationLinkId(), Link.class);
		}

		return this.linksList.get(index);
	}

	/**
	 * @deprecated breaks at the end of the list
	 * @return
	 */
	public Id<Lane> getNextLaneId() {
		// FIXME: not sure about this case because of the sending the next lane
		if (this.actualLane == this.getDestinationLinkId()) {
			return null;
		}

		if (this.indexLane == this.lanesList.size()) {
			return this.getDestinationLinkId();
		}

		// FIXME: this breaks
		return this.lanesList.get(this.indexLane + 1);
	}
	
	@Override
	public void setLeg(Leg leg) {
		super.setLeg(leg);
		// might make this a field
		// assume .s exists -> others can too (and works at links with uturns)
		this.isDirectedLanes = scenario.getLanes().getLanesToLinkAssignments().values().stream()
                .flatMap(ltla -> ltla.getLanes().keySet().stream())
                .map(Id::toString).anyMatch(id -> id.contains(".s"));
		this.linksList = ((NetworkRoute) this.route).getLinkIds();
		this.index = 0;
		// LANELIST
		if(!this.isDirectedLanes) {
			this.lanesList = this.linksList.stream().map(linkId -> Id.create(linkId.toString() + ".ol", Lane.class))
					.collect(Collectors.toList());
			this.indexLane = this.index;
			this.skipLaneIdxValue = 1;
		}
		else {
			NetworkRoute networkRoute = (NetworkRoute) this.route;
			 ArrayList<Id<Link>> linksInRoute = new ArrayList<>(networkRoute.getLinkIds());
			linksInRoute.add(0, networkRoute.getStartLinkId());
			linksInRoute.add(networkRoute.getEndLinkId());
			ArrayList<Id<Lane>> lanesInRoute = new ArrayList<>(linksInRoute.size() * 2);
			ListIterator<Id<Link>> iter = linksInRoute.listIterator();
			while (iter.hasNext()) {
				String linkId = iter.next().toString();
				if (!iter.hasNext()) {
					lanesInRoute.add(Id.create(linkId + ".ol", Lane.class));
					break;
				}
				char linkDirection = linkId.charAt(0);
				int nextIdx = iter.nextIndex();
				String nextLinkId = linksInRoute.get(nextIdx).toString();
				char nextLinkDirection = nextLinkId.charAt(0);
				if (linkDirection == '5')
					linkDirection = '4';
				if (nextLinkDirection == '5')
					nextLinkDirection = '4';
				if (linkDirection == nextLinkDirection) { // straight
					lanesInRoute.add(Id.create(linkId + ".ol", Lane.class)); // idk if addAll works better and tbh idc
					lanesInRoute.add(Id.create(linkId + ".s", Lane.class));
				} else {
					String combo = String.valueOf(linkDirection) + "-" + String.valueOf(nextLinkDirection);
					switch (combo) {
						// left
						case "1-2":
						case "2-3":
						case "3-4":
						case "4-1":
							lanesInRoute.add(Id.create(linkId + ".ol", Lane.class));
							lanesInRoute.add(Id.create(linkId + ".l", Lane.class));
							break;
						// right
						case "1-4":
						case "2-1":
						case "3-2":
						case "4-3":
							lanesInRoute.add(Id.create(linkId + ".ol", Lane.class));
							lanesInRoute.add(Id.create(linkId + ".r", Lane.class));
							break;
						// u turns in links at end of network
						case "1-3":
						case "3-1":
						case "2-4":
						case "4-2":
							lanesInRoute.add(Id.create(linkId + ".ol", Lane.class));
							lanesInRoute.add(Id.create(linkId + ".s", Lane.class));
							break;
						default:
							// TODO: find better error type
							throw new RuntimeException("Unsupported turn: " + combo);
					}

				}
			}
			// TODO: dunno which one is better
			// this.lanesList = lanesInRoute;
			this.lanesList = lanesInRoute.subList(1, lanesInRoute.size()-1);
			this.indexLane = 0;
			this.skipLaneIdxValue = 2;
		}

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		List<Id<Link>> signalLinks = signalsData.getSignalSystemsData().getSignalSystemData().values().stream()
				.map(SignalSystemData::getSignalData).flatMap(s -> s.values().stream())
				.map(SignalData::getLinkId).collect(Collectors.toList());

		// TODO: (maybe?) check if the attribute is set correct
		this.nNodes = Stream.concat(this.getLinksList().stream(), Stream.of(this.agent.getCurrentLinkId()))
				.filter(signalLinks::contains).count();
		this.getPerson().getAttributes().putAttribute(N_NODES_ATT, nNodes);
	}
	
	protected List<Id<Lane>> getLinksList() {
		return this.linksList.stream().map(linkId -> Id.create(linkId.toString() + ".ol", Lane.class)).collect(Collectors.toList());
	}

	protected List<Id<Lane>> getLanesList() {
		return this.lanesList;
	}

	protected void setLinksList(List<Id<Link>> linksList) {
		this.linksList = linksList;
	}
	
	protected int getActualIndex() {
		return this.index;
	}

	protected int getActualIndexLane() {
		return this.indexLane;
	}
	
	@Override
	public void setActualLink(Id<Lane> actualLink) {
		super.setActualLink(actualLink);
		int newIndex = this.linksList.indexOf(actualLink);
		if (newIndex == -1) {
			//siamo fuori strada, cosa fare?
		}
		
		this.index = newIndex+1;
	}

	public void setActualLane(Id<Lane> actualLane) {
		super.setActualLane(actualLane);
		// FIXME: i don't think this works because matsim doesn't call this method
		int newIndex = this.lanesList.indexOf(actualLane);
		if (newIndex == -1) {
			// idk
		}

		this.indexLane = newIndex+1;
	}
	

}
