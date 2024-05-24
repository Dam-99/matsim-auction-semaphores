package org.matsim.contrib.smartcity.comunication;

import com.google.inject.internal.cglib.core.$ClassNameReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.agent.BidAgent;

public class BidMessage extends ComunicationMessage {

    private final double time;
    private int bid;
    private Id<Link> link;
    private BidAgent.BidAgentMode mode;

    public BidMessage(ComunicationEntity sender, BidAgent.BidAgentMode mode, int bid, Id<Link> link, double time) {
        super(sender);
        this.bid = bid;
        this.link = link;
        this.mode = mode;
        this.time = time;
    }

    public int getBid() {
        return bid;
    }

    public Id<Link> getLink() {
        return this.link;
    }

    public BidAgent.BidAgentMode getMode() {
        return this.mode;
    }

    public double getTime(){
        return this.time;
    }
}
