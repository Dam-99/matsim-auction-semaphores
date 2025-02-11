package org.matsim.contrib.smartcity.analisys;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.Lane;

import java.util.List;

public class SemaphorePredictionEvent extends Event {

    private static final String SEM_PRED_EVENT_TYPE = "BID_EVENT";
    private Id<Lane> actualLink;
    private List<Id<SignalGroup>> prediction;

    public SemaphorePredictionEvent(Id<Lane> actualLink, List<Id<SignalGroup>> prediction, double time) {
        super(time);

        this.actualLink = actualLink;
        this.prediction = prediction;
    }

    @Override
    public String getEventType() {
        return SEM_PRED_EVENT_TYPE;
    }

    public Id<Lane> getActualLink() {
        return actualLink;
    }

    public void setActualLink(Id<Lane> actualLink) {
        this.actualLink = actualLink;
    }

    public List<Id<SignalGroup>> getPrediction() {
        return prediction;
    }

    public void setPrediction(List<Id<SignalGroup>> prediction) {
        this.prediction = prediction;
    }
}
