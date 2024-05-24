package org.matsim.contrib.smartcity.auctionIntersections;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompAuctionManagerEnhacment extends CompAuctionManager {

    public CompAuctionManagerEnhacment(ArrayList<Link> links) {
        super(links);
    }

    @Override
    protected void makeAuction(double time){
        List<Bid> bids = super.getBidsOfFirst(true);
        bids.sort((b1, b2) -> Double.compare(computeEnc(b2), computeEnc(b1)));
        this.bidList = bids;
        crossList = bids.stream().map(b -> b.veh).collect(Collectors.toList());
        for (QVehicle v : crossList){
            super.attend(v, time);
        }
    }

    private double computeEnc(Bid b) {
        return b.bid * (Math.log(b.nVeh) + 1);
    }
}
