/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.util.ArrayList;

import org.alex73.osmemory.IOsmNode;
import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.FastArea;
import org.alex73.osmemory.geometry.GeometryHelper;
import org.alex73.osmemory.geometry.OsmHelper;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Represent a container for facilities including parking
 * @author Filippo Muzzini
 *
 */
public class Facilities {
	
	private static final String AMENITY_KEY = "amenity";
	private static final String PARKING = "parking";
	private static final String CAPACITY_KEY = "capacity";
	private static final double AREA_PARK_FACTOR = 97/(5.12e-7);
	private ActivityFacilities facilities;
	private MemoryStorage storage;
	//private ActivityFacilities parkings;
	private short amenity_tag;
	private short capacity_tag;

	public Facilities(MemoryStorage storage) {
		this.storage = storage;
		this.facilities = FacilitiesUtils.createActivityFacilities();
		//this.parkings = FacilitiesUtils.createActivityFacilities();
		amenity_tag = storage.getTagsPack().getTagCode(AMENITY_KEY);
		capacity_tag = storage.getTagsPack().getTagCode(CAPACITY_KEY);
		parse();
	}

	/**
	 * 
	 */
	private void parse() {
		//add parking
		storage.byTag(AMENITY_KEY, o -> o.getTag(amenity_tag).equals(PARKING),
				o -> {
					try {
						this.facilities.addActivityFacility(precessParking(o));
					} catch (RuntimeException e) {};
				});
		
		//add other facilities
		storage.byTag(AMENITY_KEY, o -> !o.getTag(amenity_tag).equals(PARKING),
				o -> {
					try {
						this.facilities.addActivityFacility(precessOthers(o));
					} catch (RuntimeException e) {};
				});
		
	}
	
	
	/**
	 * @param o
	 * @return
	 */
	private Facility precessOthers(IOsmObject o) {
		Point center;
		if (o instanceof IOsmNode) {
			IOsmNode n = ((IOsmNode) o);
			double lat = n.getLatitude();
			double lon = n.getLongitude();
			center = GeometryHelper.createPoint(lon, lat);
		} else {
			Geometry area = OsmHelper.areaFromObject(o, storage);
			center = area.getCentroid();
		}
		
		long id = o.getId();
		String type = o.getTag(amenity_tag);
		
		return new Facility(id, type, center);
		
	}

	/**
	 * @param o
	 * @return
	 */
	private Parking precessParking(IOsmObject o) {
		Geometry area = OsmHelper.areaFromObject(o, storage);
		
		long id = o.getId();
		Point center = area.getCentroid();
		int cap = 0;
		if (o.hasTag(capacity_tag)) {
			cap = Integer.parseInt(o.getTag(capacity_tag));
		} else {
			cap = (int) (area.getArea() * AREA_PARK_FACTOR);
		}
		
		//loading park links
		FastArea fastArea = new FastArea(area, storage);
		ArrayList<IOsmWay> links = new ArrayList<IOsmWay>();
		storage.all(ob -> {
			return ob.getType() == IOsmObject.TYPE_WAY && fastArea.covers(ob);
		}, ob -> {
			links.add((IOsmWay) ob);
		} );
		
		return new Parking(id, center, cap, links);
		
	}

	/**
	 * 
	 */
	public void write(String facilitiesFile) {
		//String file_fac = DEFAULT_OUT_DIR +File.separator+ DEFAULT_FAC_FILE;
		//String file_park = DEFAULT_OUT_DIR+File.separator+DEFAULT_PARK_FILE;
		writeFacilities(facilitiesFile);
		//writeParking(file_park);
	}

	/**
	 * @param file_park
	 */
	/*private void writeParking(String file) {
		FacilitiesWriter w = new FacilitiesWriter(parkings);
		w.write(file);
	}*/

	/**
	 * @param file
	 */
	private void writeFacilities(String file) {
		FacilitiesWriter w = new FacilitiesWriter(facilities);
		w.write(file);
	}


}
