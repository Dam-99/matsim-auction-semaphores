/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import org.matsim.core.events.handler.EventHandler;

/**
 * Handler for CarAccident event
 * @author Filippo Muzzini
 *
 */
public interface AccidentEventHandler extends EventHandler {

	public void handleEvent(CarAccidentEvent e);
}
