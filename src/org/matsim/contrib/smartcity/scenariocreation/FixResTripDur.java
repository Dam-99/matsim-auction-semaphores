package org.matsim.contrib.smartcity.scenariocreation;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.smartcity.restriction.NetworkWithRestrictionTurnInfoBuilder;
import org.matsim.core.api.internal.MatsimPopulationObject;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.LinkToLinkRouting;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixResTripDur {

    private static RoutingModule getRouting(PopulationFactory popFac, Network network, Scenario scenario) {
        String mode = TransportMode.car;
        //Map<String, TravelDisutilityFactory> traMap = new HashMap<String, TravelDisutilityFactory>();
        //traMap.put(mode, new OnlyTimeDependentTravelDisutilityFactory());
        LinkToLinkTravelTime traTime = new FreespeedTravelTimeAndDisutility(1,1,1);
        NetworkTurnInfoBuilderI turnInfo = new NetworkWithRestrictionTurnInfoBuilder(scenario);
        LeastCostPathCalculatorFactory leastFactory = new DijkstraFactory();
        Injector inj = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(PopulationFactory.class).toInstance(popFac);
                bind(Network.class).toInstance(network);
                bind(LeastCostPathCalculatorFactory.class).toInstance(leastFactory);
                bind(LinkToLinkTravelTime.class).toInstance(traTime);
                bind(NetworkTurnInfoBuilderI.class).toInstance(turnInfo);
                MapBinder<String, TravelDisutilityFactory> mapbinder
                        = MapBinder.newMapBinder(binder(), String.class, TravelDisutilityFactory.class);
                mapbinder.addBinding(mode).toInstance(new OnlyTimeDependentTravelDisutilityFactory());
                //bind(RoutingModule.class).toProvider(LinkToLinkRouting.class);
            }

        });

        LinkToLinkRouting router = new LinkToLinkRouting(mode);
        inj.injectMembers(router);
        return router.get();
    }

    public static void main(String[] args) {
        String[] scenari = {"MASA","Manhattan"};
        String[] exps = {"all_equipped"};
        Map<String, String[]> xs = new HashMap<>();
//        String[] all_equipped = {"100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"};
//        xs.put("all_equipped", all_equipped);
//        String[] coexist = {"0", "10", "20", "30", "40", "50", "60", "70", "80", "90"};
//        xs.put("coexist", coexist);
//        String[] emergency = {"0", "200", "400", "600", "800", "1000", "1200", "1400", "1600", "1800", "2000"};
//        xs.put("emergency", emergency);

        //old
        String[] all_equipped = {"1000", "1500", "2000", "2500", "3000", "3500", "4000", "4500", "5000"};
        xs.put("all_equipped", all_equipped);
//        String[] coexist = {"10", "20", "30", "40", "50", "60", "70", "80", "90"};
//        xs.put("coexist", coexist);
//        String[] prop_coexist = {"1"};
//        xs.put("emergency_coexist", prop_coexist);

        String[] runs = {"5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19"};

        for (String scenario : scenari) {
            Network network = NetworkUtils.createNetwork();
            MatsimNetworkReader netReader = new MatsimNetworkReader(network);
            netReader.readFile(scenario+"/network.xml");

            for (String exp : exps) {
                String expFold;
                if (exp.equals("all_equipped")){
                    expFold = "all_equipped";
                } else if (exp.equals("prop_coexist")) {
                    expFold = "coexist";
                }
                else {
                    expFold = exp;
                }
                for (String x : xs.get(exp)) {
                    for (String run : runs) {
                        String[] modes = {"basic", "communication", "proportional", "fixed"};
                        for (String mode : modes) {
                            Scenario s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(scenario + "/config_prova.xml"));
                            s.getPopulation().getPersons().clear();
                            PopulationReader r = new PopulationReader(s);
//                        r.readFile(scenario+"/output/"+expFold+"/"+"210000"+"/plans"+run+".xml");
                            r.readFile(scenario + "/output/" + exp + "/" + mode + "/" + x + "/" + run + "/ITERS/it.0/0.plans.xml.gz");

                            PopulationFactory factory = s.getPopulation().getFactory();
                            RoutingModule routing = getRouting(factory, network, s);

                            ActivityFacilitiesFactoryImpl activityFac = new ActivityFacilitiesFactoryImpl();

                            for (Person p : s.getPopulation().getPersons().values()) {
                                ActivityFacility home = activityFac.createActivityFacility(null, ((Activity) p.getPlans().get(0).getPlanElements().get(0)).getLinkId());
                                ActivityFacility work = activityFac.createActivityFacility(null, ((Activity) p.getPlans().get(0).getPlanElements().get(2)).getLinkId());
                                List<? extends PlanElement> morningPlan = routing.calcRoute(home, work, 0, null);
                                List<? extends PlanElement> returnPlan = routing.calcRoute(work, home, 0, null);
                                double routeDur1 = ((Leg) morningPlan.get(0)).getTravelTime();
                                double routeDur2 = ((Leg) returnPlan.get(0)).getTravelTime();

                                double dur = routeDur1 + routeDur2;

                                //p.getAttributes().putAttribute("travelTime", dur);

                                updateTravelTime(p, dur, scenario, exp, x, run);

                            }
                        }
//                        PopulationWriter pw = new PopulationWriter(s.getPopulation());
//                        pw.write(scenario+"/plans/"+expFold+"/"+"210000"+"/plans"+run+".xml");
                    }
                }
            }
        }
    }

    private static void updateTravelTime(Person p, double dur, String scenario, String exp, String x, String run) {
        String[] modes = {"basic", "communication", "proportional", "fixed"};
        for (String mode : modes){
            String outfile = scenario+"/output/"+exp+"/"+mode+"/"+x+"/"+run+"/ITERS/it.0/0.tripdurations.txt";
            try {
                BufferedReader file = new BufferedReader(new FileReader(outfile));
                StringBuffer inputBuffer = new StringBuffer();
                String line;
                while ((line = file.readLine()) != null) {
                    if (line.split("\t")[0].equals(p.getId().toString())) {
                        String[] attrs = line.split("\t")[2].split(";");
                        inputBuffer.append(line.split("\t")[0]+"\t"+line.split("\t")[1]+"\t");
                        for (String att : attrs) {
                            String name = att.split("=")[0];
                            String value = att.split("=")[1];
                            if (name.equals("travelTime")) {
                                continue;
                            }
                            inputBuffer.append(name+"="+value+";");
                        }
                        inputBuffer.append("travelTime"+"="+dur);
                    } else {
                        inputBuffer.append(line);
                    }
                    inputBuffer.append("\n");
                }
                file.close();
                FileOutputStream fileOut = new FileOutputStream(outfile);
                fileOut.write(inputBuffer.toString().getBytes());
                fileOut.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //leggere file
            //per ogni riga splitto sugli spazi e vedo il primo per controllare persona
            //se è lei splitto gli attributi e aggiorno travel time
        }

    }
}
