/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import java.util.Set;

import org.alex73.osmemory.MemoryStorage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.ActionOnSignalSpecsViolation;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.actuation.semaphore.SmartSemaphoreController;

/**
 * Container that extract singal info from osm storage
 * @author Filippo Muzzini
 *
 */
public class Signals {

	private static final String HIGHWAY_KEY = "highway";
	private static final Object SIGNAL = "traffic_signals";
	private static final String DEFAULT_SYSTEM_FILE = "SignalsSystem.xml";
	private static final String DEFAULT_CONTROLLER = SmartSemaphoreController.class.getCanonicalName();
	private static final String DEFAULT_CONTROL_FILE = "SignalControl.xml";
	private static final String DEFAULT_GROUPS_FILE = "SignalGroups.xml";
	
	private MemoryStorage storage;
	private SignalSystemsData systemdata;
	private short highway_tag;
	private SignalSystemsDataFactory systemfactory;
	private SignalControlData controldata;
	private SignalControlDataFactory controlfactory;
	private String controllerIdentifier;
	private Network network;
	private SignalGroupsData groupsdata;
	private SignalGroupsDataFactory groupsfactory;

	/**
	 * @param storage
	 * @param network 
	 */
	public Signals(MemoryStorage storage, Network network, String controllerIdentifier) {
		this.storage = storage;
		this.network = network;
		try {
			this.controllerIdentifier = InstantationUtils.foundClassName(controllerIdentifier);
		} catch (NullPointerException e) {
			this.controllerIdentifier = DEFAULT_CONTROLLER;
		}
		this.systemdata = new SignalSystemsDataImpl();
		this.controldata = new SignalControlDataImpl();
		this.groupsdata = new SignalGroupsDataImpl();
		this.systemfactory = this.systemdata.getFactory();
		this.controlfactory = this.controldata.getFactory();
		this.groupsfactory = this.groupsdata.getFactory();
		highway_tag = storage.getTagsPack().getTagCode(HIGHWAY_KEY);
		parse();
	}
	
	public Signals(MemoryStorage storage, Network network) {
		this(storage, network, DEFAULT_CONTROLLER);
	}

	/**
	 * 
	 */
	private void parse() {
		storage.byTag(HIGHWAY_KEY, o -> o.getTag(highway_tag).equals(SIGNAL),
				o -> createSignals(o.getId()));		
	}

	/**
	 * @param id
	 */
	private void createSignals(long id_) {
		Id<Node> nodeId = Id.createNodeId(id_);
		Id<SignalSystem> id = Id.create(id_, SignalSystem.class);
		SignalSystemData sData = systemfactory.createSignalSystemData(id);
		
		Node node = network.getNodes().get(nodeId);
		if (node == null){
			System.out.println("SEMAFORO OMESSO PERCHE' NON ASSOCIATO A NESSUNA STRADA!!! ("+nodeId+")");
			return;
		}
		Set<Id<Link>> links = node.getInLinks().keySet();
		for (Id<Link> idLink : links) {
			SignalData signal = this.systemfactory.createSignalData(Id.create(idLink, Signal.class));
			signal.setLinkId(Id.createLinkId(idLink));
			sData.addSignalData(signal);
			
			Id<SignalGroup> groupId = Id.create(idLink, SignalGroup.class);
			SignalGroupData group = this.groupsfactory.createSignalGroupData(id, groupId);
			group.addSignalId(signal.getId());
			this.groupsdata.addSignalGroupData(group);
		}
		
		this.systemdata.addSignalSystemData(sData);
		
		SignalSystemControllerData control = this.controlfactory.createSignalSystemControllerData(id);
		control.setControllerIdentifier(controllerIdentifier);
		this.controldata.addSignalSystemControllerData(control);
	}

	/**
	 * 
	 */
	public void writeFiles(String dir) {
		String filename_system = dir + File.separator + DEFAULT_SYSTEM_FILE;
		SignalSystemsWriter20 writer = new SignalSystemsWriter20(this.systemdata);
		writer.write(filename_system);	
		
		String filename_control = dir + File.separator + DEFAULT_CONTROL_FILE;
		SignalControlWriter20 controlWriter = new SignalControlWriter20(this.controldata);
		controlWriter.write(filename_control);
		
		String filename_groups = dir + File.separator + DEFAULT_GROUPS_FILE;
		SignalGroupsWriter20 groupsWriter = new SignalGroupsWriter20(this.groupsdata);
		groupsWriter.write(filename_groups);
		
	}


	public SignalSystemsConfigGroup getConfigGroup() {
		SignalSystemsConfigGroup config = new SignalSystemsConfigGroup();
		config.setActionOnIntergreenViolation(ActionOnSignalSpecsViolation.WARN);
		config.setAmberTimesFile(null);
		config.setIntergreenTimesFile(null);
		config.setSignalControlFile(DEFAULT_CONTROL_FILE);
		config.setSignalGroupsFile(DEFAULT_GROUPS_FILE);
		config.setSignalSystemFile(DEFAULT_SYSTEM_FILE);
		config.setUseAmbertimes(false);
		config.setUseIntergreenTimes(false);
		config.setUseSignalSystems(true);
		
		return config;
	}

}
