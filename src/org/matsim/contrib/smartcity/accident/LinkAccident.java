/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class LinkAccident implements MobsimBeforeSimStepListener {

	private final static double ACCIDENT_IN_YEAR = 191146;
	private final static double SECONDS_IN_YEAR = 3600*24*365;
	private final static double LAMBDA = ACCIDENT_IN_YEAR / SECONDS_IN_YEAR;
	
	@Inject private EventsManager events;
	@Inject private CarAccidentNetworkChanger changer;
	private QNetwork qNetwork;
	//private PoissonDistribution dist = new PoissonDistribution(LAMBDA);
	//private BinomialDistribution bin = new BernoulliDistribution(LAMBDA / SECONDS_IN_YEAR);
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener#notifyMobsimBeforeSimStep(org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {		
		QSim sim = (QSim) e.getQueueSimulation();
		qNetwork = (QNetwork) sim.getNetsimNetwork();
		//ArrayList<QLinkI> links = new ArrayList<QLinkI>(qNetwork.getNetsimLinks().values());
		//Collections.sort(links, (l1,l2) -> Integer.compare(l1.getAllNonParkedVehicles().size(), l2.getAllNonParkedVehicles().size())); 
		QLinkI accLink = null;
		MobsimVehicle accVeh = null;
		ArrayList<QLinkI> links = new ArrayList<QLinkI>(qNetwork.getNetsimLinks().values());
		List<QLinkI> LinksWithAcc = changer.getLimitedLinks().stream().map(l -> qNetwork.getNetsimLink(l.getId())).collect(Collectors.toList());
		links.removeAll(LinksWithAcc);
		for (QLinkI link : qNetwork.getNetsimLinks().values()) {
			boolean acc = false;
			int n = link.getAllNonParkedVehicles().size();
			if (n == 0) {
				continue;
			}
			ArrayList<MobsimVehicle> vehs = new ArrayList<MobsimVehicle>(link.getAllNonParkedVehicles());
			double prob = (LAMBDA / SECONDS_IN_YEAR);
			for (MobsimVehicle veh : vehs) {
				double r = MatsimRandom.getRandom().nextDouble();
				if (r <= prob) {
					accLink = link;
					accVeh = veh;
					acc = true;
					break;
				}
			}
			
			if (acc) {
				break;
			}
			
//			for (int i=links.size()-1; i>=0; i--) {
//				double p = 1 / Math.pow(2, i);
//				double r = MatsimRandom.getRandom().nextDouble();
//				if (r <= p && link.getAllNonParkedVehicles().size() > 0) {
//					link = links.get(i);
//					break;
//				}
//			}
		}
		
		if (accLink == null) {
			return;
		}
		//ArrayList<MobsimVehicle> vehs = new ArrayList<MobsimVehicle>(accLink.getAllNonParkedVehicles());
		//List<Id<Person>> drivers = vehs.stream().map(v -> v.getDriver().getId()).collect(Collectors.toList());
		Id<Person> principal = accVeh.getDriver().getId();
		List<Id<Person>> others = new ArrayList<Id<Person>>();
		CarAccidentEvent accident = new CarAccidentEvent("Normal", accLink.getLink().getId(), null, principal, others, e.getSimulationTime());
		
		events.processEvent(accident);
		//changer.handleEvent(accident);
	}

}
