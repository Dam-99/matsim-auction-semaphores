/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.agent.parking.CameraPark;
import org.matsim.contrib.smartcity.agent.parking.ParksContainer;
import org.matsim.contrib.smartcity.perception.CamerasContainer;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Basic ServerFactory
 * @author Filippo Muzzini
 *
 */
public class ComunicationServerFactoryImpl implements ComunicationServerFactory {
	
	@Inject private Injector inj;
	@Inject private CamerasContainer camCont;
	@Inject private ParksContainer parksCont;

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationServerFactory#instantiateServer(java.lang.String, java.lang.String, java.util.Set)
	 */
	@Override
	public void instantiateServer(String serverId, String serverClass, Set<Coord> coord,
			Set<Id<Camera>> camerasId, Set<Id<CameraPark>> parksId) {		
		Set<Camera> cameras = camerasId.stream().map(id -> camCont.getCamera(id)).collect(Collectors.toSet());
		Set<CameraPark> parks = parksId.stream().map(id -> parksCont.getCamera(id)).collect(Collectors.toSet());
		ServerData data = new ServerData();
		data.cameras = cameras;
		data.coord = coord;
		data.parks = parks;
		Object[] params = {serverId, data};
		InstantationUtils.instantiateForNameWithParams(inj, serverClass, params);		
	}

}
