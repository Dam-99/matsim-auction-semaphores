/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import org.matsim.contrib.smartcity.agent.StaticDriverLogic;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Simple agent that can transit with red signal
 * @author Filippo Muzzini
 *
 */
public class RedBizzantineDriverLogic extends StaticDriverLogic implements BizzantineRedSignal {
	

	private static final double TRANSIT_PROB = 0.038;

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.accident.BizzantineRedSignal#transitOnRed()
	 */
	@Override
	public boolean transitOnRed() {
		double x = MatsimRandom.getRandom().nextDouble();
		return x <= TRANSIT_PROB;
	}

}
