/**
 * 
 */
package org.matsim.contrib.smartcity.analisys;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author Filippo Muzzini
 *
 */
public class CalcLegTimesWithoutWalk extends CalcLegTimes {
	
	private HashSet<Id<Person>> inTrip = new HashSet<Id<Person>>();
	private HashMap<Id<Person>, Double> start = new HashMap<Id<Person>, Double>();
	private HashMap<Id<Person>, Double> end = new HashMap<Id<Person>, Double>();
	private HashMap<Id<Person>, Double> duration = new HashMap<Id<Person>, Double>();

	@Inject
	private Population population;
	
	@Inject
	CalcLegTimesWithoutWalk(EventsManager eventsManager) {
		eventsManager.addHandler(this);
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		if (event.getLegMode().equals("car") && !this.inTrip.contains(event.getPersonId())) {
			this.inTrip.add(event.getPersonId());
			this.start.put(event.getPersonId(), event.getTime());
			super.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		if (event.getLegMode().equals("car")) {
			this.end.put(event.getPersonId(), event.getTime());
			super.handleEvent(event);
		}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.inTrip.remove(event.getPersonId());
		double dur = this.end.get(event.getPersonId()) - this.start.get(event.getPersonId());
		Double lastDur = this.duration.get(event.getPersonId());
		double totDur = lastDur != null ? (lastDur+dur) : dur;
		this.duration.put(event.getPersonId(), totDur);
		super.handleEvent(event);
	}
	
	@Override
	public void writeStats(final java.io.Writer out) throws UncheckedIOException {
		try {
			out.write("Person\tDuration\tAttributes\n");
			for (Entry<Id<Person>, Double> e : this.duration.entrySet()) {
			    String atts = population.getPersons().get(e.getKey()).getAttributes().getAsMap().entrySet().stream()
                        .map(att -> att.getKey()+"="+att.getValue()).collect(Collectors.joining(";"));
				out.write(e.getKey()+"\t"+e.getValue()+"\t"+atts+"\n");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		super.writeStats(out);
	}
	
}
