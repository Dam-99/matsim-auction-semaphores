package org.matsim.contrib.smartcity.analisys;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.smartcity.agent.BidAgent;

public class BidEvent extends Event {


    private static final String BID_EVENT_TYPE = "BID_EVENT";
    private final Id<Link> link;
    private final Id<Person> agent;
    private final int bid;

    public BidEvent(Id<Link> link, BidAgent agent, int bid, double time) {
        super(time);

        this.link = link;
        this.agent = agent.getPerson().getId();
        this.bid = bid;
    }

    @Override
    public String getEventType() {
        return BID_EVENT_TYPE;
    }

    public Id<Link> getLink() {
        return link;
    }

    public Id<Person> getAgent() {
        return agent;
    }

    public int getBid() {
        return bid;
    }
}
