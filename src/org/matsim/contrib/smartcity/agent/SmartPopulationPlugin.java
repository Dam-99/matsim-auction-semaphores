/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;

/**
 * Plugin for MATSim that bind the agent factory to SmartAgentFactory
 * 
 * @see SmartAgentFactory
 * @author Filippo Muzzini
 *
 */
public class SmartPopulationPlugin extends AbstractQSimModule {
	public final static String SMART_POPULATION_AGENT_SOURCE_NAME = "PopulationAgentSource";


	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.AbstractQSimModule#configureQSim()
	 */
	@Override
	protected void configureQSim() {
		bind(AgentFactory.class).to(SmartAgentFactory.class).asEagerSingleton(); // (**)
		bind(PopulationAgentSource.class).asEagerSingleton();
		
		bindAgentSource(SMART_POPULATION_AGENT_SOURCE_NAME).to(PopulationAgentSource.class);
		
	}

}
