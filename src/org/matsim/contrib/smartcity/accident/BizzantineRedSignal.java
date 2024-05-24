/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

/**
 * Inteface for bizzantine agents that can transit with red signals
 * @author Filippo Muzzini
 *
 */
public interface BizzantineRedSignal extends Bizzantine{
	
	public static final double DEFAULT_PROB = 0.53;
	
	/**
	 * Method called to ask if the agent transit or no.
	 * @return true if the agent transit
	 */
	public boolean transitOnRed();

}
