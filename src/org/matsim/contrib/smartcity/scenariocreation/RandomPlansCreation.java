/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.lang.reflect.Modifier;
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

/**
 * @author Filippo Muzzini
 *
 */
public class RandomPlansCreation {

	private static final String DEFAULT_OUTPUT_FILE = "plans.xml";
	private static final String HOME_ACT = "h";
	private static final String WORK_ACT = "w";
	private static final double WORK_END_MEAN = 3600*18;
	private static final double WORK_VAR = 3600*1;
	private static final double WORK_START_MEAN = 3600*9;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("need the input number of agents and config file");
			System.exit(1);
		}
		int agents = Integer.parseInt(args[0]);
		String configFile = args[1];
		String outputFile;
		if (args.length > 2) {
			outputFile = args[2];
		} else {
			outputFile = DEFAULT_OUTPUT_FILE;
		}
		String agentClass = StaticDriverLogic.class.getCanonicalName();
		if (args.length > 3) {
			agentClass = args[3];
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
		
		List<Id<Link>> signalLinks = signalsData.getSignalSystemsData().getSignalSystemData().values().stream()
				.map(SignalSystemData::getSignalData).flatMap(s -> s.values().stream())
				.map(SignalData::getLinkId).collect(Collectors.toList());
		
		Collection<? extends Link> networkWithoutSignal = network.getLinks().values().stream()
														  		 .filter(link -> !signalLinks.contains(link.getId()))
														  		 .collect(Collectors.toSet());
		
		double routeDur = 0;
		RoutingModule routing = getRouting(factory, network, scenario);
		ActivityFacilitiesFactoryImpl activityFac = new ActivityFacilitiesFactoryImpl();		
		for (int i=0; i<agents; i++) {
			Link home;
			Link work;
			ActivityFacility facHome;
			ActivityFacility facWork;
			boolean found = false;
			
			do {
				home = getRandomFromSet(networkWithoutSignal);
				work =  getRandomFromSet(networkWithoutSignal);
				if (home.equals(work)) {
					continue;
				}
				facHome = activityFac.createActivityFacility(null, home.getId());
				facWork = activityFac.createActivityFacility(null, work.getId());
				try {
					List<? extends PlanElement> morningPlan = routing.calcRoute(facHome, facWork, 0, null);
					routing.calcRoute(facWork, facHome, 0, null);
					found = true;
					routeDur = ((Leg)morningPlan.get(0)).getTravelTime();
				} catch (RuntimeException e) {
					
				}
				
			} while (!found);
			
			//double workDur = getWorkDur();
			double workStart = getWorkStart();
			double workEnd = getWorkEnd();
			double morningDep = workStart - routeDur;

			HashMap<String, String> otherAtts = new HashMap<>();
			if (args.length > 4) {
				otherAtts = getOtherAtts(args, 4);
				otherAtts.put("travelTime", ""+routeDur);
			}

			Person person = factory.createPerson(Id.createPersonId(i));
			person.getAttributes().putAttribute(SmartAgentFactory.DRIVE_LOGIC_NAME, agentClass);
			for (Map.Entry<String, String> e : otherAtts.entrySet()) {
				person.getAttributes().putAttribute(e.getKey(), e.getValue());
			}
			Plan plan = factory.createPlan();
			Activity homeAct1 = factory.createActivityFromLinkId(HOME_ACT, home.getId());
			homeAct1.setMaximumDuration(morningDep);
			Activity homeAct2 = factory.createActivityFromLinkId(HOME_ACT, home.getId());
			Activity workAct = factory.createActivityFromLinkId(WORK_ACT, work.getId());
			//workAct.setMaximumDuration(workDur);
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
		
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outputFile);
		
	}

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
					NormalDistribution dist = new NormalDistribution((double) (max - min) /2, 1);
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
