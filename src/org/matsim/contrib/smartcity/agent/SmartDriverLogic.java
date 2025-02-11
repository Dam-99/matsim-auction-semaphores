/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.lanes.Lane;

/**
 * Interface that define the method that can be called for using an agent driver logic.
 * 
 * @see AbstractDriverLogic
 * @author Filippo Muzzini
 *
 */
public interface SmartDriverLogic {

	/**
	 * Called after an action is endend in now time
	 * 
	 * @param now ending time
	 */
	void finalizeAction(double now);

	/**
	 * Called when agent change is positione
	 * 
	 * @param linkId new Link id
	 */
	void setActualLink(Id<Lane> linkId);

	/**
	 * Return the destination
	 * @return destination
	 */
	Id<Lane> getDestinationLinkId();

	/**
	 * Return the next link that agent must take.
	 * 
	 * @return next link
	 */
	Id<Lane> getNextLinkId();

	/**
	 * Set the leg
	 * 
	 * @param leg
	 */
	void setLeg(Leg leg);

	/**
	 * Return the excepted travel time
	 * @return travel time
	 */
	Double getTravelTime();

	/**
	 * Return the excepted travel distance
	 * @return travel distance
	 */
	Double getDistance();

	/**
	 * @param p
	 */
	void setPerson(Person p);

	/**
	 * @param agent
	 */
	void setAgent(MobsimDriverPassengerAgent agent);




}
