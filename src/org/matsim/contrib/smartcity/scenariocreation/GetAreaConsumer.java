/**
 * 
 */
package org.matsim.contrib.smartcity.scenariocreation;

import java.util.function.Consumer;

import org.alex73.osmemory.IOsmObject;
import org.alex73.osmemory.IOsmWay;
import org.alex73.osmemory.MemoryStorage;
import org.alex73.osmemory.geometry.ExtendedWay;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Consumer that extract area from IOsmObject
 * @author Filippo Muzzini
 *
 */
public class GetAreaConsumer implements Consumer<IOsmObject> {
	
	private Geometry areaPol;
	private MemoryStorage storage;
	
	public GetAreaConsumer(MemoryStorage storage) {
		this.storage = storage;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(IOsmObject o) {
		ExtendedWay areaRel = new ExtendedWay((IOsmWay) o, storage);
		areaPol = areaRel.getArea();
	}
	
	public Geometry getAreaPol() {
		return areaPol;
	}
}
