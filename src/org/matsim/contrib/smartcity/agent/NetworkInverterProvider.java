/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.util.HashMap;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilderI;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author Filippo Muzzini
 *
 */
public class NetworkInverterProvider {

	private final static HashMap<Tuple<Network, NetworkTurnInfoBuilderI>, NetworkInverter> net = new HashMap<Tuple<Network, NetworkTurnInfoBuilderI>, NetworkInverter>();
	
	public static NetworkInverter getInverted(Network network, NetworkTurnInfoBuilderI turn) {
		Tuple<Network, NetworkTurnInfoBuilderI> key = new Tuple<Network, NetworkTurnInfoBuilderI>(network, turn);
		NetworkInverter inverter = net.get(key);
		if (inverter == null) {
			inverter = new NetworkInverter(network, turn.createAllowedTurnInfos());
			net.put(key, inverter);
		}
		
		return inverter;
	}
	
}
