/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;

import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.XMLReader;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * Extract priority from osm file
 * @author Filippo Muzzini
 *
 */
public class PriorityFromOSM {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("need the input osm and matsim xlm network");
			System.exit(1);
		}
		File input = new File(args[0]);
		String matsimXml = args[1];
		
		XMLReader reader = new XMLReader();
		
		MemoryStorage result = null;
		try {
			result = reader.read(input);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		netReader.readFile(matsimXml);
		
		Priorities priorities = new Priorities(result, network);
		priorities.write();

	}

}
