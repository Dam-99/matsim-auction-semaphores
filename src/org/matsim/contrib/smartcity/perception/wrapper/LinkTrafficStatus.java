package org.matsim.contrib.smartcity.perception.wrapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleType;

/**
 * Class that represents a status of a link.
 * It count the number of vehicles on link and the number of types.
 * 
 * @author Filippo Muzzini
 *
 */
public class LinkTrafficStatus {
	private ConcurrentHashMap<Id<VehicleType>, Integer> status;
	private double lastUpdate;
	private final Id<VehicleType> UNKNOW_TYPE = Id.create("UNKNOW_TYPE", VehicleType.class);
	private Lock l = new ReentrantLock();
	
	public LinkTrafficStatus() {
		this.status = new ConcurrentHashMap<Id<VehicleType>, Integer>();
	}
	
	/**
	 * Increments the count of vehicles of this type on link
	 * 
	 * @param type vehicle type
	 */
	public void addVehicle(Id<VehicleType> type, double time) {
		l.lock();
		if (type == null)
			type = UNKNOW_TYPE;
		
		Integer actual = this.status.get(type);
		if (actual == null) {
			actual = 0;
		}
		actual++;
		
		this.status.put(type, actual);
		this.lastUpdate = time;
		l.unlock();
	}
	
	/**
	 * Decrements the count of vehicles of this type on link
	 * 
	 * @param type vehicle type
	 */
	public void subVehicle(Id<VehicleType> type, double time) throws SubOnNull {
		l.lock();
		if (type == null)
			type = UNKNOW_TYPE;
		
		Integer actual = this.status.get(type);
		if (actual == null) {
			l.unlock();
			throw new SubOnNull();
		}
		else
			actual--;
		
		this.status.put(type, actual);
		this.lastUpdate = time;
		l.unlock();
	}

	/**
	 * @return the toal number of vehicles on link
	 */
	public int getTotal() {
		int sum = 0;
		for(Integer partial : this.status.values()) {
			sum += partial;
		}
		return sum;
	}

	public double getLastUpdate() {
		return lastUpdate;
	}

	public Tuple<Double, Integer> getTotalSnap() {
		l.lock();
		Tuple<Double, Integer> res = new Tuple<>(getLastUpdate(), getTotal());
		l.unlock();
		return res;
	}

	/**
	 * Returns the number of vehicles of this type
	 * 
	 * @param idType type
	 * @return number of vehicles
	 */
	public int getTotalByType(Id<VehicleType> idType) {
		return this.status.get(idType);
	}
	
	
}
