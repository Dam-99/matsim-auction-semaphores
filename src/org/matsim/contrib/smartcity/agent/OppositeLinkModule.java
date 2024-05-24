/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;


/**
 * @author Filippo Muzzini
 *
 */
public class OppositeLinkModule extends AbstractQSimModule {
	public final static String DEPARTURE_HANDLER = "Opposite";
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.AbstractQSimModule#configureQSim()
	 */
	@Override
	protected void configureQSim() {
		bind(QNetsimEngine.class).asEagerSingleton();
		bindMobsimEngine(QNetsimEngineModule.NETSIM_ENGINE_NAME).to(QNetsimEngine.class);
		
		bind(OppositeLinkDepartureHandler.class).asEagerSingleton();
		bindDepartureHandler(QNetsimEngineModule.NETSIM_ENGINE_NAME).to(OppositeLinkDepartureHandler.class);
	}

}
