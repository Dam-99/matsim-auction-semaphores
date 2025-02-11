/**
 * 
 */
package org.matsim.contrib.smartcity.comunication.wrapper;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.smartcity.agent.SmartAgentFactory;
import org.matsim.contrib.smartcity.agent.SmartDriverLogic;
import org.matsim.contrib.smartcity.comunication.ComunicationConfigGroup;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Wrapper for V2V comunications
 * @author Filippo Muzzini
 *
 */
public class ComunicationV2VWrapper implements ComunicationWrapper, LinkEnterEventHandler, LinkLeaveEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	@Inject private Network network;
	@Inject private Scenario scenario;
	
	private double range;
	private ConcurrentHashMap<Id<Vehicle>, Coord> vehicles = new ConcurrentHashMap<Id<Vehicle>, Coord>();
	private ConcurrentHashMap<Id<Vehicle>, ComunicationServer> servers = new ConcurrentHashMap<Id<Vehicle>, ComunicationServer>();

	@Inject
	public ComunicationV2VWrapper(Config config) {
		ComunicationConfigGroup configGroup = ConfigUtils.addOrGetModule(config, ComunicationConfigGroup.class);
		this.range = Double.parseDouble(configGroup.getParams().get(ComunicationConfigGroup.RANGE));
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationWrapper#discover(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Set<ComunicationServer> discover(Id<Lane> position) {
		Coord actualCoord = network.getLinks().get(position).getCoord();
		return vehicles.entrySet().stream().filter(
				e -> NetworkUtils.getEuclideanDistance(actualCoord, e.getValue()) <= range
				).map(e -> servers.get(e.getKey())).collect(Collectors.toSet());
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Vehicle> vehicle = event.getVehicleId();
		vehicles.remove(vehicle);
		servers.remove(vehicle);
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleEntersTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Vehicle> vehicle = event.getVehicleId();
		ComunicationServer server = getServerFromEvent(event);
		if (server != null) {
			Coord coord = getCoordFromEvent(event);
			vehicles.put(vehicle, coord);
			servers.put(vehicle, server);
		}		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Coord coord = getCoordFromEvent(event);
		Id<Vehicle> vehicle = event.getVehicleId();
		vehicles.put(vehicle, coord);
	}
	
	private ComunicationServer getServerFromEvent(HasPersonId event) {
		Id<Person> personId = event.getPersonId();
		Person person = scenario.getPopulation().getPersons().get(personId);
		SmartDriverLogic logic = (SmartDriverLogic) person.getCustomAttributes().get(SmartAgentFactory.DRIVERLOGICATT);
		if (logic instanceof ComunicationServer) {
			ComunicationServer server = (ComunicationServer) logic; 
			return server;
		}
		
		return null;
	}
	
	private Coord getCoordFromEvent(HasLinkId event) {
		return network.getLinks().get(event.getLinkId()).getCoord();
	}

}
