/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.smartcity.agent.parking.CameraPark;
import org.matsim.contrib.smartcity.perception.camera.Camera;

/**
 * Factory for server creation
 * @author Filippo Muzzini
 *
 */
public interface ComunicationServerFactory {

	/**
	 * @param serverId 
	 * @param serverClass java class of server
	 * @param coord set of coords of position of server appendices
	 */
	void instantiateServer(String serverId, String serverClass, Set<Coord> coord,
			Set<Id<Camera>> camerasId, Set<Id<CameraPark>> parksId);
	
	class ServerData {
		public Set<Coord> coord;
		public Set<Camera> cameras;
		public Set<CameraPark> parks;
	}

}
