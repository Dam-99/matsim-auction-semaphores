/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import com.vividsolutions.jts.geom.Point;

/**
 * Rappresent a facility
 * @author Filippo Muzzini
 *
 */
public class Facility extends ActivityFacilityImpl {

	/**
	 * @param id
	 * @param type
	 * @param coord
	 */
	public Facility(long id, String type, Point coord) {
		super(Id.create(id, ActivityFacility.class), new Coord(coord.getX(), coord.getY()), null);
		this.createAndAddActivityOption(type);
	}	
	

}
