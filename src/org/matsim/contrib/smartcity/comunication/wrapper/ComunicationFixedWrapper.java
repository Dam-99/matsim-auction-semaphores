/**
 * 
 */
package org.matsim.contrib.smartcity.comunication.wrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.ComunicationConfigGroup;
import org.matsim.contrib.smartcity.comunication.ComunicationMessage;
import org.matsim.contrib.smartcity.comunication.ComunicationServer;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Wrapper for systems with fixed appendices
 * @author Filippo Muzzini
 *
 */
public class ComunicationFixedWrapper implements ComunicationWrapper, LinkEnterEventHandler, LinkLeaveEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	@Inject private Network network;
	
	private double range;
	private HashMap<Coord, Set<ComunicationServer>> fixed = new HashMap<Coord, Set<ComunicationServer>>();
	private HashSet<ComunicationClient> reachable = new HashSet<ComunicationClient>();
	private HashMap<Id<Vehicle>, ComunicationClient> vehToClient = new HashMap<Id<Vehicle>, ComunicationClient>();
	private Scenario scenario;
	
	@Inject
	public ComunicationFixedWrapper(Config config, Scenario scenario) {
		ComunicationConfigGroup configGroup = ConfigUtils.addOrGetModule(config, ComunicationConfigGroup.class);
		this.range = Double.parseDouble(configGroup.getParams().get(ComunicationConfigGroup.RANGE));
		this.scenario = scenario;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationWrapper#discover(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Set<ComunicationServer> discover(Id<Link> position) {
		Link actualLink = network.getLinks().get(position);
		return fixed.entrySet().stream().filter(
				c -> NetworkUtils.getEuclideanDistance(actualLink.getCoord(), c.getKey()) <= range 
				).map(e -> e.getValue()).flatMap(s -> s.stream()).collect(Collectors.toSet());
		
	}
	
	public void broadcast(ComunicationMessage message) {
		synchronized (reachable) {
			reachable.stream().forEach(c -> c.sendToMe(message));
		}
	}
	
	/**
	 * Add server with his appendices
	 * @param server Server
	 * @param positions set of appendices' position
	 */
	public void addFixedComunicator(ComunicationServer server, Set<Coord> positions) {
		for (Coord c : positions) {
			Set<ComunicationServer> serverSet = fixed.get(c);
			if (serverSet == null)
				serverSet = new HashSet<ComunicationServer>();
			serverSet.add(server);
			fixed.put(c, serverSet);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		ComunicationClient client = getClientFromEvent(event);
		synchronized (reachable) {
			this.reachable.remove(client);
		}
		this.vehToClient.remove(event.getVehicleId());
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler#handleEvent(org.matsim.api.core.v01.events.VehicleEntersTrafficEvent)
	 */
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		ComunicationClient client = getClientFromEvent(event);
		if (client != null) {
			Coord coord = getCoordFromEvent(event);
			vehToClient.put(event.getVehicleId(), client);
			boolean covered = false;
			for (Coord c : fixed.keySet()) {
				if (NetworkUtils.getEuclideanDistance(coord, c) <= range) {
					covered = true;
					break;
				}
			}
			if (covered == true) {
				synchronized (reachable) {
					this.reachable.add(client);
				}
			}
		}				
	}

	/**
	 * @param event
	 * @return
	 */
	private ComunicationClient getClientFromEvent(HasPersonId event) {
		Id<Person> personId = event.getPersonId();
		Person person = scenario.getPopulation().getPersons().get(personId);
		SmartDriverLogic logic = (SmartDriverLogic) person.getCustomAttributes().get(SmartAgentFactory.DRIVERLOGICATT);
		if (logic instanceof ComunicationClient) {
			ComunicationClient client = (ComunicationClient) logic; 
			return client;
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		event.getVehicleId();
		ComunicationClient client = getClientFromVehicle(event.getVehicleId());
		if (client != null) {
			Coord coord = getCoordFromEvent(event);
			boolean covered = false;
			for (Coord c : fixed.keySet()) {
				if (NetworkUtils.getEuclideanDistance(coord, c) <= range) {
					covered = true;
					break;
				}
			}
			if (covered == true && !this.reachable.contains(client)) {
				synchronized (reachable) {
					this.reachable.add(client);
				}
			} else if (covered == false && this.reachable.contains(client)) {
				synchronized (reachable) {
					this.reachable.remove(client);
				}
			}
		}				
		
	}
	
	/**
	 * @param vehicleId
	 * @return
	 */
	private ComunicationClient getClientFromVehicle(Id<Vehicle> vehicleId) {
		return vehToClient.get(vehicleId);
	}

	private Coord getCoordFromEvent(HasLinkId event) {
		return network.getLinks().get(event.getLinkId()).getCoord();
	}

}
