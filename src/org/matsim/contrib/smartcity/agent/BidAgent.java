package org.matsim.contrib.smartcity.agent;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.smartcity.actuation.semaphore.BidsSemaphoreController;
import org.matsim.contrib.smartcity.comunication.*;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationWrapper;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class BidAgent extends StaticDriverLogic implements ComunicationClient, ComunicationServer {

    public static final String REDISTRIBUTION = "REDISTRIBUTION";
    private static final String MODE_ATT = "mode";
    public static final String BUDGET_ATT = "budget";
    private static final String BID_MODE_ATT = "bidMode";
    public static final String SPONSORSHIP = "SPONSORSHIP";

    private final int originalBudget;
    private final String calcBidMode;
    private BidsSemaphoreController previousSem;

    @Inject
    private ComunicationWrapper wrapper;
    @Inject
    private Scenario scenario;
    @Inject
    private QSim qSim;

    private int budget;

    private BidAgentMode mode;
    private int staticBid;
    private int usedBudgetForSponsor = 0;
   
    public BidAgent(HashMap<String, Object> params) {
        super();
        this.mode = BidAgentMode.valueOf((String) params.get(MODE_ATT));
        this.originalBudget = (int) Float.parseFloat((String) params.get(BUDGET_ATT));
        this.calcBidMode = (String) params.get(BID_MODE_ATT);
        this.budget = this.originalBudget;
        if(this.budget == Integer.MAX_VALUE)
        	System.out.println();
    }
    
    @Override
    public Set<ComunicationServer> discover() {
        return this.wrapper.discover(actualLink);
    }

    @Override
    public void sendToMe(ComunicationMessage message) {
        if (message instanceof DecriseBudget) {
            if (SPONSORSHIP.equals(((DecriseBudget) message).getReason()) && !this.calcBidMode.equals("random")) {
                this.usedBudgetForSponsor += ((DecriseBudget) message).getAmount();
            } else if (REDISTRIBUTION.equals(((DecriseBudget) message).getReason())) {
                this.budget = this.budget - ((DecriseBudget) message).getAmount();
                int remainingNodes = this.getLinksList().size() - this.getActualIndex();
                this.staticBid = remainingNodes != 0 ? (int) (budget / remainingNodes) : budget;
            } else {
                this.budget = this.budget - ((DecriseBudget) message).getAmount();
                this.budget -= this.usedBudgetForSponsor;
                this.usedBudgetForSponsor = 0;
            }
        }
    }

    @Override
    public void setActualLink(Id<Link> actualLink) {
    	Id<Link> previousLink = super.actualLink;
    	if (previousLink != null && this.previousSem != null) {
    		if(this.agent.getId().toString().equals(Integer.toString(13896)))
        		System.out.println();
	    	this.previousSem.sendToMe(new CrossedMessage(this,this.getBid(),previousLink,this.qSim.getSimTimer().getTimeOfDay()));
	    	this.previousSem = null;
    	}
    	
        super.setActualLink(actualLink);

        //if remain in the same link don't bid
        if (actualLink.equals(previousLink))
            return;

        List<BidsSemaphoreController> sem = this.discover().stream().filter(s -> s instanceof BidsSemaphoreController)
                .map(s -> (BidsSemaphoreController) s)
                .filter(s -> s.controlLink(actualLink))
                .collect(Collectors.toList());

        if (sem.size() == 0) {
            return;
        }

        if (sem.size() > 1){
            System.err.println("TOO MUCH BID SEMAPHORE FOR THE LINK :" + actualLink);
            return;
        }
        
        //calcolo puntata
        int bid = calcBid();
        if(this.agent.getId().toString().equals(Integer.toString(20000)))
    		System.out.println();
        //mando puntata
        sem.get(0).sendToMe(new BidMessage(this, this.mode, bid, actualLink, qSim.getSimTimer().getTimeOfDay()));
        sem.get(0).sendToMe(new RideMessage(this, this.getActualIndex(), this.getLinksList(), qSim.getSimTimer().getTimeOfDay()));
        this.previousSem = sem.get(0);
    }

    @Override
    public void setLeg(Leg leg){
        super.setLeg(leg);
        this.budget = this.originalBudget;
        //this.nNodes = this.getLinksList().size() + 1;
        this.staticBid = nNodes != 0 ? (int) (budget / nNodes) : budget;
    }

    private int calcBid() {
        if (this.calcBidMode.equals("random")){
            if (this.budget > 0) {
                return new Random().nextInt(this.budget - this.usedBudgetForSponsor) + 1;
            } else {
                return 0;
            }
        }

        return this.staticBid - this.usedBudgetForSponsor;
//        int min;
//        int max;
//        switch (this.mode) {
//            case NR:
//                min = 0;
//                max = this.budget / 3;
//                break;
//            case N:
//                min = this.budget / 3;
//                max =  2 * this.budget / 3;
//                break;
//            case R:
//                min = 2 * this.budget / 3;
//                max = this.budget + 1;
//                break;
//            default:
//                min = 0;
//                max = this.budget + 1;
//        }
//
//        return min + new Random().nextInt(max - min);
    }

    public int getBid() {
        return calcBid();
    }

    public Tuple<Integer, List<Tuple<AbstractDriverLogic, Integer>>> getBidWithSponsor() {
        int myBid = calcBid();
        List<Tuple<AbstractDriverLogic, Integer>> sponsrs = wrapper
                .discover(this.actualLink != null ? this.actualLink : this.startLink)
                .stream().filter(Objects::nonNull).filter(s -> !s.equals(this))
                .map(a -> ((BidAgent) a).getSponsor((BidAgent) a, this.actualLink != null ? this.actualLink : this.startLink)).filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new Tuple<Integer, List<Tuple<AbstractDriverLogic, Integer>>>(myBid, sponsrs);
    }

    public Tuple<AbstractDriverLogic, Integer> getSponsor(BidAgent a, Id<Link> actualLink) {
        if (actualLink.equals(this.actualLink)){
            return new Tuple<AbstractDriverLogic, Integer>(this, calcSponsorBid());
        }

        return null;
    }

    private Integer calcSponsorBid() {
        if (this.calcBidMode.equals("random")){
            if (this.budget - this.usedBudgetForSponsor > 0)
                return new Random().nextInt(this.budget - this.usedBudgetForSponsor);
            else
                return 0;
        }
        double bound = Math.floor(0.5*this.staticBid) - this.usedBudgetForSponsor;
        return new Random().nextInt((int) bound);
    }

    public enum BidAgentMode {
        N,
        NR,
        R
    }
}
