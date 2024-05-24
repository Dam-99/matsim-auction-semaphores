/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.smartcity.perception.TrafficFlow;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * @author Filippo Muzzini
 *
 */
public class TravelDisutilityWithFlow implements TravelDisutility {

	private TrafficFlow flow;
	private TravelDisutility delegate;

	/**
	 * @param baseTravel
	 * @param trafficFlow
	 */
	public TravelDisutilityWithFlow(TravelDisutility baseTravel, TrafficFlow trafficFlow) {
		this.delegate = baseTravel;
		this.flow = trafficFlow;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.router.util.TravelDisutility#getLinkTravelDisutility(org.matsim.api.core.v01.network.Link, double, org.matsim.api.core.v01.population.Person, org.matsim.vehicles.Vehicle)
	 */
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double baseCost = this.delegate.getLinkTravelDisutility(link, time, person, vehicle);
		Double flow = this.flow.getFlow(link.getId());
		if (flow == null) {
			return baseCost;
		} else {
			return baseCost * flow;
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.router.util.TravelDisutility#getLinkMinimumTravelDisutility(org.matsim.api.core.v01.network.Link)
	 */
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}

	/**
	 * @param flow
	 */
	public void setFlow(TrafficFlow flow) {
		this.flow = flow;
		
	}

}
