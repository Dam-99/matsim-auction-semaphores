/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * Event created when there is a car accident
 * @author Filippo Muzzini
 *
 */
public class CarAccidentEvent extends Event {

	private String type;
	private Id<Link> fromId;
	private Id<Link> toId;
	private Id<Person> driver;
	private List<Id<Person>> others;

	/**
	 * 
	 * @param type Type of accident
	 * @param fromId Link from that agent come
	 * @param toId Link to that agent gone
	 * @param driver agent
	 * @param others other involved agents 
	 * @param time when the accident occurs
	 */
	public CarAccidentEvent(String type, Id<Link> fromId, Id<Link> toId, Id<Person> driver, List<Id<Person>> others, double time) {
		super(time);
		this.type = type;
		this.fromId = fromId;
		this.toId = toId;
		this.driver = driver;
		this.others = others;
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.Event#getEventType()
	 */
	@Override
	public String getEventType() {
		return type;
	}

	public Id<Link> getFromId() {
		return fromId;
	}

	public Id<Link> getToId() {
		return toId;
	}

	public Id<Person> getDriver() {
		return driver;
	}

	public List<Id<Person>> getOthers() {
		return others;
	}
	
	

}
