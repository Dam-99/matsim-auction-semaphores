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
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;

/**
 * .
 * Class that implement PassivePerceptionWrapperImpl method.
 * It's needed because if ActivePerceptionWrapperImpl extends directly from PassivePerceptionWrapperImpl that will cause 
 * a duplicated handler for the same event extended by PassivePerceptionWrapper
 * 
 * @author Sebastiano Manfredini
 * 
 *
 */
public class PerceptionWrapper {
	
	protected HashMap<Id<Lane>, LinkTrafficStatus> trafficMap;
	protected Map<Id<Vehicle>, Vehicle> vehicles;
	
	@Inject
	public PerceptionWrapper(Network network, Scenario scenario) {
		this.trafficMap = new HashMap<Id<Lane>, LinkTrafficStatus>();
		for (Id<Link> id : network.getLinks().keySet()) {
			String[] laneExtensions = {".l", ".s", ".r", ".ol"}; // TODO: check if it's not a problem for single lane (shouldn't)
			for(String lane : laneExtensions) {
				this.trafficMap.put(Id.create(id.toString() + lane, Lane.class), new LinkTrafficStatus());
			}
		}
		
		this.vehicles = scenario.getVehicles().getVehicles();
	}
	
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Link> idLink = event.getLinkId();
		Id<Vehicle> idVehicle = event.getVehicleId();
		vehicleEntered(idLink, idVehicle, event.getTime());
	}

	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Link> idLink = event.getLinkId();
		Id<Vehicle> idVehicle = event.getVehicleId();
		vehicleLeaved(idLink, idVehicle, event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> idLink = event.getLinkId();
		Id<Vehicle> idVehicle = event.getVehicleId();
		vehicleLeaved(idLink, idVehicle, event.getTime());
	}

	public void handleEvent(LinkEnterEvent event) {
		Id<Link> idLink = event.getLinkId();
		Id<Vehicle> idVehicle = event.getVehicleId();
		vehicleEntered(idLink, idVehicle, event.getTime());

	}
	
	public LinkTrafficStatus getLinkTrafficStatus(Id<Lane> idLink) {
		return this.trafficMap.get(idLink);
	}
	
	public int getTotalVehicleOnLink(Id<Lane> idLink) {
		LinkTrafficStatus linkStatus = this.trafficMap.get(idLink);
		return linkStatus.getTotal();
	}
	
	public int getTypeVehicleOnLink(Id<Lane> idLink, Id<VehicleType> idType) {
		LinkTrafficStatus linkStatus = this.trafficMap.get(idLink);
		return linkStatus.getTotalByType(idType);
	}
	
	protected void vehicleEntered(Id<Link> idLink, Id<Vehicle> vehicle, double time) {
		Id<VehicleType> idType = typeFromVehicle(vehicle);
		this.trafficMap.get(idLink).addVehicle(idType, time);
		
	}
	
	protected void vehicleLeaved(Id<Link> idLink, Id<Vehicle> vehicle, double time) throws VehicleLeftBeforeEnter {
		Id<VehicleType> idType = typeFromVehicle(vehicle); 
		try {
			this.trafficMap.get(idLink).subVehicle(idType, time);
		} catch (SubOnNull e) {
			throw new VehicleLeftBeforeEnter(vehicle, idLink);
		}
	}
	
	private Id<VehicleType> typeFromVehicle(Id<Vehicle> idVehicle) {
		Id<VehicleType> type = this.vehicles.get(idVehicle).getType().getId();
		return type;
	}

	
	public HashMap<Id<Lane>, LinkTrafficStatus> getTrafficMap() {
		return this.trafficMap;
	}
}
