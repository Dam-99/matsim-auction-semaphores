package org.matsim.contrib.smartcity.perception.wrapper;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Inject;
import org.matsim.lanes.Lane;

/**
 * Implementation of an ActivePerceptionWrapper.
 * This implementation handle the event of MATSim and also update a data structure
 * like PassivePerceptionWrapperImpl, also notify to listeners that there is a change.
 * 
 * @author Filippo Muzzini
 *
 * @see ActivePerceptionWrapper
 * @see PassivePerceptionWrapperImpl
 *
 */
public class ActivePerceptionWrapperImpl extends PerceptionWrapper implements ActivePerceptionWrapper {
	
	private HashMap<Id<Lane>,ArrayList<LinkChangedListener>> listeners;
	/**
	 * Constructor of ActivePerceptionWrapper.
	 * It creates the data structure that represents the network and the vehicles
	 * 
	 * @param network MATSim network
	 * @param scenario MATSim scenario
	 */
	@Inject
	public ActivePerceptionWrapperImpl(Network network, Scenario scenario) {
		super(network, scenario);
		this.listeners = new HashMap<Id<Lane>, ArrayList<LinkChangedListener>>();
		for (Id<Link> link : network.getLinks().keySet()) {
			String[] laneExtensions = {".l", ".s", ".r", ".ol"};
			for (String lane : laneExtensions) {
				this.listeners.put(Id.create(link.toString() + lane, Lane.class), new ArrayList<LinkChangedListener>());
			}
		}
	}

	@Override
	public void addLinkChangedListener(LinkChangedListener listener, Id<Lane> linkId) {
		this.listeners.get(linkId).add(listener);
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Lane> idLink = Id.create(event.getLinkId().toString() + ".ol", Lane.class);
		super.handleEvent(event);
		linkChanged(idLink, event.getTime());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Lane> idLink = Id.create(event.getLinkId().toString() + ".ol", Lane.class);
		super.handleEvent(event);
		linkChanged(idLink, event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Lane> idLink = Id.create(event.getLinkId().toString() + ".ol", Lane.class);
		super.handleEvent(event);
		linkChanged(idLink, event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Lane> idLink = Id.create(event.getLinkId().toString() + ".ol", Lane.class);
		super.handleEvent(event);
		linkChanged(idLink, event.getTime());

	}

	private void linkChanged(Id<Lane> idLink, double time) {
		LinkTrafficStatus status = this.getLinkTrafficStatus(idLink);		
		for (LinkChangedListener listener : this.listeners.get(idLink))
			listener.publishLinkChanged(idLink, status, time);
		
	}

	@Override
	public void addLinkChangedListener(LinkChangedListener listener) {
		for (ArrayList<LinkChangedListener> l : this.listeners.values()) {
			l.add(listener);
			l.getClass();
		}
	}
	
	@Override
	public void addLinkChangedListener(LinkChangedListener listener, Iterable<Id<Lane>> links) {
		for (Id<Lane> link : links) {
			this.listeners.get(link).add(listener);
		}
	}

}
