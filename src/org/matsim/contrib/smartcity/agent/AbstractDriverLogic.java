/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;

/**
 * Abstract class that implements a standard behavior of SmartDriverLogic.
 * This simplify the implementations of SmartDriverLogic interface.
 * 
 * The only abstract method is chooseNextLinkId; implement in this method the
 * smart behavior of agent.
 * 
 * Furthermore is possible overriding all methods of SmartDriverLogic interface. 
 * 
 * @author Filippo Muzzini
 *
 */
public abstract class AbstractDriverLogic implements SmartDriverLogic {

	protected Leg leg;
	protected Id<Link> actualLink;
	protected Id<Link> startLink;
	protected Id<Link> endLink;
	protected Route route;
	protected MobsimDriverPassengerAgent agent;
	protected Person person;

	@Override
	public abstract Id<Link> getNextLinkId();

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.SmartDriverLogic#finalizeAction(double)
	 */
	@Override
	public void finalizeAction(double now) {		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.SmartDriverLogic#setActualLink(org.matsim.api.core.v01.Id)
	 */
	@Override
	public void setActualLink(Id<Link> linkId) {
		this.actualLink = linkId;		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.SmartDriverLogic#getDestinationLinkId()
	 */
	@Override
	public Id<Link> getDestinationLinkId() {
		return this.endLink;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.agent.SmartDriverLogic#setLeg(org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public void setLeg(Leg leg) {
		this.leg = leg;
		this.route = leg.getRoute();
		this.startLink = route.getStartLinkId();
		this.endLink = route.getEndLinkId();
	}
	
	@Override
	public Double getTravelTime() {
		return this.route.getTravelTime();
	}
	
	@Override
	public Double getDistance() {
		return this.route.getDistance();
	}
	
	@Override
	public void setAgent(MobsimDriverPassengerAgent agent) {
		this.agent = agent;
	}
	
	@Override
	public void setPerson(Person p) {
		this.person = p;
	}

	public Person getPerson() {
		return person;
	}
}
