package org.matsim.contrib.smartcity.analisys;

import org.matsim.contrib.smartcity.auctionIntersections.AuctionManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AttendingIntersectionTime implements ShutdownListener {

    private static final String ATTENDING_INTERSECTION_TIME_FILENAME = "attending_intersection_time.txt";
    private List<AuctionManager.AttendingTime> attendingTimes = new ArrayList<>();

    @Inject
    private OutputDirectoryHierarchy controlerIO;

    Lock l = new ReentrantLock();
    private HashMap<String, Double> starts = new HashMap<String, Double>();

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            PrintWriter writer = new PrintWriter(controlerIO.getOutputFilename(ATTENDING_INTERSECTION_TIME_FILENAME));
            writer.write("Agent\tStart\tEnd\tTime\n");
            for (AuctionManager.AttendingTime t : this.attendingTimes) {
                //Assumento che il veicolo abbia lo stesso id dell'agente
                writer.write(t.getId()+"\t"+t.getStart()+"\t"+t.getEnd()+"\t"+t.getAttendingTime()+"\n");
            }
            writer.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void add(AuctionManager.AttendingTime attendingTime) {
        this.attendingTimes.add(attendingTime);
    }

    public void addStart(QVehicle veh, double now) {
        l.lock();
        String agent = veh.getId().toString();
        if (this.starts.containsKey(agent)){
            l.unlock();
            return;
        }

        this.starts.put(agent, now);
        l.unlock();
    }

    public void addEnd(QVehicle veh, double now) {
        l.lock();
        String agent = veh.getId().toString();
        Double start = this.starts.get(agent);

        AuctionManager.AttendingTime attendingTime = new AuctionManager.AttendingTime(agent, start, now);
        this.attendingTimes.add(attendingTime);

        this.starts.remove(agent);
        l.unlock();
    }
}
