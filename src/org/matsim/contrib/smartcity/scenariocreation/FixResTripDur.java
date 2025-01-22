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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    public static void main(Scenario s){
        String[] scenari = {"manhattan"};
        String[] exps = {"all_equipped"};
        Map<String, String[]> exps_populs = new HashMap<>();
//        String[] all_equipped = {"100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"};
//        xs.put("all_equipped", all_equipped);
//        String[] coexist = {"0", "10", "20", "30", "40", "50", "60", "70", "80", "90"};
//        xs.put("coexist", coexist);
//        String[] emergency = {"0", "200", "400", "600", "800", "1000", "1200", "1400", "1600", "1800", "2000"};
//        xs.put("emergency", emergency);

        //old
         String[] all_equipped = {"1000", "1500", "2000", "2500", "3000", "3500", "4000", "4500", "5000"};
        // String[] all_equipped = {"500"};
        exps_populs.put("all_equipped", all_equipped);
//        String[] coexist = {"10", "20", "30", "40", "50", "60", "70", "80", "90"};
//        xs.put("coexist", coexist);
//        String[] prop_coexist = {"1"};
//        xs.put("emergency_coexist", prop_coexist);


        for (String scenario : scenari) {
            Network network = NetworkUtils.createNetwork();
            MatsimNetworkReader netReader = new MatsimNetworkReader(network);
            netReader.readFile("configs/network.xml");

            for (String exp : exps) {
                String expFold;
                if (exp.equals("all_equipped")){
                    expFold = "allEquipped";
                } else if (exp.equals("prop_coexist")) {
                    expFold = "coexist";
                }
                else {
                    expFold = exp;
                }
                String[] modes = {"basic", "communication", "proportional", "fixed"};
                for (String mode : modes) {
                for (String x : exps_populs.get(exp)) {
                    for (int run = 0; run < 20; run++) {
                            // Scenario s = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(scenario + "/config_prova.xml"));
                            s.getPopulation().getPersons().clear();
                            PopulationReader r = new PopulationReader(s);
//                        r.readFile(scenario+"/output/"+expFold+"/"+"210000"+"/plans"+run+".xml");
                            String plansPath = "plans/"+expFold+"/"+ mode + "/" + x + "/plans"+run+".xml";
                            String resDir = "output/all_equipped/"+(mode=="fixed"?"ftc":mode)+"/"+x+"agents/"+run+"/ITERS/it.0/";
                  if (!Files.exists(Paths.get(plansPath))) {
                  // if (!Files.exists(Paths.get(resDir+"0.tripdurations.txt"))) {
                    System.out.println("FIXING - SKIPPED " + resDir + " " + run); continue; }
                             r.readFile(plansPath);

                            PopulationFactory factory = s.getPopulation().getFactory();
                            RoutingModule routing = getRouting(factory, network, s);

                            ActivityFacilitiesFactoryImpl activityFac = new ActivityFacilitiesFactoryImpl();
                            HashMap<Integer,String> popAttrMap = getPopulationAttributeMap(resDir, Integer.parseInt(x));


                            for (Person p : s.getPopulation().getPersons().values()) {
                                ActivityFacility home = activityFac.createActivityFacility(null, ((Activity) p.getPlans().get(0).getPlanElements().get(0)).getLinkId());
                                ActivityFacility work = activityFac.createActivityFacility(null, ((Activity) p.getPlans().get(0).getPlanElements().get(2)).getLinkId());
                                List<? extends PlanElement> morningPlan = routing.calcRoute(home, work, 0, null);
                                List<? extends PlanElement> returnPlan = routing.calcRoute(work, home, 0, null);
                                double routeDur1 = ((Leg) morningPlan.get(0)).getTravelTime();
                                double routeDur2 = ((Leg) returnPlan.get(0)).getTravelTime();

                                double tripTrav = routeDur1 + routeDur2;

                                // ------ UPDATETRAVELTIME OUTPUT ------
                                p.getAttributes().putAttribute("travelTime", tripTrav);
                                Integer pId = Integer.parseInt(p.getId().toString());
                                String dur = popAttrMap.get(pId).split("\t")[0];
                                String attrs = popAttrMap.get(pId).split("\t")[1];
                                String[] newAttrs = new String[6];
                                int i = 0;
                                boolean found = false;
                                for (String attr : attrs.split(";")) {
                                    String name = attr.split("=")[0];
                                    String value = attr.split("=")[1];
                                    if (name.equals("travelTime")) {
                                        value = "" + tripTrav;
                                        found = true;
                                    }
                                    newAttrs[i] = name + "=" + value;
                                    i++;
                                }
                                if (!found) {
                                    newAttrs[i] = "travelTime=" + tripTrav;
                                }
                                popAttrMap.put(pId, dur + "\t" + String.join(";", newAttrs));
                                // -------------------------------------
                            }
                             updateTravelTime(popAttrMap, Integer.parseInt(x), resDir);
                            // PopulationWriter pw = new PopulationWriter(s.getPopulation());
                            // pw.write(plansPath);
                        }
                    }
                }
            }
        }
    }

    private static void updateTravelTime(HashMap<Integer,String> popAttrMap, Integer popSize, String resDir) {
            try {
                String resPath = resDir+"0.tripdurations.txt";
                BufferedReader file = new BufferedReader(new FileReader(resPath));
                StringBuffer inputBuffer = new StringBuffer();
                String line;
                int count = 0;
                boolean flushedPop = false;
                while ((line = file.readLine()) != null) {
                    if (count > 0 && count <= popSize) {
                        if (!flushedPop) {
                            popAttrMap.forEach((id, attrs) -> {
                                inputBuffer.append(id + "\t" + attrs + "\n");
                            });
                            flushedPop = true;
                        }
                    } else {
                        inputBuffer.append(line + "\n");
                    }
                    count++;
                }
                file.close();
                FileOutputStream fileOut = new FileOutputStream(resPath);
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

    private static HashMap<Integer, String> getPopulationAttributeMap(String resDir, Integer popSize) {
        HashMap<Integer,String> popAttrMap = new LinkedHashMap<Integer, String>();
        String resPath = resDir+"0.tripdurations.txt";
        try {
            BufferedReader file = new BufferedReader(new FileReader(resPath));
            String line;
            int count = 0;
            while ((line = file.readLine()) != null) {
                if (count == 0 || count > popSize) { // salta le righe che non contengono dati della popolazione
                    count++;
                    continue;
                }
                popAttrMap.put(Integer.parseInt(line.split("\t")[0]),  line.split("\t")[1] + "\t" + line.split("\t")[2]);

                count++;
            }
            file.close();
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return popAttrMap;
    }
}
