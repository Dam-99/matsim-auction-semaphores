package org.matsim.contrib.smartcity.actuation.semaphore;

import com.google.inject.Injector;
import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalEvents2ViaCSVWriter;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.builder.SignalSystemsModelBuilder;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.contrib.smartcity.analisys.CalcSemaphoreCrossingTime;
import org.matsim.contrib.smartcity.restriction.NetworkWithRestrictionTurnInfoBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSignalsNetworkFactory;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.replanning.ReplanningContext;

/**
 * This module is the same of SingalModule but using
 * SmartSemaphoreModelFactory is more flexible
 * 
 * @author Filippo Muzzini
 *
 */
public class SmartSemaphoreModule extends AbstractModule implements StartupListener {

	private SignalSystemsManager signalSystemsManager;
	
	@Override
	public void install() {
		if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			// bindings for smartsignals
			bind(SignalModelFactory.class).to(SmartSemaphoreModelFactory.class);
			//addControlerListenerBinding().to(DefaultSignalControlerListener.class);

			// general signal bindings
			addControlerListenerBinding().to(this.getClass());
			bind(SignalSystemsModelBuilder.class).to(FromDataBuilder.class);
			addMobsimListenerBinding().to(QSimSignalEngine.class);
			bind(QNetworkFactory.class).to(QSignalsNetworkFactory.class);

			// bind tool to write information about signal states for via
			bind(SignalEvents2ViaCSVWriter.class).asEagerSingleton();
			/* asEagerSingleton is necessary to force creation of the SignalEvents2ViaCSVWriter class as it is never used somewhere else. theresa dec'16 */

			if (getConfig().qsim().isUsingFastCapacityUpdate()) {
				throw new RuntimeException("Fast flow capacity update does not support signals");
			}
		}
		if (getConfig().controler().isLinkToLinkRoutingEnabled()){
			//use the extended NetworkWithSignalsTurnInfoBuilder (instead of NetworkTurnInfoBuilder)
			//michalm, jan'17
			bind(NetworkTurnInfoBuilderI.class).to(NetworkWithRestrictionTurnInfoBuilder.class);
		}
		
		

	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		Scenario scenario = event.getServices().getScenario();
		createScenarioElement(scenario);
	}
	
	/**
	 * 
	 * @param scenario
	 */
	public static void createScenarioElement(Scenario scenario) {
		if (scenario.getScenarioElement(SignalsData.ELEMENT_NAME) != null) {
			return;
		}
		Config config = scenario.getConfig();
		SignalsData signalsData = new SignalsDataLoader(config).loadSignalsData();
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
	}

	@Provides
	SignalSystemsManager provideSignalSystemsManager(ReplanningContext replanningContext, SignalSystemsModelBuilder modelBuilder) {
		if (signalSystemsManager == null) {
			signalSystemsManager = modelBuilder.createAndInitializeSignalSystemsManager();
			signalSystemsManager.resetModel(replanningContext.getIteration());
		}

		return signalSystemsManager;
	}

}
