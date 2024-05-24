package org.matsim.contrib.smartcity.auctionIntersections;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionManagerProvider {

    @Inject
    private Injector inj;
    @Inject
    private Scenario scenario;

    private final String managerClass;
    private HashMap<Link, AuctionManager> managerMap = null;

    @Inject
    public AuctionManagerProvider(Config config){
        AuctionIntersectionConfigGroup configGroup = ConfigUtils.addOrGetModule(config, AuctionIntersectionConfigGroup.class);
        this.managerClass = configGroup.getParams().get(AuctionIntersectionConfigGroup.MANAGER_CLASS);;
    }

    private void makeManager(Collection<? extends Link> values) {
        ArrayList<Link> links = new ArrayList<>(values);
        AuctionManager manager = InstantationUtils.instantiateForNameWithParams(inj, managerClass, links);
        values.forEach(l -> managerMap.put(l, manager));
    }

    public AuctionManager getManager(Link link) {
        if (this.managerMap == null){
           this.managerMap = new HashMap<>();
            scenario.getNetwork().getNodes().values().forEach(n -> makeManager(n.getInLinks().values()));
        }

        return this.managerMap.get(link);
    }
}
