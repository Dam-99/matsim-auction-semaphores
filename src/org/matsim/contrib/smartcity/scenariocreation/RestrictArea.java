/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.XMLReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Extract poligon from osm area
 * @author Filippo Muzzini
 *
 */
public class RestrictArea {

	private static final String AREA_TAG = "name";
	private static final String DEFAULT_OUTPUT = "polygon.txt";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("need the input file and area name");
			System.exit(1);
		}
		File input = new File(args[0]);
		String areaName = args[1];
		
		XMLReader reader = new XMLReader();
		
		MemoryStorage result = null;
		try {
			result = reader.read(input);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		GetAreaConsumer areaConsumer = new GetAreaConsumer(result);
		short areaTagId = result.getTagsPack().getTagCode(AREA_TAG);
		result.byTag(AREA_TAG, o -> o.getTag(areaTagId).equals(areaName), areaConsumer);
		//FastArea area = new FastArea(areaConsumer.getAreaPol(), result);
		
		String filename = DEFAULT_OUTPUT;
		writePol(filename, areaConsumer.getAreaPol());
		
		//result.all(o -> !area.covers(o), o -> remove(o, reader));

	}
	
	private static void writePol(String file, Geometry area) {
		File f = new File(file);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		writer.write("restricted_area\n");
		writer.write("1\n");
		for (Coordinate v : area.getCoordinates()) {
			//long tab lat
			String line = "\t" + v.x + "\t" + v.y + "\n";
			writer.write(line);
		}
		writer.write("END\n");
		writer.write("END\n");
		
		writer.close();
	}

}
