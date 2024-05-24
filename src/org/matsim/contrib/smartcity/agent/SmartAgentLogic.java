/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.StaticDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.search.RandomParkingSearchLogic;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

/**
 * This class define the agent behavior during day.
 * Using the specified logics return the activity and the leg.
 * 
 * @author Filippo Muzzini
 *
 */
/**
 * @author Filippo Muzzini
 *
 */
public class SmartAgentLogic implements DynAgentLogic {
	
	public static final String STOPPING_STRING = "STOPPING";
	
	private SmartDriverLogic smartDriverLogic;
	private DynAgent agent;
	private ParkingSearchLogic parkingLogic;
	private Iterator<PlanElement> planIter;
	
	@Inject private ParkingSearchManager parkingManager;
	@Inject private WalkLegFactory walkLegFactory;

	private SmartAgentLogicState state;

	private Activity act;

	private Id<Link> parkedLink;

	private Leg lastLeg;

	/**
	 * Construct the agent with the specified logic.
	 * 
	 * @param plan1 Plan for PersonDriverAgentImpl
	 * @param smartDriverLogic the agent's drive logic
	 * @param parkingLogic the agent's parking logic
	 */
	public SmartAgentLogic(Plan plan1, Netsim simulation, SmartDriverLogic smartDriverLogic, ParkingSearchLogic parkingLogic) {
		this.planIter = plan1.getPlanElements().iterator();
		
		if (smartDriverLogic == null) {
			smartDriverLogic = new StaticDriverLogic();
		}
		
		if (parkingLogic == null) {
			parkingLogic = new RandomParkingSearchLogic(simulation.getNetsimNetwork().getNetwork());
		}
		
		this.smartDriverLogic = smartDriverLogic;
		this.parkingLogic = parkingLogic;
	}
	
	public SmartAgentLogic(Plan plan1, Netsim simulation) {
		this(plan1, simulation, null, null);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynAgentLogic#getDynAgent()
	 */
	@Override
	public DynAgent getDynAgent() {
		return this.agent;
	}
	
	public Object getActualLogic() {
		if (this.state == SmartAgentLogicState.PARKING_SEARCH) {
			return this.parkingLogic;
		} else {
			return this.smartDriverLogic;
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynAgentLogic#computeInitialActivity(org.matsim.contrib.dynagent.DynAgent)
	 */
	@Override
	public DynActivity computeInitialActivity(DynAgent dynAgent) {
		this.agent = dynAgent;
		PlanElement planElement = this.planIter.next();
		Activity act = (Activity) planElement;
		DynActivity dynAct = getDynActivity(act, 0);
		this.state = SmartAgentLogicState.ACTIVITY;
		this.parkedLink = this.agent.getCurrentLinkId();
		return dynAct;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dynagent.DynAgentLogic#computeNextAction(org.matsim.contrib.dynagent.DynAction, double)
	 */
	@Override
	public DynAction computeNextAction(DynAction oldAction, double now) {
		switch (this.state) {
			case  PARKING_SEARCH: {
				this.parkedLink = this.agent.getCurrentLinkId();
				this.state = SmartAgentLogicState.WALK_TO_ACTIVITY;
				return createWalkLeg(this.parkedLink, this.act.getLinkId(), now);
			}
		
			case WALK_TO_ACTIVITY: {
				this.state = SmartAgentLogicState.ACTIVITY;
				return getDynActivity(this.act, now);
			}
			
			case ACTIVITY: {
				this.state = SmartAgentLogicState.WALK_TO_PARKED_LINK;
				return createWalkLeg(this.agent.getCurrentLinkId(), this.parkedLink, now);
			}
			
			default: {
				PlanElement planElement = this.planIter.next();
				return processPlanElement(planElement, now);
			}
		}
	}
	
	/**
	 * Create a walk leg (e.g. walk to activity)
	 * 
	 * @param start starting link id
	 * @param end ending link id
	 * @param sartTime starting time
	 * @return Leg with walking mode
	 */
	private DynAction createWalkLeg(Id<Link> start, Id<Link> end, double startTime) {
		Leg walkLeg = this.walkLegFactory.createWalkLeg(start, end, startTime, TransportMode.egress_walk);
		return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
	}

	/**
	 * process the plan element and return the appropriate action
	 * 
	 * @param planElement plan element
	 * @param now actual time
	 * @return the appropriate action
	 * @throws IllegalActionExpection if planElement is incorrect
	 */
	private DynAction processPlanElement(PlanElement planElement, double now) throws IllegalActionExpection {
		if (planElement == null)
			return null;
		
		if (planElement instanceof Activity) {
			Activity act = (Activity) planElement;
			
			//if it is a stopping activity it can be done
			if (act.getType().contains(STOPPING_STRING)){
				this.state = SmartAgentLogicState.STOPPING_ACTIVITY;
				return getDynActivity(act, now);
			}
			
			//else it must search parking
			this.state = SmartAgentLogicState.PARKING_SEARCH;
			this.act = act;		
			return new SmartParkingDynLeg(parkingLogic, parkingManager, lastLeg, this.agent.getCurrentLinkId());
		} else if (planElement instanceof Leg) {
			this.state = SmartAgentLogicState.LEG;
			Leg leg = (Leg) planElement;
			this.lastLeg = leg;
			return new SmartDriverDynLeg(leg, smartDriverLogic);
		} else {
			throw new IllegalActionExpection(planElement.getClass());
		}		
			
	}
	
	/**
	 *  Create a DynActivity based on Activity
	 * 
	 *  @param act the Activity
	 *  @param now actual time
	 *  @return the DynActivity
	 */
	@SuppressWarnings("deprecation")
	private DynActivity getDynActivity(Activity act, double now) {
		double endTime =act.getEndTime() ; 
		double dur = act.getMaximumDuration();
		if (endTime == Time.UNDEFINED_TIME){
			endTime = Double.POSITIVE_INFINITY;
		}
		
		if (dur != Time.UNDEFINED_TIME && endTime == Double.POSITIVE_INFINITY) {
			endTime = now + dur;
		}
		
		return new StaticDynActivity(act.getType(), endTime);
	}
	
	protected enum SmartAgentLogicState {
		PARKING_SEARCH, WALK_TO_ACTIVITY, ACTIVITY
	, WALK_TO_PARKED_LINK, STOPPING_ACTIVITY, LEG}


}
