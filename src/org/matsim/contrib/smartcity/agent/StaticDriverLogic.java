/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.List;
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
	private List<Id<Lane>> linksList;
	private int index;
	protected long nNodes;

	@Inject
	private Scenario scenario;

	@Override
	public Id<Lane> getNextLinkId() {
		if (this.actualLink == this.getDestinationLinkId()) {
			return null;
		}
		
		if (this.index == this.linksList.size()) {
			//last link
			return this.getDestinationLinkId();
		}
		
		return this.linksList.get(index);
	}
	
	@Override
	public void setLeg(Leg leg) {
		super.setLeg(leg);
		this.linksList = ((NetworkRoute) this.route).getLinkIds().stream().map(id -> Id.create(id, Lane.class)).collect(Collectors.toList());
		this.index = 0;

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		List<Id<Link>> signalLinks = signalsData.getSignalSystemsData().getSignalSystemData().values().stream()
				.map(SignalSystemData::getSignalData).flatMap(s -> s.values().stream())
				.map(SignalData::getLinkId).collect(Collectors.toList());

		this.nNodes = Stream.concat(this.getLinksList().stream(), Stream.of(this.agent.getCurrentLinkId()))
				.filter(signalLinks::contains).count();
		this.getPerson().getAttributes().putAttribute(N_NODES_ATT, nNodes);
	}
	
	protected List<Id<Lane>> getLinksList() {
		return this.linksList;
	}
	
	protected void setLinksList(List<Id<Lane>> linksList) {
		this.linksList = linksList;
	}
	
	protected int getActualIndex() {
		return this.index;
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

	

}
