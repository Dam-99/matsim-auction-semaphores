/**
 * 
 */
package org.matsim.contrib.smartcity;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.smartcity.accident.AccidentModule;
import org.matsim.contrib.smartcity.actuation.semaphore.SmartSemaphoreModule;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.agent.SmartAgentFactory;
import org.matsim.contrib.smartcity.agent.SmartAgentModule;
import org.matsim.contrib.smartcity.agent.StaticDriverLogic;
import org.matsim.contrib.smartcity.analisys.AnalisysModule;
import org.matsim.contrib.smartcity.auctionIntersections.AuctionIntersectionModule;
import org.matsim.contrib.smartcity.comunication.ComunicationModule;
import org.matsim.contrib.smartcity.perception.SmartPerceptionModule;
import org.matsim.contrib.smartcity.restriction.RestrictionsModule;
import org.matsim.contrib.smartcity.scenariocreation.RandomPlansCreation;
import org.matsim.contrib.smartcity.scenariocreation.RandomPlansCreationMixed;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Filippo Muzzini
 *
 */
public class RunSmartcity {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.assertIf(args.length >=1 && args[0]!="" );
		Scenario s;
		s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])) ;
		Controler controler = new Controler(s);
		addModules(controler);
		controler.run();
		 // = run(ConfigUtils.loadConfig(args[0]));
//		args = new String[9];
		/*for(int i = 2000; i <= 20000; i += 1000) {
			
			final int limit = i;
			List<Person> personList = s.getPopulation().getPersons().values().stream()
									   .filter(p -> Integer.parseInt(p.getId().toString()) < limit)
									   .collect(Collectors.toList());
			for(Person p : personList ) {
				p.getAttributes().putAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME, StaticDriverLogic.class.getCanonicalName());
			}
			//RandomPlansCreationMixed.main(args);
			s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory", "output/random/"+i+"/");
			//s.getConfig().getModules().get("plans").addParam("inputPlansFile", "../Risultati sperimentali/MASA/mixed_advanced/"+i+"/output_plans.xml");
			//s = ScenarioUtils.loadScenario(s.getConfig());
			Controler controler = new Controler(s);
			addModules(controler);
			controler.run();

		}*/
//		args[0] = Integer.toString(200);
//		args[2] = Integer.toString(0);
//		args[3] = "./scenarioManhattan/config.xml";
//		args[4] = "./scenarioManhattan/plans_mixed.xml";
//		args[5] = "org.matsim.contrib.smartcity.agent.BidAgent";
//		args[6] = "mode=N";
//		args[7] = "bidMode=nonrandom";
//		args[8] = "budget=[10-280,380-650,750-1000]";
//		for(int i = 1; i < 21; i++) {
//			args[1] = Integer.toString(i*1000);
//			RandomPlansCreationMixed.main(args);
//			s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("./scenarioManhattan/config.xml")) ;
//			s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory", "output_sem_manhattan_max"+args[1]+"/test/"+i+"/");
//			Controler controler = new Controler(s);
//			addModules(controler);
//			controler.run();
//		}
		/*List<Person> sorted = s.getPopulation().getPersons().values().stream()
				.sorted(Comparator.comparingLong(p -> (long) p.getAttributes().getAttribute(StaticDriverLogic.N_NODES_ATT)))
				.collect(Collectors.toList());
		int n = sorted.size();
		for (Person p : sorted.subList(n/2, n)){
			p.getAttributes().putAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME, BidAgent.class.getCanonicalName());
		}
		sorted = sorted.subList(n/2, n);
		long max = sorted.stream().map(p -> (long) p.getAttributes().getAttribute(StaticDriverLogic.N_NODES_ATT)).max(Comparator.comparingLong(l -> l)).get();

		sorted = sorted.stream().filter(p -> (long)p.getAttributes().getAttribute(StaticDriverLogic.N_NODES_ATT) == max).collect(Collectors.toList());
	
		HashMap<Person, Long> original = new HashMap<Person, Long>();

		for (Person p : sorted) {
			original.put(p, Long.parseLong((String)p.getAttributes().getAttribute(BidAgent.BUDGET_ATT)));
		}


		n = sorted.size();
		for (int perc_agent_increased=84; perc_agent_increased>=1; perc_agent_increased--){
			List<Person> to_increase = sorted.subList(0, n*perc_agent_increased/100);
			FileWriter fw = null;
			try {
				File f = new File("output/"+perc_agent_increased+"/increased.txt");
				f.getParentFile().mkdirs();
				fw = new FileWriter(f);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			for (Person p : to_increase){
				try {
					fw.write(p.getId().toString()+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int perc_budget_increased=1; perc_budget_increased<=100; perc_budget_increased++){
				for (Person p : to_increase){
					long orig = original.get(p);
					p.getAttributes().removeAttribute("signedCross");
					p.getAttributes().putAttribute(BidAgent.BUDGET_ATT, Long.toString(orig + (orig * perc_budget_increased / 100)));
				}

				s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory", "output/"+perc_agent_increased+"/"+perc_budget_increased);
				Controler c = new Controler(s);
				addModules(c);
				c.run();
			}
		}*/
		
		/*List<Person> fourCrossedSem = s.getPopulation().getPersons().values().stream()
									   .filter(p ->(long) p.getAttributes().getAttribute(StaticDriverLogic.N_NODES_ATT) == 3)
									   .collect(Collectors.toList());
		fourCrossedSem = s.getPopulation().getPersons().values().stream()
						  .filter(p -> p.getId().toString().equals(Integer.toString(4970)) ||
								  		p.getId().toString().equals(Integer.toString(2302)) ||
								  		p.getId().toString().equals(Integer.toString(3632)) ||
								  		p.getId().toString().equals(Integer.toString(3660)))
						  .collect(Collectors.toList());
		try {
	      FileWriter myWriter = new FileWriter("./output/incrementedAgent.txt");
	      for (int j = 0; j < 4; j++)
	    	  myWriter.write(fourCrossedSem.get(j).getId().toString() + "\n");
	      myWriter.close();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
		
		for (int i = 0; i < 1; i++) {
			Person p = fourCrossedSem.get(i);
			Person p = s.getPopulation().getPersons().values().stream()
					   .filter(pers -> pers.getId().toString().equals(Integer.toString(12225)))
					   .collect(Collectors.toList()).get(0);
					   
			long orig = Long.parseLong((String)p.getAttributes().getAttribute(BidAgent.BUDGET_ATT));
			for (int increment = 2500; increment <= 40000; increment += 2500){
				p.getAttributes().putAttribute(BidAgent.BUDGET_ATT, Long.toString(orig + increment));
				s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory", "output/"+ p.getId() + "/" + increment+"/");
				Controler c = new Controler(s);
				addModules(c);
				c.run();
			}
			p.getAttributes().putAttribute(BidAgent.BUDGET_ATT, Long.toString(orig));
		}*/
	}

	private static void addModules(Controler controler) {
		//add perception module
		controler.addOverridingModule(new SmartPerceptionModule());

		//add signal module
		controler.addOverridingModule(new SmartSemaphoreModule());

		//add smartagent module
		controler.addOverridingModule(new SmartAgentModule());

		//add comunication module
		controler.addOverridingModule(new ComunicationModule());

		//add accident module
		//controler.addOverridingModule(new AccidentModule());

		//add auction intersesction module
		//controler.addOverridingModule(new AuctionIntersectionModule());

		//add restriction module
		controler.addOverridingModule(new RestrictionsModule());

		controler.addOverridingModule(new AnalisysModule());

		//add vis module
		if (controler.getConfig().getModules().containsKey(OTFVisConfigGroup.GROUP_NAME)) {
			ConfigUtils.addOrGetModule(controler.getConfig(), OTFVisConfigGroup.class);

			//controler.addOverridingModule(new OTFVisLiveModule());
			/*controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
					addEventHandlerBinding().toInstance(visualiser);
					addControlerListenerBinding().toInstance(visualiser);
				}
			});*/

			controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}
	}

	/**
	 * @param loadConfig
	 */
	private static Scenario run(Config config) {
		//get a simple scenario		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
				
		Controler controler = new Controler(scenario) ;
		
		addModules(controler);
		
		controler.run();

		return scenario;
		
	}

}
