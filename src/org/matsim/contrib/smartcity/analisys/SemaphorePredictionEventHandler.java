package org.matsim.contrib.smartcity.analisys;

import org.matsim.core.events.handler.EventHandler;

public interface SemaphorePredictionEventHandler extends EventHandler {

    public void handleEvent(SemaphorePredictionEvent event);
}
