package org.matsim.contrib.smartcity.perception;

import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.perception.wrapper.ActivePerceptionWrapper;
import org.matsim.contrib.smartcity.perception.wrapper.PassivePerceptionWrapper;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
 * Module that instantiates the wrapper using reflection
 * and bind the class to the implemented interfaces.
 * 
 * @author Filippo Muzzini
 *
 */
public class SmartPerceptionModule extends AbstractModule {
	
	private static final String DEFAULT_WRAPPER_CLASS = "perception.wrapper.ActivePerceptionWrapperImpl";

	@Override
	public void install() {		
		Config config = getConfig();
		if (!config.getModules().containsKey(PerceptionConfigGroup.GRUOPNAME)) {
			return;
		}
		PerceptionConfigGroup cameraConfig = ConfigUtils.addOrGetModule(config, PerceptionConfigGroup.GRUOPNAME, PerceptionConfigGroup.class);
		String className = cameraConfig.getWrapperClass();
		className = className != null ? className : DEFAULT_WRAPPER_CLASS;
		className = InstantationUtils.foundClassName(className);
		Class<PassivePerceptionWrapper> perceptionClass = InstantationUtils.getClassForName(className);
		
		//bind the class to interfaces
		bind(PassivePerceptionWrapper.class).to(perceptionClass);
		try {
			Class<? extends ActivePerceptionWrapper> activeClass = ActivePerceptionWrapper.class;
		    activeClass = perceptionClass.asSubclass(activeClass);
			bind(ActivePerceptionWrapper.class).to(activeClass);
		} catch (ClassCastException e) {
		}
		bind(perceptionClass).asEagerSingleton();
		addEventHandlerBinding().to(perceptionClass);
		
		bind(FlowWriter.class).asEagerSingleton();
		addControlerListenerBinding().to(FlowWriter.class);
		addMobsimListenerBinding().to(FlowWriter.class);
		
		bind(CamerasContainer.class).asEagerSingleton();;		
		addControlerListenerBinding().to(CameraStartupListener.class);
	}

}
