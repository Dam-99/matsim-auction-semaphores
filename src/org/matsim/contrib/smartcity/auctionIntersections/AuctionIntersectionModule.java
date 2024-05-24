package org.matsim.contrib.smartcity.auctionIntersections;

import org.matsim.contrib.smartcity.analisys.AttendingIntersectionTime;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;

public class AuctionIntersectionModule extends AbstractModule {

    @Override
    public void install() {
        Config config = getConfig();
        if (!config.getModules().containsKey(AuctionIntersectionConfigGroup.GROUPNAME)) {
            bind(TurnAcceptanceLogic.class).to(PriorityTurnAcceptanceLogic.class);
        } else {
            ConfigUtils.addOrGetModule(config, AuctionIntersectionConfigGroup.class);
            bind(TurnAcceptanceLogic.class).to(AuctionBasedPriorityTurnAcceptanceLogic.class);
            bind(AuctionManagerProvider.class).asEagerSingleton();
        }

        bind(AttendingIntersectionTime.class).asEagerSingleton();
        addControlerListenerBinding().to(AttendingIntersectionTime.class);

        try {
            bind(QNetworkFactory.class).to(SmartNetworkFactory.class);
        } catch (RuntimeException e) {
            bind(QSignalsNetworkFactory.class).to(SmartNetworkFactory.class);
        }
    }
}
