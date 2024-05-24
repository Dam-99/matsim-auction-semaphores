/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

/**
 * Inteface for bizzantine agents that can transit without priority
 * @author Filippo Muzzini
 *
 */
public interface PriorityBizzantine extends Bizzantine {
	
	public static final double DEFAULT_PROB = 1;
	
	public boolean transitWithOutPriority();

}
