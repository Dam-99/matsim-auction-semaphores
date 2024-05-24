package org.matsim.contrib.smartcity.auctionIntersections;

import com.google.inject.internal.cglib.core.$Signature;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.DecriseBudget;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoopAuctionManager extends AuctionManager {

    protected List<QVehicle> crossList = new ArrayList<>();
    protected List<Bid> bidList = new ArrayList<>();

    public CoopAuctionManager(ArrayList<Link> links) {
        super(links);
    }

    @Override
    protected TurnAcceptanceLogic.AcceptTurn canPass(QVehicle veh, double now) {
        if (this.crossList.size() == 0){
            makeAuction(now);
        }

        if (this.crossList.indexOf(veh) == 0){
            ComunicationClient agent = (ComunicationClient) this.bidList.get(0).agent;
            this.crossList.remove(0);
            super.pass(veh, now);

            if (this.crossList.size() == 0 && this.bidList.size() > 0) {
                //ho appena fatto passare tutti
                //pagamento
                payBids();
                //redistribuzione
                budgetRedistribution();
                this.bidList.clear();
            }

            return TurnAcceptanceLogic.AcceptTurn.GO;
        }

        return TurnAcceptanceLogic.AcceptTurn.WAIT;
    }

    private void payBids() {
        for (Bid b : this.bidList) {
            BidAgent agent = (BidAgent) b.agent;
            agent.sendToMe(new DecriseBudget(null, b.bid));
        }
    }

    private void budgetRedistribution() {
        int total_budget = this.bidList.stream().mapToInt(b -> b.bid).sum();
        int nAgents = this.bidList.size();
        for (Bid b : this.bidList) {
            BidAgent agent = (BidAgent) b.agent;
            agent.sendToMe(new DecriseBudget(null, -(total_budget / nAgents), BidAgent.REDISTRIBUTION));
        }
    }

    protected void makeAuction(double time) {
        List<Bid> bids = super.getBidsOfFirst(false);
        bids.sort((b1, b2) -> Integer.compare(b2.bid, b1.bid));
        this.bidList = bids;
        crossList = bids.stream().map(b -> b.veh).collect(Collectors.toList());
        for (QVehicle v : crossList){
            super.attend(v, time);
        }
    }
}
