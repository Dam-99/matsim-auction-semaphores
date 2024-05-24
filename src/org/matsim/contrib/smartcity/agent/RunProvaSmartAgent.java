/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Simple main class to test SmartAgent
 * 
 * @author Filippo Muzzini
 *
 */
public class RunProvaSmartAgent {
	
	public static void main(String[] args) {
		Gbl.assertIf(args.length >=1 && args[0]!="" );
		run(ConfigUtils.loadConfig(args[0]));
	}
	
	static void run(Config config) {
		
		//get a simple scenario		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
				
		Controler controler = new Controler(scenario) ;
		
		//add smartagent module
		controler.addOverridingModule(new SmartAgentModule());
		
		//add vis module
		controler.addOverridingModule(new OTFVisLiveModule());
		
		controler.run();
	}

}
