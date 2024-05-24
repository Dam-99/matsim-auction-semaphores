/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;
import org.matsim.contrib.smartcity.perception.wrapper.PassivePerceptionWrapper;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.charts.BarChart;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class FlowWriter implements MobsimAfterSimStepListener, ShutdownListener{

	private static final String FLOW_NAME = "maxFlow.txt";

	private static final String PNG_NAME = "maxFlow.png";
	
	private HashMap<Double, Integer> dayStatus = new HashMap<Double, Integer>();
	@Inject private PassivePerceptionWrapper wrapper;
	@Inject private OutputDirectoryHierarchy controlerIO;

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener#notifyMobsimBeforeSimStep(org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		int max = 0;
		for (LinkTrafficStatus linkStatus: wrapper.getTrafficMap().values()) {
			int totalLink = linkStatus.getTotal();
			if (totalLink > max) {
				max = totalLink;
			}
		}
		this.dayStatus.put(e.getSimulationTime(), max);
	}


	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.ShutdownListener#notifyShutdown(org.matsim.core.controler.events.ShutdownEvent)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}
		String filename = Controler.OUTPUT_PREFIX + FLOW_NAME;
		String header = "Time\tMaxFlow";
		PrintStream writer = null;
		try {
			writer = new PrintStream(controlerIO.getOutputFilename(filename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println(header);
		
		//BarChart chart = new BarChart("MaxFlow during day", "time", "maxFlow");
		//double[] bars = new double[dayStatus.size()];
		
		//int i=0;
		for (Entry<Double, Integer> e : dayStatus.entrySet()) {
			Double time = e.getKey();
			Integer flow = e.getValue();
			writer.println(time+"\t"+flow);
			//bars[i] = (double)flow;
			//i++;
		}
		
		//String pngName = Controler.OUTPUT_PREFIX + PNG_NAME;
		//chart.addSeries("maxFlow", bars);
		//chart.saveAsPng(controlerIO.getOutputFilename(pngName), 800, 600);
		
		//writer.close();
	}
	
}
