package org.matsim.contrib.smartcity.auctionIntersections;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.comunication.ComunicationClient;
import org.matsim.contrib.smartcity.comunication.DecriseBudget;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;

import java.util.ArrayList;

public class CompAuctionManager extends CoopAuctionManager {

    public CompAuctionManager(ArrayList<Link> links) {
        super(links);
    }

    @Override
    protected TurnAcceptanceLogic.AcceptTurn canPass(QVehicle veh, double now) {
        if (this.crossList.size() == 0){
            makeAuction(now);
            this.crossList = this.crossList.subList(0,1);
        }

        if (this.crossList.indexOf(veh) == 0){
            ComunicationClient agent = (ComunicationClient) this.bidList.get(0).agent;
            this.crossList.remove(0);
            super.pass(veh, now);

            if (this.bidList.size() > 0){
                //il primo è passato
                //paga e il suo budget viene ridistribuito
                budgetRedistribution();
                this.bidList.clear();
            }

            return TurnAcceptanceLogic.AcceptTurn.GO;
        }

        return TurnAcceptanceLogic.AcceptTurn.WAIT;
    }

    protected void budgetRedistribution() {
        Bid winnerBid = this.bidList.get(0);
        BidAgent winner = (BidAgent) winnerBid.agent;
        winner.sendToMe(new DecriseBudget(null, winnerBid.bid));

        int nOthersAgents = this.bidList.size() - 1;
        for (Bid b : this.bidList.subList(1, this.bidList.size())){
            BidAgent agent = (BidAgent) b.agent;
            agent.sendToMe(new DecriseBudget(null, -(winnerBid.bid / nOthersAgents), BidAgent.REDISTRIBUTION));
        }
    }
}
