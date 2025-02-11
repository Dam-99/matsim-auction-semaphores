package org.matsim.contrib.smartcity.comunication;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.lanes.Lane;

public class BidMessage extends ComunicationMessage {

    private final double time;
    private int bid;
    private Id<Lane> lane;
    private BidAgent.BidAgentMode mode;

    public BidMessage(ComunicationEntity sender, BidAgent.BidAgentMode mode, int bid, Id<Lane> lane, double time) {
        super(sender);
        this.bid = bid;
        this.lane = lane;
        this.mode = mode;
        this.time = time;
    }

    public int getBid() {
        return bid;
    }

    public Id<Lane> getLink() {
        return this.lane;
    }

    public BidAgent.BidAgentMode getMode() {
        return this.mode;
    }

    public double getTime(){
        return this.time;
    }
}
