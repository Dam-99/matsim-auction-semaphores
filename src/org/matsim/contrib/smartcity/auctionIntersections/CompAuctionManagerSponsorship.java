package org.matsim.contrib.smartcity.auctionIntersections;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.agent.AbstractDriverLogic;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.comunication.DecriseBudget;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompAuctionManagerSponsorship extends CompAuctionManager {

    public CompAuctionManagerSponsorship(ArrayList<Link> links) {
        super(links);
    }

    private void decriseSponsors(List<Tuple<AbstractDriverLogic, Integer>> sponsors) {
        for (Tuple<AbstractDriverLogic, Integer> s : sponsors){
            ((BidAgent) s.getFirst()).sendToMe(new DecriseBudget(null, s.getSecond(), BidAgent.SPONSORSHIP));
        }
    }

    @Override
    protected void budgetRedistribution() {
        BidWithSponsor winnerBid = (BidWithSponsor) this.bidList.get(0);
        BidAgent winner = (BidAgent) winnerBid.agent;
        winner.sendToMe(new DecriseBudget(null, winnerBid.bid));
        //decrise sponsors
        winnerBid.sponsors.forEach(s -> ((BidAgent) s.getFirst()).sendToMe(new DecriseBudget(null, s.getSecond(), BidAgent.SPONSORSHIP)));

        int totalWinnerBid = winnerBid.bid + winnerBid.sponsors.stream().mapToInt(Tuple::getSecond).sum();
        int nOthersAgents = this.bidList.size() - 1;
        for (Bid b : this.bidList.subList(1, this.bidList.size())){
            BidAgent agent = (BidAgent) b.agent;
            agent.sendToMe(new DecriseBudget(null, -(totalWinnerBid / nOthersAgents), BidAgent.REDISTRIBUTION));
        }
    }

    @Override
    protected void makeAuction(double time){
        List<Bid> bids = super.getBidsWithSponsor();
        bids.sort((b1, b2) -> Double.compare(computeSponsorship((BidWithSponsor) b2), computeSponsorship((BidWithSponsor) b1)));
        this.bidList = bids;
        crossList = bids.stream().map(b -> b.veh).collect(Collectors.toList());
        for (QVehicle v : crossList){
            super.attend(v, time);
        }
    }

    private double computeSponsorship(BidWithSponsor b) {
        return b.bid + b.sponsors.stream().map(Tuple::getSecond).mapToInt(Integer::intValue).sum();
    }
}
