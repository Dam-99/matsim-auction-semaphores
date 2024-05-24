/**
 * 
 */
package org.matsim.contrib.smartcity.accident;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.SmartNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.AccidentTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSignalsNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;

/**
 * Module that insert in MATSim the possibility of car accident
 * @author Filippo Muzzini
 *
 */
public class AccidentModule extends AbstractModule {

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		bind(TurnAcceptanceLogic.class).to(AccidentTurnAcceptanceLogic.class);
		addMobsimListenerBinding().to(CarAccidentNetworkChanger.class);
		addEventHandlerBinding().to(CarAccidentNetworkChanger.class);
		bind(CarAccidentNetworkChanger.class).asEagerSingleton();
		
		try {
			bind(QNetworkFactory.class).to(SmartNetworkFactory.class);
		} catch (RuntimeException e) {
			bind(QSignalsNetworkFactory.class).to(SmartNetworkFactory.class);
		}
		
		addMobsimListenerBinding().to(LinkAccident.class);
		addEventHandlerBinding().to(AccidentWriter.class);
		addControlerListenerBinding().to(AccidentWriter.class);
		bind(AccidentWriter.class).asEagerSingleton();
		
	}

}
