/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.util.ArrayList;

import org.alex73.osmemory.IOsmWay;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.facilities.ActivityOption;
import com.vividsolutions.jts.geom.Point;

/**
 * Represent a parking
 * @author Filippo Muzzini
 *
 */
public class Parking extends Facility {

	private static final String PARKING_TYPE = ParkingUtils.PARKACTIVITYTYPE;


	public Parking(long id, Point coord, int cap, ArrayList<IOsmWay> links) {
		super(id, PARKING_TYPE, coord);
		ActivityOption option = this.getActivityOptions().get(PARKING_TYPE);
		option.setCapacity(cap);
		
		//vedere se più comodo tanti attributi o uno solo con la lista
		String linksString = "";
		for (IOsmWay link : links) {
			long linkId = link.getId();
			linksString = linksString.concat(linkId+";");
		}
		this.getAttributes().putAttribute("links", linksString.substring(0, linksString.length()-1));
	}

}
