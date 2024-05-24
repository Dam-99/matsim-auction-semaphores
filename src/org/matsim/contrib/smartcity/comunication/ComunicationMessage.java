/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

/**
 * Message of comunication
 * @author Filippo Muzzini
 *
 */
public class ComunicationMessage {

	private ComunicationEntity sender;
	
	public ComunicationMessage(ComunicationEntity sender) {
		this.sender = sender;
	}


	public ComunicationEntity getSender() {
		return this.sender;
	}

}
