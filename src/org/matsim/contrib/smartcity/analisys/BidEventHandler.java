package org.matsim.contrib.smartcity.analisys;

import org.matsim.core.events.handler.EventHandler;

public interface BidEventHandler extends EventHandler {

    public void handleEvent(BidEvent bidEvent);
}
