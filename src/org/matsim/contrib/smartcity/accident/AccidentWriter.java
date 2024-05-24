/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * @author Filippo Muzzini
 *
 */
public class AccidentWriter implements AccidentEventHandler, ShutdownListener{

	private PrintWriter writer;
	
	public AccidentWriter() {
		try {
			writer = new PrintWriter("accident.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.accident.AccidentEventHandler#handleEvent(org.matsim.contrib.smartcity.accident.CarAccidentEvent)
	 */
	@Override
	public void handleEvent(CarAccidentEvent e) {
		String str = "Accident on link: " + e.getFromId() + " at: " + e.getTime();
		writer.println(str);

	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.ShutdownListener#notifyShutdown(org.matsim.core.controler.events.ShutdownEvent)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writer.close();
		
	}


}
