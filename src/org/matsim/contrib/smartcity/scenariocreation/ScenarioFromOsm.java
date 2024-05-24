/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.XMLReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.smartcity.comunication.ComunicationConfigGroup;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;
import org.matsim.contrib.smartcity.perception.PerceptionConfigGroup;
import org.matsim.contrib.smartcity.perception.wrapper.ActivePerceptionWrapperImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * Create a directory with files used by MATSim (smartcity module)
 * 
 * @author Filippo Muzzini
 *
 */
public class ScenarioFromOsm {
	
	private static final String DEFAULT_OUTPUT_DIR = "./scenario";
	private static final String NETWORK_FILE = "network.xml";
	private static final String FACILITIES_FILE = "facilities.xml";
	private static final String CONFIG_FILE = "config.xml";
	private static final long RANDOM_SEED = 4711;
	private static final String COORDINATE_SYSTEM_FROM = "WGS84";
	private static final String COORDINATE_SYTEM_TO = "EPSG:32632";
	private static final String TO_SET = "plans.xml";
	private static final String OUTPUT_DIR = "./output";
	private static final String HOME = "h";
	private static final String WORK = "w";
	private static final String DEFAULT_COMM_WRAPPER = ComunicationFixedWrapper.class.getCanonicalName();
	private static final String DEFAULT_RANGE = "1000";
	private static final String DEFAULT_SERVER_LIST = "serverList.xml";
	private static final String DEFAULT_PERC_WRAPPER = ActivePerceptionWrapperImpl.class.getCanonicalName();
	private static final String DEFAULT_CAMERA_FILE = "cameras.xml";
	
	private static ArrayList<ConfigGroup> configGroups = new ArrayList<ConfigGroup>();
	
	/**
	 * Run using specified osm file and controllerClass for signal controller, optionali can specified the
	 * output directory
	 * @param args OsmFile controllerClass outputDir
	 */
	@SuppressWarnings("unused")
	public static void main(String args[]) {
		if (args.length < 2) {
			System.err.println("need the input osm and controllerclass for signals");
			System.exit(1);
		}
		String osmFile = args[0];
		String controllerClass = args[1];
		String outputDir;
		if (args.length > 2) {
			outputDir = args[2];
		} else {
			outputDir = DEFAULT_OUTPUT_DIR;
		}
		
		boolean keepPaths = true;
		
		XMLReader readerXml = new XMLReader();
		MemoryStorage result = null;
		try {
			result = readerXml.read(new File(osmFile));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		//read network from osm file
		Network network = NetworkUtils.createNetwork();
		OsmNetworkReader reader = new OsmNetworkReaderWithReverse(network, new GeotoolsTransformation(COORDINATE_SYSTEM_FROM, COORDINATE_SYTEM_TO), true, false, keepPaths);
		reader.parse(osmFile);
		OsmNetworkReaderWithReverse.computeCap(network);

		Set<Id<Node>> nodeToRemove = new HashSet<Id<Node>>();
		for (Node n : network.getNodes().values()){
			if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0)
				nodeToRemove.add(n.getId());
		}
		for (Id<Node> n : nodeToRemove)
			network.removeNode(n);
		
		//add restrictions
		Restrictions res = new Restrictions(result, network);
		
		//add priority
		Priorities priorities = new Priorities(result, network);
		
		//construct facilities
		Facilities fac = new Facilities(result);
		
		//construct signals
		Signals sign = new Signals(result, network, controllerClass);
		
		//write output
		File outputDirFile = new File(outputDir);
		outputDirFile.mkdirs();
		
		//write netwrok file
		String networkFile = outputDirFile.getPath() + File.separator + NETWORK_FILE;
		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(networkFile);
		NetworkConfigGroup networkConfigGroup = new NetworkConfigGroup();
		networkConfigGroup.setInputFile(NETWORK_FILE);
		networkConfigGroup.setTimeVariantNetwork(true);
		configGroups.add(networkConfigGroup);		
		
		//write facilities file
		String facilitiesFile = outputDirFile.getPath() + File.separator + FACILITIES_FILE;
		fac.write(facilitiesFile);
		FacilitiesConfigGroup facilitiesConfigGroup = new FacilitiesConfigGroup();
		facilitiesConfigGroup.setInputFile(FACILITIES_FILE);
		configGroups.add(facilitiesConfigGroup);
		
		//write signals
		sign.writeFiles(outputDirFile.getPath());
		SignalSystemsConfigGroup signalConfigGroup = sign.getConfigGroup();
		configGroups.add(signalConfigGroup);
		
		//create config
		String configFile = outputDirFile.getPath() + File.separator + CONFIG_FILE;
		Config config = new Config();
		addBasicGroups(configGroups);
		for (ConfigGroup g : configGroups) {
			config.addModule(g);
		}
		ConfigUtils.writeMinimalConfig(config, configFile);
	}

	/**
	 * @param configGroups2
	 */
	private static void addBasicGroups(ArrayList<ConfigGroup> configGroups) {
		GlobalConfigGroup global = new GlobalConfigGroup();
		global.setRandomSeed(RANDOM_SEED);
		global.setCoordinateSystem(COORDINATE_SYTEM_TO);
		global.setNumberOfThreads(8);
		configGroups.add(global);
		
		PlansConfigGroup plans = new PlansConfigGroup();
		plans.setInputFile(TO_SET);
		configGroups.add(plans);
		
		ParallelEventHandlingConfigGroup event = new ParallelEventHandlingConfigGroup();
		event.setNumberOfThreads(8);
		configGroups.add(event);
		
		ControlerConfigGroup controler = new ControlerConfigGroup();
		controler.setOutputDirectory(OUTPUT_DIR);
		controler.setFirstIteration(0);
		controler.setLastIteration(10);
		controler.setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		controler.setLinkToLinkRoutingEnabled(true);
		configGroups.add(controler);
		
		TravelTimeCalculatorConfigGroup travelCalc = new TravelTimeCalculatorConfigGroup();
		travelCalc.setCalculateLinkToLinkTravelTimes(true);
		configGroups.add(travelCalc);
		
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setStartTime(0);
		qsim.setEndTime(0);
		qsim.setSnapshotPeriod(0);
		qsim.setUsingFastCapacityUpdate(false);
		qsim.setUseLanes(true);
		qsim.setNumberOfThreads(8);
		configGroups.add(qsim);
		
		PlanCalcScoreConfigGroup score = new PlanCalcScoreConfigGroup();
		score.getParams().clear();
		for (String s : score.getScoringParametersPerSubpopulation().keySet()){
			score.removeParameterSet(score.getOrCreateScoringParameters(s));
		}
		score.getOrCreateScoringParameters(null);
		score.getOrCreateScoringParameters(null);
		score.addModeParams(new ModeParams(TransportMode.car));
		score.addModeParams(new ModeParams(TransportMode.pt));
		score.addModeParams(new ModeParams(TransportMode.walk));
		score.setLearningRate(1.0);
		score.setBrainExpBeta(2.0);
		score.setLateArrival_utils_hr(-18);
		score.setEarlyDeparture_utils_hr(-0);
		score.setPerforming_utils_hr(+6);
		score.setMarginalUtlOfWaiting_utils_hr(0);
		score.addParam("traveling", "-6");
		ActivityParams home = new ActivityParams(HOME);
		home.setPriority(1);
		home.setTypicalDuration(43200);
		home.setMinimalDuration(28800);
		score.addActivityParams(home);
		ActivityParams work = new ActivityParams(WORK);
		work.setPriority(1);
		work.setTypicalDuration(28800);
		work.setMinimalDuration(21600);
		work.setOpeningTime(25200);
		work.setLatestStartTime(32400);
		work.setEarliestEndTime(0);
		work.setClosingTime(64800);		
		score.addActivityParams(work);
		configGroups.add(score);
		
		StrategyConfigGroup strategy = new StrategyConfigGroup();
		strategy.setMaxAgentPlanMemorySize(5);
		StrategySettings set1 = new StrategySettings();
		set1.setStrategyName("BestScore");
		set1.setWeight(0.9);
		strategy.addStrategySettings(set1);
		StrategySettings set2 = new StrategySettings();
		set2.setStrategyName("ReRoute");
		set2.setWeight(0.1);
		strategy.addStrategySettings(set2);
		configGroups.add(strategy);
		
		ComunicationConfigGroup comunication = new ComunicationConfigGroup();
		comunication.setWrapper(DEFAULT_COMM_WRAPPER);
		comunication.setRange(DEFAULT_RANGE);
		comunication.setServerList(DEFAULT_SERVER_LIST);
		configGroups.add(comunication);
		
		PerceptionConfigGroup perception = new PerceptionConfigGroup();
		perception.setWrapperClass(DEFAULT_PERC_WRAPPER);
		perception.setCameraFile(DEFAULT_CAMERA_FILE);
		configGroups.add(perception);
	}

}
