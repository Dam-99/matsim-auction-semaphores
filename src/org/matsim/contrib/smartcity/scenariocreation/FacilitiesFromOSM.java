/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.XMLReader;



/**
 * Extract facilities and parking from OSM file
 * @author Filippo Muzzini
 *
 */
public class FacilitiesFromOSM {

	private static final String DEFAULT_FILE = "facilities.xml";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("need the input");
			System.exit(1);
		}
		File input = new File(args[0]);
		
		XMLReader reader = new XMLReader();
		
		MemoryStorage result = null;
		try {
			result = reader.read(input);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		Facilities fac = new Facilities(result);
		fac.write(DEFAULT_FILE);
		

	}

}
