/**
 *
 */
package org.matsim.contrib.smartcity;

import org.matsim.api.core.v01.Id;
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
public class RunMASAEmergencyExperiment {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Gbl.assertIf(args.length >=1 && args[0]!="" );
        Scenario s = run(ConfigUtils.loadConfig(args[0]));
        String output_dir = s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).getParams().getOrDefault("outputDirectory", "output");
        List<Person> sorted = s.getPopulation().getPersons().values().stream()
                .sorted(Comparator.comparingLong(p -> (long) p.getAttributes().getAttribute(StaticDriverLogic.N_NODES_ATT)))
                .collect(Collectors.toList());
        int n = sorted.size();
        for (Person p : sorted.subList(n/2, n)){
            p.getAttributes().putAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME, BidAgent.class.getCanonicalName());
        }

        Person p = s.getPopulation().getPersons().get(Id.createPersonId(684));
        if (!p.getAttributes().getAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME).equals(BidAgent.class.getCanonicalName())){
            System.out.println("NON BIDAGENT!!!!");
            System.exit(1);
        }
        long original = Long.parseLong((String)p.getAttributes().getAttribute(BidAgent.BUDGET_ATT));

        for (int perc_budget_increased=0; perc_budget_increased<=1000; perc_budget_increased += 10){
            p.getAttributes().removeAttribute("signedCross");
            p.getAttributes().putAttribute(BidAgent.BUDGET_ATT, Long.toString(original + (original * perc_budget_increased / 100)));

            s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory", output_dir+"/"+perc_budget_increased);
            Controler c = new Controler(s);
            addModules(c);
            c.run();
        }
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
