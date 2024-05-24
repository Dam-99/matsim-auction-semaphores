package org.matsim.contrib.smartcity.actuation.semaphore;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.builder.SignalModelFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemImpl;
import org.matsim.contrib.smartcity.InstantationUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This factory create a instance of class indicated.
 * 
 * @author Filippo Muzzini
 *
 */
public class SmartSemaphoreModelFactory implements SignalModelFactory {
	
	@Inject Injector inj;

	@Override
	public SignalSystem createSignalSystem(Id<SignalSystem> id) {
		return new SignalSystemImpl(id);
	}

	@Override
	public SignalController createSignalSystemController(String controllerIdentifier, SignalSystem signalSystem) {
		SignalController signalControl = InstantationUtils.instantiateForName(inj, controllerIdentifier);
		
		signalControl.setSignalSystem(signalSystem);
		return signalControl;		
	}

	@Override
	public SignalPlan createSignalPlan(SignalPlanData planData) {
		return new DatabasedSignalPlan(planData);
	}

}
