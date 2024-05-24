package org.matsim.contrib.smartcity.analisys;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BidAnalyzer implements ShutdownListener, BidEventHandler {

    private static final String BID_ANALYZER_FILENAME = "bids.txt";

    ArrayList<String> bids = new ArrayList<String>();

    @Inject
    private OutputDirectoryHierarchy controlerIO;

//    @Inject
//    public BidAnalyzer(EventsManager eventsManager){
//        eventsManager.addHandler(this);
//    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            PrintWriter writer = new PrintWriter(controlerIO.getOutputFilename(BID_ANALYZER_FILENAME));
            writer.write("Link\tTime\tAgent\tBid\n");
            for (String t : this.bids) {
                //Assumento che il veicolo abbia lo stesso id dell'agente
                writer.write(t
                        +"\n");
            }
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void handleEvent(BidEvent e){
        String s = e.getLink() + "\t" + e.getTime() + "\t" + e.getAgent() + "\t" + e.getBid();
        this.bids.add(s);
    }

    @Override
    public void reset(int iteration) {
        bids = new ArrayList<String>();
    }
}
