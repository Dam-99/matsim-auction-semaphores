/**
 * 
 */
package org.matsim.contrib.smartcity.analisys;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.matsim.analysis.TravelDistanceStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author Filippo Muzzini
 *
 */
public class TravelDistance extends TravelDistanceStats {
	
	private static final String FILENAME_TRAVELBYPERSON = "travel_by_person.txt";
	private Map<Id<Person>, Double> distance = new HashMap<Id<Person>, Double>();
	private String file;

	@Inject
	TravelDistance(Config config, OutputDirectoryHierarchy controlerIO) {
		super(config, controlerIO.getOutputFilename(Controler.FILENAME_TRAVELDISTANCESTATS), false);
		this.file = controlerIO.getOutputFilename(FILENAME_TRAVELBYPERSON);
	}
	
	@Override
	public void addIteration(int iteration, Map<Id<Person>, Plan> map) {
		for (Entry<Id<Person>, Plan> e : map.entrySet()) {
			this.distance.put(e.getKey(), calcDist(e.getValue()));
		}
		super.addIteration(iteration, map);
	}
	
	@Override
	public void close() {
		try {
			PrintWriter writer = new PrintWriter(this.file);
			writer.write("Person\tDistance\n");
			for (Entry<Id<Person>, Double> e : this.distance.entrySet()) {
				writer.write(e.getKey()+"\t"+e.getValue()+"\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		super.close();
	}

	private Double calcDist(Plan p) {
		double planTravelDistance = 0.0;
		for (PlanElement pe : p.getPlanElements()) {
			if (pe instanceof Leg) {
				final Leg leg = (Leg) pe;
				double distance = leg.getRoute().getDistance();
				if (!Double.isNaN(distance)) {
					planTravelDistance += distance;
				}
			}
		}
		
		return planTravelDistance;
	}
}
