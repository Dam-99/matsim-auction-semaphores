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
import org.matsim.contrib.smartcity.comunication.ComunicationConfigGroup;
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
        Gbl.assertIf(args.length >= 1 && args[0] != "");
        String cfg_pth;
        Scenario s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
        Controler controler = new Controler(s);
        addModules(controler);
        controler.run();
        System.exit(0);
        args = new String[10];
        args[0] = "manhattan"; // map
        args[1] = Integer.toString(500); // config file usato da plansCreation
        // args[1+1] = ...; // ~total population? experiment name? output path?~ "root" dir per i file dell'esperimento
        args[3] = Integer.toString(0); // boh
        args[4] = "./configs/config_ftc.xml"; // config path
        args[5] = "./esempio/plans_mixed.xml"; // plans path
        args[6] = "org.matsim.contrib.smartcity.agent.BidAgent"; // smart agents class name
        args[7] = "mode=N"; // mode? traffic light mode?
        args[8] = "bidMode=nonrandom"; // bid_mode
        args[9] = "budget=[10-280,380-650,750-1000]"; // budget array

        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[4]));
        runExperimentsOnSystem(SemaphoreSystem.FTC, args, s);
        cfg_pth = "./configs/config_basic.xml";
        args[4] = cfg_pth;
        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(cfg_pth));
        runExperimentsOnSystem(SemaphoreSystem.Basic, args, s);
        cfg_pth = "./configs/config.xml";
        args[4] = cfg_pth;
        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(cfg_pth));
        runExperimentsOnSystem(SemaphoreSystem.Coordinated, args, s);
        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(cfg_pth));
        runExperimentsOnSystem(SemaphoreSystem.CoordinatedNoPropagation, args, s);
        cfg_pth = "./configs/config_proportional.xml";
        args[4] = cfg_pth;
        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(cfg_pth));
        runExperimentsOnSystem(SemaphoreSystem.CoordinatedProp, args, s);
        s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(cfg_pth));
        runExperimentsOnSystem(SemaphoreSystem.CoordinatedPropNoPropagation, args, s);
    }

    private static void runExperimentsOnSystem(SemaphoreSystem system, String[] args, Scenario s) {
        if (s == null) {
            s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
        }
        String map_name = args[0];
        args = popFirst(args);
        String outputRoot;
        String ogServerList = s.getConfig().getModules().get(ComunicationConfigGroup.GROUPNAME).getParams().get(ComunicationConfigGroup.WRAPPER);
        String serverList = ogServerList;
        String systemDirName;
        boolean runNoPropagation = false;
        outputRoot = "output/" + map_name + "/";
        switch (system) {
            case FTC:
                systemDirName = "ftc";
                break;
            case Basic:
                systemDirName = "basic";
                break;
            case CoordinatedNoPropagation:
                runNoPropagation = true;
                serverList = "./serverList_communication.xml";
            case Coordinated:
                systemDirName = "communication";
                break;
            case CoordinatedPropNoPropagation:
                runNoPropagation = true;
                serverList = "./serverList_proportional.xml";
            case CoordinatedProp:
                systemDirName = "proportional";
                break;
            default:
                systemDirName = "other";
                break;
        }

        int exp = 1;
        boolean createPlans = false;
        System.out.println("SEMAPHORE " + systemDirName + (runNoPropagation ? " - NO propagation" : ""));
        if (!runNoPropagation) {
        // boolean isBasic = args[3].contains("_basic");
        System.out.println("EXPERIMENT " + exp);
        for (int totalAgents = 500; totalAgents <= 5000; totalAgents += 500) {
            args[0] = "" + totalAgents;
            args[1] = "exp" + exp;
            System.out.println("SETTING" + " " + systemDirName + " " + runNoPropagation + " " + args[1]);
            RandomPlansCreationMixed.main(args, exp, createPlans);// , isBasic);
            s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[3]));
            for (int i = 0; i < 20; i++) {
                System.out.println("PLANS " + i);
                s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory",
                        outputRoot + args[1] + "/" + systemDirName + "/" + totalAgents + "agents" + "/" + i + "/");
                s.getConfig().getModules().get("plans").addParam("inputPlansFile",
                        "../plans/" + args[1] + "/" + totalAgents + "agents" + "/" + "plans" + i + ".xml");
                s = ScenarioUtils.loadScenario(s.getConfig());
                Controler controler = new Controler(s);
                addModules(controler);
                System.out.println("ENDRUN agents: " + totalAgents + " i: " + i);
                controler.run();
            }
        }
        exp = 2;
        System.out.println("EXPERIMENT " + exp);
        for (int totalAgents = 1000; totalAgents <= 4000; totalAgents += 1500) {
            args[0] = "" + totalAgents;
            args[1] = "exp" + exp + "traffic" + totalAgents;
            System.out.println("SETTING" + " " + systemDirName + " " + runNoPropagation + " " + args[1]);
            s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[3]));
            for (int smartPercentage = 20; smartPercentage <= 80; smartPercentage += 20) {
                args[2] = "" + smartPercentage;
                RandomPlansCreationMixed.main(args, exp, createPlans);// , isBasic);
                int smartAgents = totalAgents * smartPercentage / 100;
                for (int i = 0; i < 5; i++) {
                    System.out.println("PLANS " + smartPercentage + i);
                    s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory",
                            outputRoot + args[1] + "/" + systemDirName + "/" + smartAgents + "smart_agents" + "/" + i + "/");
                    s.getConfig().getModules().get("plans").addParam("inputPlansFile",
                            "../plans/" + args[1] + "/" + smartAgents + "smart_agents" + "/" + "plans" + i + ".xml");
                    s = ScenarioUtils.loadScenario(s.getConfig());
                    Controler controler = new Controler(s);
                    addModules(controler);
                    System.out.println("ENDRUN agents: " + totalAgents + " i: " + i + " smart: " + smartAgents);
                    controler.run();
                }
            }
        }

        }
        exp = 3;
        System.out.println("EXPERIMENT " + exp);
        for (int totalAgents = 1000; totalAgents <= 4000; totalAgents += 1500) {
            args[0] = "" + totalAgents;
            args[1] = "exp" + exp + "traffic" + totalAgents;
            s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[3]));
            System.out.println("SETTING" + " " + systemDirName + " " + runNoPropagation + " " + args[1]);
            int smartPercentage = 60;
            args[2] = "" + smartPercentage;
            int emergencyBudget = 210000;
            args[4] = "" + emergencyBudget;
            RandomPlansCreationMixed.main(args, exp, createPlans);// , isBasic);
            // int smartAgents = totalAgents * smartPercentage / 100;
            for (int i = 0; i < 5; i++) {
                System.out.println("PLANS " + i);
                if (runNoPropagation) {
                    s.getConfig().getModules().get(ComunicationConfigGroup.GROUPNAME)
                            .addParam(ComunicationConfigGroup.SERVERLIST, serverList);
                    s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory",
                            outputRoot + "exp" + exp + "_no_prop/" + systemDirName + "/" + totalAgents + "traffic" + emergencyBudget + "emergency_budget" + "/" + i + "/");
                } else {
                    s.getConfig().getModules().get(ControlerConfigGroup.GROUP_NAME).addParam("outputDirectory",
                            outputRoot + "exp" + exp + "/" +  systemDirName + "/" + totalAgents + "traffic" + emergencyBudget + "emergency_budget" + "/" + i + "/");
                }
                s.getConfig().getModules().get("plans").addParam("inputPlansFile",
                        "../plans/" + args[1] + "/" + emergencyBudget + "emergency_budget" + "/" + "plans" + i + ".xml");
                s = ScenarioUtils.loadScenario(s.getConfig());
                Controler controler = new Controler(s);
                addModules(controler);
                controler.run();
                System.out.println("ENDRUN agents: " + totalAgents + " i: " + i + " propagation: " + runNoPropagation);
                s.getConfig().getModules().get(ComunicationConfigGroup.GROUPNAME).addParam(ComunicationConfigGroup.WRAPPER, ogServerList);
            }
        }
        args = pushFirst(args, map_name);
    }

    private static void addModules(Controler controler) {
        // add perception module
        controler.addOverridingModule(new SmartPerceptionModule());

        // add signal module
        controler.addOverridingModule(new SmartSemaphoreModule());

        // add smartagent module
        controler.addOverridingModule(new SmartAgentModule());

        // add comunication module
        controler.addOverridingModule(new ComunicationModule());

        // add accident module
        // controler.addOverridingModule(new AccidentModule());

        // add auction intersesction module
        controler.addOverridingModule(new AuctionIntersectionModule());

        // add restriction module
        controler.addOverridingModule(new RestrictionsModule());

        controler.addOverridingModule(new AnalisysModule());

        // add vis module
        if (controler.getConfig().getModules().containsKey(OTFVisConfigGroup.GROUP_NAME)) {
            ConfigUtils.addOrGetModule(controler.getConfig(), OTFVisConfigGroup.class);

            // controler.addOverridingModule(new OTFVisLiveModule());
            /*
             * controler.addOverridingModule(new AbstractModule() {
             * 
             * @Override
             * public void install() {
             * ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
             * addEventHandlerBinding().toInstance(visualiser);
             * addControlerListenerBinding().toInstance(visualiser);
             * }
             * });
             */

            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
    }

    private enum SemaphoreSystem {
        FTC,
        Basic,
        Coordinated,
        CoordinatedProp,
        CoordinatedNoPropagation,
        CoordinatedPropNoPropagation
    }

    private static String[] popFirst(String[] arr) {
        String[] new_arr = new String[arr.length - 1];
        System.arraycopy(arr, 1, new_arr, 0, new_arr.length);
        return new_arr;
    }

    private static String[] pushFirst(String[] arr, String el) {
        String[] new_arr = new String[arr.length + 1];
        new_arr[0] = el;
        System.arraycopy(arr, 0, new_arr, 1, arr.length);
        return new_arr;
    }

    /**
     * @param config
     */
    private static Scenario run(Config config) {
        // get a simple scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        addModules(controler);

        controler.run();

        return scenario;

    }

}
