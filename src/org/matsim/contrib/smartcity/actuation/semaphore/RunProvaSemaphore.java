package org.matsim.contrib.smartcity.actuation.semaphore;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Main class to test the semaphore module
 * 
 * @author Filippo Muzzini
 *
 */
public class RunProvaSemaphore {
	//private static final String INPUT_DIR = "./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/";

	private static final String INPUT_DIR = "./testSignal/useSignalInput/withLanes/";
	
	public static void run(boolean startOtfvis) {
		// --- load the configuration file
		Config config = ConfigUtils.loadConfig(INPUT_DIR + "config.xml");
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		// --- create the scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// load the information about signals data (i.e. fill the SignalsData object) and add it to the scenario as scenario element
		

		// --- create the controler
		Controler c = new Controler(scenario);
		// add the semaphore module
		c.addOverridingModule(new SmartSemaphoreModule());
		if (startOtfvis) {
			// add the module that start the otfvis visualization with signals
			c.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}

		// --- run the simulation
		c.run();
	}

	public static void main(String[] args) {
		run(true);
	}
}
