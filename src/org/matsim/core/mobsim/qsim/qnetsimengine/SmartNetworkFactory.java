/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import com.google.inject.Inject;

/**
 * Factory for creations of network. Node are implemented using the TurnAcceptanceLogic specified in the
 * constructor
 * @author Filippo Muzzini
 *
 */
public class SmartNetworkFactory extends QSignalsNetworkFactory {

	private NetsimInternalInterface netsimEngine;
	private NetsimEngineContext context;
	private EventsManager events;
	private Scenario scenario;
	private TurnAcceptanceLogic logic;

	/**
	 * 
	 * @param scenario
	 * @param events
	 * @param logic
	 */
	@Inject
	public SmartNetworkFactory(Scenario scenario, EventsManager events, TurnAcceptanceLogic logic) {
		super(scenario, events);
		this.scenario = scenario;
		this.events = events;
		this.logic = logic;
	}
	
	@Override
	QNodeI createNetsimNode(Node node) {
		QNodeImpl.Builder builder = new QNodeImpl.Builder( netsimEngine, context ) ;
		builder.setTurnAcceptanceLogic(logic) ;
		return builder.build( node ) ;
	}
	
	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface simEngine1) {
		super.initializeFactory(agentCounter, mobsimTimer, simEngine1);
		
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( scenario.getConfig().qsim().getLinkWidthForVis() );
		linkWidthCalculator.setLaneWidth( scenario.getNetwork().getEffectiveLaneWidth() );
		
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		
		this.netsimEngine = simEngine1;
		this.context = new NetsimEngineContext( events, scenario.getNetwork().getEffectiveCellSize(), agentCounter, agentSnapshotInfoBuilder, 
				scenario.getConfig().qsim(), mobsimTimer, linkWidthCalculator );
	}

}
