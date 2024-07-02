package org.matsim.contrib.smartcity.scenariocreation;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.agent.SmartAgentFactory;
import org.matsim.contrib.smartcity.agent.SmartDriverLogic;
import org.matsim.contrib.smartcity.agent.StaticDriverLogic;
import org.matsim.contrib.smartcity.restriction.NetworkWithRestrictionTurnInfoBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
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
import org.reflections.Reflections;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;

public class RandomPlansCreationMixed {
	private static final String DEFAULT_OUTPUT_FILE = "plans.xml";
	private static final String HOME_ACT = "h";
	private static final String WORK_ACT = "w";
	private static final double WORK_END_MEAN = 3600*18;
	private static final double WORK_VAR = 3600*1;
	private static final double WORK_START_MEAN = 3600*9;

//	/**
//     * experiment 1
//     * Crea un file population secondo i parametri passati e lo salva nel path passato (o il default)
//	 * @param args
//	 */
//	public static void main(String[] args) throws IOException {
//		for (int i = 100; i <= 1000; i += 100) {
//			String[] new_args = new String[args.length-2+6];
//			new_args[0] = "0";
//			new_args[1] = "0";
//			new_args[2] = "" + i;
//			new_args[3] = args[0];
//			new_args[5] = "";
//			if (args.length - 2 >= 0) System.arraycopy(args, 2, new_args, 6, args.length - 2);
//			try {
//				Files.createDirectories(Paths.get(args[1] + i + "/"));
//			} catch (IOException e) {
//				System.exit(1);
//			}
//
//			for (int j = 0; j < 20; j++) {
//				new_args[4] = args[1] + i + "/plans" + j + ".xml";
//
//				main_(new_args, null);
//			}
//		}
//	}

//	/**
//     * experiment 4
//     * Crea un file population secondo i parametri passati e lo salva nel path passato (o il default)
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		for (int emergencyBudget = 0; emergencyBudget <= 2000; emergencyBudget += 200) {
//			String[] new_args = new String[args.length-2+6];
//			new_args[0] = "0";
//			new_args[1] = "0";
//			new_args[2] = "" + 1000;
//			new_args[3] = args[0];
//			new_args[5] = "";
//			if (args.length - 2 >= 0) System.arraycopy(args, 2, new_args, 6, args.length - 2);
//			try {
//				Files.createDirectories(Paths.get(args[1] + emergencyBudget + "/"));
//			} catch (IOException e) {
//				System.exit(1);
//			}
//
//			for (int j = 0; j < 20; j++) {
//				new_args[4] = args[1] + emergencyBudget + "/plans" + j + ".xml";
//
//				main_(new_args, emergencyBudget);
//			}
//		}
//	}

	/**
     * experiment 2
     * Crea un file population secondo i parametri passati e lo salva nel path passato (o il default)
	 * @param args
	 */
	public static void main(String[] args) {
		int totalAgents = 1000;
		for (int i = 200; i < 1000; i += 200) {
			String[] new_args = new String[args.length-2+6];
			new_args[0] = "" + (totalAgents - i); //non-equipped
			new_args[1] = "0"; // emergency?
			new_args[2] = "" + i; //equipped
			new_args[3] = args[3]; //config path
			new_args[5] = "";
			// srcPos: indice array, incluso; dstPos: indice array, incluso; dst[dstPos] = src[srcPos];
			if (args.length - 2 >= 0) System.arraycopy(args, 2, new_args, 6, args.length - 2); // copia i parametri passati, per avere quelli nuovi all'inizio
			try {
				Files.createDirectories(Paths.get(args[1] + (i * 100 / totalAgents) + "/")); // crea le cartelle per ogni popolazione? e forse anche per gli output
			} catch (IOException e) {
				System.exit(1);
			}

			for (int j = 0; j < 5; j++) { // esegue l'esperimento più volte?
				new_args[4] = args[1] + (i * 100 / totalAgents) + "/plans" + j + ".xml";

				main_(new_args, null);
			}
		}
	}

/**
     * Crea un file population secondo i parametri passati e lo salva nel path passato (o il default)
	 * @param args passato come "new_args"
	 */
	public static void main_(String[] args, Integer emergencyBudget) {
		if (args.length < 2) {
			System.err.println("need the input number of agents and config file");
			System.exit(1);
		}
        // non-equipped, emergency?, equipped
		int[] agentsCollection = {Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2])};
		String configFile = args[3];
		String outputFile;
		if (args.length > 4) {
			outputFile = args[4];
		} else {
			outputFile = DEFAULT_OUTPUT_FILE;
		}
		String[] agentClassCollection = {StaticDriverLogic.class.getCanonicalName(),"",BidAgent.class.getCanonicalName()};
		if (args.length > 5) {
			agentClassCollection[1] = args[5];
		}
		
		Config config = ConfigUtils.loadConfig(configFile);
		//Network network = NetworkUtils.createNetwork();
		//MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		//netReader.readFile(networkFile);
		
		
		Population population = PopulationUtils.createPopulation(config);
		PopulationFactory factory = population.getFactory();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		//Population population = scenario.getPopulation();
		SignalsData signalsData = new SignalsDataLoader(config).loadSignalsData();
		
        // links dove ci sono dei semafori
		List<Id<Link>> signalLinks = signalsData.getSignalSystemsData().getSignalSystemData().values().stream()
				.map(SignalSystemData::getSignalData).flatMap(s -> s.values().stream())
				.map(SignalData::getLinkId).collect(Collectors.toList());
		
        // evita di aggiungere facilities dove ci sono dei semafori
		Collection<? extends Link> networkWithoutSignal = network.getLinks().values().stream()
														  		 .filter(link -> !signalLinks.contains(link.getId()))
														  		 .collect(Collectors.toSet());
		
		double routeDur = 0;
		double afterDur = 0;
		RoutingModule routing = getRouting(factory, network, scenario);
		ActivityFacilitiesFactoryImpl activityFac = new ActivityFacilitiesFactoryImpl();
		int k = 0; // id usato per creare l'istanza effettiva degli agenti, e mantenuta a livello "globale" per non avere duplicati
        // per ogni tipo di agente
		for (int j = 0; j < agentsCollection.length ; j++) {
			int agents = agentsCollection[j];
			String agentClass = agentClassCollection[j];
			for (int i=0; i< agents; i++,k++) {
				Link home;
				Link work;
				ActivityFacility facHome;
				ActivityFacility facWork;
				boolean found = false;
				
                // cerca una combinazione casa/lavoro random
				do {
					home = getRandomFromSet(networkWithoutSignal);
					work =  getRandomFromSet(networkWithoutSignal);
                    // skippa se sono la stessa
					if (home.equals(work)) {
						continue;
					}
                    // non sono facilty effettive, solo la posizione nella strada
					facHome = activityFac.createActivityFacility(null, home.getId());
					facWork = activityFac.createActivityFacility(null, work.getId());
					try {
						List<? extends PlanElement> morningPlan = routing.calcRoute(facHome, facWork, 0, null);
						List<? extends PlanElement> afterPlan = routing.calcRoute(facWork, facHome, 0, null);
						found = true;
                        // suppongo che ci siano solo una leg per raggiungere la destinazione?
                        // comunque prende qunato tempo serve per svolgere il viaggio
						routeDur = ((Leg)morningPlan.get(0)).getTravelTime();
						afterDur = ((Leg)afterPlan.get(0)).getTravelTime();
					} catch (RuntimeException e) {
						// se non esiste una route valida tra casa e lavoro, continua a cercare
					}
					
				} while (!found);
				
				//double workDur = getWorkDur();
                
                // tutti iniziano e finiscono di lavorare alla stessa ora
				double workStart = getWorkStart();
				double workEnd = getWorkEnd();
				double morningDep = workStart - routeDur;
	
				HashMap<String, String> otherAtts = new HashMap<>();
                // per gli agenti che partecipano attivamente all'asta, passa gli eventuali args aggiuntivi
				if (agentClass != StaticDriverLogic.class.getCanonicalName() && args.length > 6) {
					otherAtts = getOtherAtts(args, 12);
				}
				otherAtts.put("travelTime", ""+(routeDur+afterDur));
	
                // crea istanza agente e aggiungi gli attributi
				Person person = factory.createPerson(Id.createPersonId(k));
				person.getAttributes().putAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME, agentClass);
				for (Map.Entry<String, String> e : otherAtts.entrySet()) {
					/*if(j == 2 && e.getKey().equals("budget"))
						person.getAttributes().putAttribute(e.getKey(), Integer.toString(Integer.MAX_VALUE/2));
					else*/
						person.getAttributes().putAttribute(e.getKey(), e.getValue());
				}
                //home activity (prima e dopo lavoro)
				Plan plan = factory.createPlan();
				Activity homeAct1 = factory.createActivityFromLinkId(HOME_ACT, home.getId());
				homeAct1.setMaximumDuration(morningDep);
				Activity homeAct2 = factory.createActivityFromLinkId(HOME_ACT, home.getId());
                // work activity
				Activity workAct = factory.createActivityFromLinkId(WORK_ACT, work.getId());
				//workAct.setMaximumDuration(workDur); // differenza tra maxDuration e start/endTime ?
				workAct.setStartTime(workStart);
				workAct.setEndTime(workEnd);
				Leg legToWork = factory.createLeg(TransportMode.car);
				Leg legToHome = factory.createLeg(TransportMode.car);
				
				plan.addActivity(homeAct1);
				plan.addLeg(legToWork);
				plan.addActivity(workAct);
				plan.addLeg(legToHome);
				plan.addActivity(homeAct2);
				person.addPlan(plan);
				population.addPerson(person);
			}
		}

		//experiment 4
		if (emergencyBudget != null) {
			for (int x = 0; x < 4; x++) {
				Person p = getRandomFromSet(population.getPersons().values());
				p.getAttributes().putAttribute("budget", emergencyBudget.toString());
			}
		}
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outputFile);
		
	}

    /**
     * Splitta gli args e li aggiunge in una mappa.
     * Se sono array [], ne sceglie uno e lo usa in una distr normale per ottenere il valore
     *
     * @param i arg di partenza
     */
	private static HashMap<String, String> getOtherAtts(String[] args, int i) {
		HashMap<String, String> otherAtt = new HashMap<>();
		for (; i<args.length; i++){
			String[] splitted = args[i].split("=");
			String name = splitted[0];
			String value = splitted[1];

			if (value.startsWith("[")){
				//array di possibili valori
				String[] values = value.replace("[", "").replace("]", "").split(",");
				int chosen = new Random().nextInt(values.length);
				value = values[chosen];
				if (value.contains("-")){
					String[] minMax = value.split("-");
					int min = Integer.parseInt(minMax[0]);
					int max = Integer.parseInt(minMax[1]);
					//value = String.valueOf((new Random().nextInt()) + min);
					NormalDistribution dist = new NormalDistribution((double) (max - min) /2, 15);
					value = String.valueOf(dist.sample());
				}
			}

			otherAtt.put(name, value);
		}

		return otherAtt;
	}

	/**
	 * @return
	 */
	private static double getWorkStart() {
		NormalDistribution dist = new NormalDistribution(WORK_START_MEAN, WORK_VAR);
		return dist.sample();
	}

	/**
	 * @return
	 */
	private static double getWorkEnd() {
		NormalDistribution dist = new NormalDistribution(WORK_END_MEAN, WORK_VAR);
		return dist.sample();
	}

	/**
	 * @return
	 */
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

	/**
	 * @return
	 */
	private static String getRandomLogic() {
		Reflections reflections = new Reflections(SmartDriverLogic.class.getPackage().getName());
		 Set<Class<? extends SmartDriverLogic>> subTypes = 
		           reflections.getSubTypesOf(SmartDriverLogic.class);
		 Class<? extends SmartDriverLogic> type;
		 do {
			 type = getRandomFromSet(subTypes);
		 } while (Modifier.isAbstract(type.getModifiers()));
		 return type.getCanonicalName();
	}

	/**
	 * @param network 
	 * @return
	 */
	private static Link getRandomLink(Network network) {
		return getRandomFromSet(network.getLinks().values());
	}
	
	
	private static <T> T getRandomFromSet(Collection<T> set) {
		int max = set.size() - 1;
		//int n = MatsimRandom.getRandom().nextInt(max);
		UniformIntegerDistribution dist = new UniformIntegerDistribution(1, max);
		int n = dist.sample();
		return set.stream().collect(Collectors.toList()).get(n);
	}
}
