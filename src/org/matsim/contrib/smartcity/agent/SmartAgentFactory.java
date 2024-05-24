/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.agent.parking.NoParkingLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This AgentFactory instantiate the parking and smart drive logic specified in the
 * configuration for the specific person; the create the agent.
 * 
 * @see SmartAgentLogic
 * @see SmartDriverLogic
 * @see ParkingSearchLogic
 * @see Injector
 * @author Filippo Muzzini
 *
 */
public class SmartAgentFactory implements AgentFactory {
	
	@Inject private Netsim simulation;
	@Inject private Injector inj;
	@Inject private EventsManager events;
	
	public static final String DRIVE_LOGIC_NAME = "drivelogic";
	public static final String PARKING_LOGIC_NAME = "parkinglogic";
	public static final String DRIVERLOGICATT = "driverlogic";
	
	private String DEFAULT_DRIVE_LOGIC = StaticDriverLogic.class.getCanonicalName();
	private String DEFAULT_PARKING_LOGIC = NoParkingLogic.class.getCanonicalName();

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.agents.AgentFactory#createMobsimAgentFromPerson(org.matsim.api.core.v01.population.Person)
	 */
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		String driveLogicClass = (String) p.getAttributes().getAttribute(DRIVE_LOGIC_NAME);
		String parkingLogicClass = (String) p.getAttributes().getAttribute(PARKING_LOGIC_NAME);
		driveLogicClass = driveLogicClass != null ? driveLogicClass : DEFAULT_DRIVE_LOGIC;
		parkingLogicClass = parkingLogicClass != null ? parkingLogicClass : DEFAULT_PARKING_LOGIC;

        Map<String, Object> params = p.getAttributes().getAsMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		
		SmartDriverLogic smartLogic = InstantationUtils.instantiateForNameWithParams(inj, driveLogicClass, params);
		ParkingSearchLogic parkingLogic = InstantationUtils.instantiateForName(inj, parkingLogicClass);
		
		DynAgentLogic agentLogic = new SmartAgentLogic(p.getSelectedPlan(), this.simulation, smartLogic, parkingLogic);
		inj.injectMembers(agentLogic);
		
		
		Id<Link> startLinkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		p.getCustomAttributes().put(DRIVERLOGICATT, smartLogic);
		
		DynAgent agent = new DynAgent(p.getId(), startLinkId, events, agentLogic);
		smartLogic.setPerson(p);
		smartLogic.setAgent(agent);
		
		return agent;
	}

}
