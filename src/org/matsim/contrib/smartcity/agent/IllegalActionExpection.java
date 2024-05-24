/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author Filippo Muzzini
 *
 */
public class IllegalActionExpection extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<? extends PlanElement> classType;

	/**
	 * @param class1
	 */
	public IllegalActionExpection(Class<? extends PlanElement> class1) {
		this.classType = class1;
	}
	
	public String toString() {
		return "Class "+this.classType.getName()+" is not a Leg or Activity";
	}

}
