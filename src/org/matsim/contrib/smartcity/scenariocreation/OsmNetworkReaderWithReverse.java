/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * Reader that add the direction attribute on links
 * @author Filippo Muzzini
 *
 */
public class OsmNetworkReaderWithReverse extends OsmNetworkReader {

	static final String DIRECTION_ATT = "direction";
	static final String FORWARD = "forward";
	static final String BACKWARD = "backward";

	/**
	 * @param network
	 * @param transformation
	 * @param useHighwayDefaults
	 * @param useVspAdjustments
	 */
	public OsmNetworkReaderWithReverse(Network network, CoordinateTransformation transformation,
			boolean useHighwayDefaults, boolean useVspAdjustments, boolean keepPaths) {
		super(network, transformation, useHighwayDefaults, useVspAdjustments);
		this.setHighwayDefaults(9, "service", 1,  10.0/3.6, 1.0,  300);
		setKeepPaths(keepPaths);
	}
	
	@Override
	protected void setOrModifyLinkAttributes(Link l, OsmWay way, boolean forwardDirection) {
		String direction = forwardDirection ? FORWARD : BACKWARD;
		l.getAttributes().putAttribute(DIRECTION_ATT, direction);
	}
	
	public static void computeCap(Network network) {
		for (Link link : network.getLinks().values()) {
			double lenght = link.getLength();
			double nlanes = link.getNumberOfLanes();
			link.setCapacity(lenght * nlanes / 4.0 * network.getCapacityPeriod()); 
		}
	}

}
