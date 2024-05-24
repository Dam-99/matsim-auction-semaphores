/**
 * 
 */
package org.matsim.contrib.smartcity.perception;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.smartcity.agent.parking.ParkData;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.facilities.ActivityFacility;
import org.xml.sax.Attributes;

import com.google.inject.Inject;

/**
 * Reader for cameras' xml
 * @author Filippo Muzzini
 *
 */
public class CameraXMLReader extends MatsimXmlParser {
	private static final String CAMERA_TAG = "camera";
	private static final String CAMERA_ID = "id";
	private static final String CAMERA_LINK = "link";
	private static final String PARK_ID = "park";
	private static final String CAMERA_CLASS = "class";
	private static final String CAMERAS_TAG = "cameras";
	private static final String PARKS_TAG = "parkings";
	private static final String ALL = "all";
	private static final String TRUE = "true";
	
	private List<CameraData> cameraList = new ArrayList<CameraData>();
	private List<ParkData> parkList = new ArrayList<ParkData>();
	@Inject private Network network;
	@Inject private Scenario scenario;
	
	private TypeList actualList;
	
	private enum TypeList {
		Cameras, Parkings;
	}
	

	/**
	 * @return list of cameras
	 */
	public List<CameraData> getCameraList() {
		return cameraList;
	}
	
	public List<ParkData> getParkList(){
		return this.parkList;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		switch (name) {
			case CAMERA_TAG:
				String cameraClass = atts.getValue(CAMERA_CLASS);
				String cameraId = atts.getValue(CAMERA_ID);
				switch (actualList) {
				 case Cameras:
					 String cameraLink = atts.getValue(CAMERA_LINK);
					 CameraData cameraData = new CameraData(cameraClass, cameraLink, cameraId);
					 this.cameraList.add(cameraData);
					 break;
				 case Parkings:
					 String parkId = atts.getValue(PARK_ID);
					 ParkData parkData = new ParkData(cameraClass, parkId, cameraId);
					 this.parkList.add(parkData);
					 break;
				}
				break;
			case CAMERAS_TAG:
				this.actualList = TypeList.Cameras;
				String all = atts.getValue(ALL);
				String cameraClass_ = atts.getValue(CAMERA_CLASS);
				if (TRUE.equals(all)) {
					for (Link link : network.getLinks().values()) {
						CameraData cameraData_ = new CameraData(cameraClass_, link.getId().toString(), link.getId().toString());
						this.cameraList.add(cameraData_);
					}
				}
				break;
			case PARKS_TAG:
				this.actualList = TypeList.Parkings;
				String all_ = atts.getValue(ALL);
				String cameraClass__ = atts.getValue(CAMERA_CLASS);
				if (TRUE.equals(all_)) {
					TreeMap<Id<ActivityFacility>, ActivityFacility> parkings = scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE);
					for (Id<ActivityFacility> park : parkings.keySet()) {
						ParkData parkData = new ParkData(cameraClass__, park.toString(), park.toString());
						this.parkList.add(parkData);
					}
				}
				
		}
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
	}

}
