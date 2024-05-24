/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

/**
 * @author Filippo Muzzini
 *
 */
public interface ComunicationEntity {

	/**
	 * Called by entities that want send a message
	 * @param message
	 */
	public void sendToMe(ComunicationMessage message);
	
}
