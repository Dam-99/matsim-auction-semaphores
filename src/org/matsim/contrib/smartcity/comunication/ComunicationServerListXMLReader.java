/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.smartcity.agent.parking.CameraPark;
import org.matsim.contrib.smartcity.agent.parking.ParksContainer;
import org.matsim.contrib.smartcity.perception.CamerasContainer;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import com.google.inject.Inject;

/**
 * Reader for server file
 * @author Filippo Muzzini
 *
 */
public class ComunicationServerListXMLReader extends MatsimXmlParser {
	
	public static final String SERVERTAG = "server";
	
	public static final String COMMTAG = "comunication";
	
	public static final String PERCTAG = "perception";
	
	public static final String COORDTAG = "coord";
	private static final String XATT = "x";
	private static final String YATT = "y";
	
	private static final String IDATT = "id";
	private static final String CLASSATT = "class";

	private static final String CAMERATAG = "camera";
	private static final String IDCAMERA = "id";
	
	private static final String ALL = "all";
	private static final String TRUE = "true";

	private static final String PARKCAMERATAG = "parkCamera";
	private static final String PARKTAG = "parkings";
	
	private HashMap<Tuple<String, String>, Server> servers = new HashMap<Tuple<String, String>, Server>();
	private Server actualServer;
	@Inject private Network network;
	@Inject private CamerasContainer camCont;
	@Inject private ParksContainer parkCont;
	
	public Set<Tuple<String, String>> getServerList() {
		return servers.keySet();
	}
	
	public Set<Coord> getServerCoord(Tuple<String, String> serverId) {
		return servers.get(serverId).getCoord();
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {

		switch (name) {
			case SERVERTAG:
				String serverId = atts.getValue(IDATT);
				String serverClass = atts.getValue(CLASSATT);
				actualServer = new Server();
				Tuple<String, String> key = new Tuple<String, String>(serverId, serverClass);
				servers.put(key, actualServer);
				break;
			case COORDTAG:
				double x = Double.parseDouble(atts.getValue(XATT));
				double y = Double.parseDouble(atts.getValue(YATT));
				Coord coord = new Coord(x,y);
				actualServer.addCoord(coord);
				break;
			case CAMERATAG:
				Id<Camera> id = Id.create(atts.getValue(IDCAMERA), Camera.class);
				actualServer.addCamera(id);
				break;
			case PARKCAMERATAG:
				Id<CameraPark> id_ = Id.create(atts.getValue(IDCAMERA), CameraPark.class);
				actualServer.addPark(id_);
				break;
			case COMMTAG:
				String all = atts.getValue(ALL);
				if (all.equals(TRUE)) {
					for (Link link : network.getLinks().values()) {
						actualServer.addCoord(link.getCoord());
					}
				}
				break;
			case PERCTAG:
				String all_ = atts.getValue(ALL);
				if (all_.equals(TRUE)) {
					for (Id<Camera> camera : camCont.getAllId()) {
						actualServer.addCamera(camera);
					}
				}
				break;
			case PARKTAG:
				String all__ = atts.getValue(ALL);
				if (all__.equals(TRUE)) {
					for (Id<CameraPark> camera : parkCont.getAllId()) {
						actualServer.addPark(camera);
					}
				}
				
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		switch (name) {
		case SERVERTAG:
			break;
		case COORDTAG:
			break;
	}

	}

	/**
	 * @param server
	 * @return
	 */
	public Set<Id<Camera>> getServerCameras(Tuple<String, String> server) {
		return servers.get(server).getCameras();
	}
	
	public Set<Id<CameraPark>> getServerParks(Tuple<String, String> server) {
		return servers.get(server).getParks();
	}
	
	private class Server {
		
		private Set<Coord> coord = new HashSet<Coord>();
		private Set<Id<Camera>> cameras = new HashSet<Id<Camera>>();
		private Set<Id<CameraPark>> parks = new HashSet<Id<CameraPark>>();
		
		public Set<Coord> getCoord() {
			return coord;
		}
		/**
		 * @return
		 */
		public Set<Id<CameraPark>> getParks() {
			return this.parks;
		}
		/**
		 * @param id_
		 */
		public void addPark(Id<CameraPark> id_) {
			parks.add(id_);
			
		}
		/**
		 * @param id
		 */
		public void addCamera(Id<Camera> id) {
			cameras.add(id);			
		}
		/**
		 * @param coord2
		 */
		public void addCoord(Coord coord2) {
			coord.add(coord2);			
		}
		public Set<Id<Camera>> getCameras() {
			return cameras;
		}
		
		
	}

}
