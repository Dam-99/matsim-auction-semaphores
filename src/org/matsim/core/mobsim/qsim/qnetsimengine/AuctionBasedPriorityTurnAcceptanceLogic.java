package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.auctionIntersections.AuctionManager;
import org.matsim.contrib.smartcity.auctionIntersections.AuctionManagerProvider;

public class AuctionBasedPriorityTurnAcceptanceLogic implements TurnAcceptanceLogic {

    private AuctionManagerProvider provider;

    @Inject
    public AuctionBasedPriorityTurnAcceptanceLogic(AuctionManagerProvider provider) {
        this.provider = provider;
    }

    @Override
    public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh,
                                      QNetwork qNetwork, double now) {
        AuctionManager manager = provider.getManager(currentLink);
        return manager.canPass_(veh, now, qNetwork);
    }
}
