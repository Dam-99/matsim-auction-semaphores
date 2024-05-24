package org.matsim.contrib.smartcity.perception.wrapper;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;

/**
 * Implementation of an PassivePerceptionWrapper.
 * This implementation handle the event of MATSim and update a data structure.
 * 
 * @author Filippo Muzzini
 * 
 * @see PassivePerceptionWrapper
 *
 */
public class PassivePerceptionWrapperImpl extends PerceptionWrapper implements PassivePerceptionWrapper {
	
	/**
	 * Constructor of PassivePerceptionWrapper.
	 * It creates the data structure that represents the network and the vehicles
	 * 
	 * @param network MATSim network
	 * @param scenario MATSim scenario
	 */
	@Inject
	public PassivePerceptionWrapperImpl(Network network, Scenario scenario) {
		super(network,scenario);
	}
	

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		super.handleEvent(event);

	}

}
