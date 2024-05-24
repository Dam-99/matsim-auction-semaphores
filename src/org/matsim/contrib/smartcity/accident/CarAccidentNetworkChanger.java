/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;

import com.google.inject.Inject;

/**
 * Class that reduce the link capacity after an accident.
 * Thought the day time the capacity is modelled.
 * @author Filippo Muzzini
 *
 */
public class CarAccidentNetworkChanger implements AccidentEventHandler,  MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {
	
	private static final double LIMITING_TIME = 3000;

	@Inject private Network network;
	private QSim sim;
	
	private ConcurrentHashMap<Link, LimitedLink> limitedLinks = new ConcurrentHashMap<Link, LimitedLink>();

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.accident.AccidentEventHandler#handleEvent(org.matsim.contrib.smartcity.accident.CarAccidentEvent)
	 */
	@Override
	public void handleEvent(CarAccidentEvent e) {
		int involvedCars = e.getOthers() != null ? e.getOthers().size() + 2 : 2 ;
		double startTime = e.getTime();
		List<Link> involvedLinks = new ArrayList<Link>();
		involvedLinks.add(network.getLinks().get(e.getFromId()));
		involvedLinks.add(network.getLinks().get(e.getToId()));
		
		for (Link link : involvedLinks) {
			if (link == null) {
				continue;
			}
			
			if (limitedLinks.containsKey(link)) {
				//LimitedLink limited = limitedLinks.get(link);
				//limited.startTime = startTime;
				//limited.involvedCars += involvedCars;
				return;
			} else {
				LimitedLink limitedLink = new LimitedLink(link, startTime, involvedCars);
				this.limitedLinks.put(link, limitedLink);
			}
		}
	}
	
	private void doSimStep(double now) {
		NetworkChangeEvent ev = new NetworkChangeEvent(now);
		boolean change = false;
		for (LimitedLink limited : limitedLinks.values()) {
			Link link = limited.getLink();
			double startTime = limited.getStartTime();
			int involvedCars = limited.getInvolvedCars();
			
			double actualCap = limited.actualCap;
			double capacityRatio = calcFlow(startTime, now, involvedCars);
			double newCapacity = limited.getStartCapacity() * capacityRatio;
			if (actualCap == newCapacity) {
				continue;
			}
			link.setCapacity(newCapacity);
			limited.actualCap = newCapacity;
			
			ev.addLink(link);	
			change = true;
			
			if (capacityRatio == 1) {
				limitedLinks.remove(link);
			}
		}
		
		if (change) {
			sim.addNetworkChangeEvent(ev);
		}
	}
	
	/**
	 * @param startTime
	 * @param now
	 * @param involvedCars
	 * @return
	 */
	private double calcFlow(double startTime, double now, int involvedCars) {
		double offset = now - startTime;
		return twoStepFunction(offset);
	}

	/**
	 * @param offset
	 * @return
	 */
	private double twoStepFunction(double x) {
		double x1 = 0.25 * LIMITING_TIME;
		double x2 = 0.75 * LIMITING_TIME;
		if (x < x1)
			return 0;
		if (x > x2)
			return 1;
		return 0.5;
	}

	/**
	 * Class that memorize the sate of a link
	 * @author Filippo Muzzini
	 *
	 */
	private class LimitedLink {
		
		private Link link;
		private double startTime;
		private int involvedCars;
		private double startCapacity;
		private double actualCap;
		
		/**
		 * @param link
		 * @param startTime
		 * @param involvedCars
		 */
		public LimitedLink(Link link, double startTime, int involvedCars) {
			this.link = link;
			this.startTime = startTime;
			this.involvedCars = involvedCars;
			
			this.startCapacity = link.getCapacity();
			this.actualCap = startCapacity;
		}

		public Link getLink() {
			return link;
		}

		public double getStartTime() {
			return startTime;
		}

		public int getInvolvedCars() {
			return involvedCars;
		}
		
		public double getStartCapacity() {
			return startCapacity;
		}
				
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener#notifyMobsimBeforeSimStep(org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		this.sim = (QSim) e.getQueueSimulation();
		double now = e.getSimulationTime();
		doSimStep(now);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener#notifyMobsimBeforeCleanup(org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		limitedLinks.clear();
	}
	
	public List<Link> getLimitedLinks() {
		return limitedLinks.keySet().stream().collect(Collectors.toList());
	}

}
