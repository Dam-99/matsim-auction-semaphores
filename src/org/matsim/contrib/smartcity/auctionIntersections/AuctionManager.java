package org.matsim.contrib.smartcity.auctionIntersections;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.smartcity.agent.AbstractDriverLogic;
import org.matsim.contrib.smartcity.agent.BidAgent;
import org.matsim.contrib.smartcity.agent.SmartAgentLogic;
import org.matsim.contrib.smartcity.analisys.AttendingIntersectionTime;
import org.matsim.contrib.smartcity.perception.wrapper.PassivePerceptionWrapper;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AuctionManager {
    private List<QLaneI> qLanes;
    private final List<Link> links;

    private QNetwork qNetwork;
    @Inject
    private AttendingIntersectionTime attendingIntersectionTime;
    @Inject
    private PassivePerceptionWrapper perception;

    private HashMap<QVehicle, Double> attendStart = new HashMap<QVehicle, Double>();

    public AuctionManager(List<Link> links) {
        this.links = links;
    }

    protected abstract TurnAcceptanceLogic.AcceptTurn canPass(QVehicle veh, double now);

    public TurnAcceptanceLogic.AcceptTurn canPass_(QVehicle veh, double now, QNetwork qNetwork) {
        this.qNetwork = qNetwork;
        if (qLanes == null){
            this.qLanes = links.stream().map(l -> qNetwork.getNetsimLink(l.getId()).getAcceptingQLane()).collect(Collectors.toList());
        }

        return canPass(veh, now);
    }

    protected List<Bid> getBidsOfFirst(boolean getNVeh) {
        List<Tuple<QVehicle, QLaneI>> vheicles = qLanes.stream().filter(q -> !q.isNotOfferingVehicle())
                .map(q -> new Tuple<QVehicle, QLaneI>(q.getFirstVehicle(), q))
                .filter(t -> t.getFirst() != null).collect(Collectors.toList());
        List<Bid> bids = new ArrayList<>();
        for (Tuple<QVehicle, QLaneI> t : vheicles) {
            QVehicle v = t.getFirst();
            BidAgent agent = (BidAgent) ((SmartAgentLogic) ((DynAgent) v.getDriver()).getAgentLogic()).getActualLogic();
            int bid = agent.getBid();
            if (getNVeh){
                int nVehs = perception.getLinkTrafficStatus(v.getCurrentLink().getId()).getTotal();
                bids.add(new Bid(bid, agent, v, nVehs));
            } else {
                bids.add(new Bid(bid, agent, v));
            }
        }

        return bids;
    }

    protected void pass(QVehicle v, double time) {
        double start = this.attendStart.get(v);
        this.attendingIntersectionTime.add(new AttendingTime(v.getId().toString(), start, time));
        this.attendStart.remove(v);
    }

    protected void attend(QVehicle v, double time) {
        if (!this.attendStart.containsKey(v)) {
            this.attendStart.put(v, time);
        }
    }

    protected List<Bid> getBidsWithSponsor() {
        List<Tuple<QVehicle, QLaneI>> vheicles = qLanes.stream().filter(q -> !q.isNotOfferingVehicle())
                .map(q -> new Tuple<QVehicle, QLaneI>(q.getFirstVehicle(), q))
                .filter(t -> t.getFirst() != null).collect(Collectors.toList());
        List<Bid> bids = new ArrayList<>();
        for (Tuple<QVehicle, QLaneI> t : vheicles) {
            QVehicle v = t.getFirst();
            BidAgent agent = (BidAgent) ((SmartAgentLogic) ((DynAgent) v.getDriver()).getAgentLogic()).getActualLogic();
            Tuple<Integer, List<Tuple<AbstractDriverLogic, Integer>>> bid = agent.getBidWithSponsor();
            bids.add(new BidWithSponsor(bid.getFirst(), agent, v, bid.getSecond()));
        }

        return bids;
    }

    protected static class Bid {
        public final int bid;
        public final AbstractDriverLogic agent;
        public final QVehicle veh;
        public int nVeh;

        public Bid(int bid, AbstractDriverLogic agent, QVehicle veh){
            this.bid = bid;
            this.agent = agent;
            this.veh = veh;
            this.nVeh = 1;
        }

        public Bid(int bid, AbstractDriverLogic agent, QVehicle veh, int nVeh){
            this(bid, agent, veh);
            this.nVeh = nVeh;
        }
    }

    protected static class BidWithSponsor extends Bid {

        public final List<Tuple<AbstractDriverLogic, Integer>> sponsors;

        public BidWithSponsor(int bid, AbstractDriverLogic agent, QVehicle veh,
                              List<Tuple<AbstractDriverLogic, Integer>> sponsors) {
            super(bid, agent, veh);
            this.sponsors = sponsors;
        }
    }

    public static class AttendingTime {
        private final String id;
        private final double start;
        private final double end;
        private final double attendingTime;

        public AttendingTime(String id, double start, double end) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.attendingTime = end -start;
        }

        public String getId() {
            return id;
        }

        public double getStart() {
            return start;
        }

        public double getEnd() {
            return end;
        }

        public double getAttendingTime() {
            return attendingTime;
        }
    }
}
