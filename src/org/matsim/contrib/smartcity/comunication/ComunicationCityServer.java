/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.HashSet;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;

/**
 * @author Filippo Muzzini
 *
 */
public class ComunicationCityServer implements ComunicationServer {
	
	public ComunicationCityServer(ComunicationFixedWrapper wrapper, HashSet<Coord> positions) {
		wrapper.addFixedComunicator(this, positions);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationServer#sendToMe(org.matsim.contrib.smartcity.comunication.ComunicationMessage)
	 */
	@Override
	public void sendToMe(ComunicationMessage message) {
		System.out.println("Sono il server, ho ricevuto un messaggio");
	}

}
