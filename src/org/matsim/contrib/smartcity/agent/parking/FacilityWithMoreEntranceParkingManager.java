/**
 * 
 */
package org.matsim.contrib.smartcity.agent.parking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Manager for parking with more link entrance
 * @author Filippo Muzzini
 *
 */
public class FacilityWithMoreEntranceParkingManager extends FacilityBasedParkingManager {

	private static final String LINKS_ATT = "links";
	private static final String LINKS_SEPARATOR = ";";

	/**
	 * @param scenario
	 */
	@Inject
	public FacilityWithMoreEntranceParkingManager(Scenario scenario) {
		super(scenario);
		parkingFacilities = scenario.getActivityFacilities()
				.getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE);
		Logger.getLogger(getClass()).info(parkingFacilities);

		for (ActivityFacility fac : this.parkingFacilities.values()) {
			String linksString = (String) fac.getAttributes().getAttribute(LINKS_ATT);
			String[] linksArray = linksString.split(LINKS_SEPARATOR);
			Set<Id<Link>> linksId = Arrays.stream(linksArray).map(Id::createLinkId).collect(Collectors.toSet());
			for (Id<Link> linkId : linksId) {
				Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
				if (this.facilitiesPerLink.containsKey(linkId)) {
					parkingOnLink = this.facilitiesPerLink.get(linkId);
				}
				parkingOnLink.add(fac.getId());
				this.facilitiesPerLink.put(linkId, parkingOnLink);
			}
			this.occupation.put(fac.getId(), new MutableLong(0));

		}
	}
	
	@Override
	public boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		boolean canPark = false;

		if (linkIdHasAvailableParkingForVehicle(linkId, vehicleId)) {
			canPark = true;
			// Logger.getLogger(getClass()).info("veh: "+vehicleId+" link
			// "+linkId + " can park "+canPark);
		}

		return canPark;
	}

	private boolean linkIdHasAvailableParkingForVehicle(Id<Link> linkId, Id<Vehicle> vid) {
		// Logger.getLogger(getClass()).info("link "+linkId+" vehicle "+vid);
		if (!this.facilitiesPerLink.containsKey(linkId)) {
			// no parking -> cannot park here
			return false;
		}
		Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
		for (Id<ActivityFacility> fac : parkingFacilitiesAtLink) {
			double cap = this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE)
					.getCapacity();
			if (this.occupation.get(fac).doubleValue() < cap) {
				// Logger.getLogger(getClass()).info("occ:
				// "+this.occupation.get(fac).toString()+" cap: "+cap);
				this.occupation.get(fac).increment();
				this.parkingReservation.put(vid, fac);

				return true;
			}
		}
		return false;
	}

	/**
	 * @param parking
	 * @return
	 */
	public double getNrOfFreeParkingSpaces(Id<ActivityFacility> parking) {
		double cap = this.parkingFacilities.get(parking).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE)
				.getCapacity();
		double occupation = this.occupation.get(parking).doubleValue();
		
		return cap - occupation;
	}
	
	@Override
	public List<String> produceStatistics() {
		List<String> stats = new ArrayList<>();
		for (Entry<Id<ActivityFacility>, MutableLong> e : this.occupation.entrySet()) {
			double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
					.get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
			String s = e.getKey().toString() + ";" + capacity + ";" + e.getValue().toString();
			stats.add(s);
		}
		return stats;
	}

}
