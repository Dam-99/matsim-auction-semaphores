/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.agent.parking.CameraPark;
import org.matsim.contrib.smartcity.agent.parking.FacilityWithMoreEntranceParkingManager;
import org.matsim.contrib.smartcity.agent.parking.ParkData;
import org.matsim.contrib.smartcity.agent.parking.ParksContainer;
import org.matsim.contrib.smartcity.auctionIntersections.AuctionIntersectionConfigGroup;
import org.matsim.contrib.smartcity.perception.CameraData;
import org.matsim.contrib.smartcity.perception.CameraStartupListener;
import org.matsim.contrib.smartcity.perception.CameraXMLReader;
import org.matsim.contrib.smartcity.perception.CamerasContainer;
import org.matsim.contrib.smartcity.perception.PerceptionConfigGroup;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

import com.google.inject.Injector;
import com.google.inject.Provides;

/**
 * Module for SmartAgent.
 * Provides a list of plugins for MATSim simulation; in particular
 * the SmartPopulationPlugin.
 * 
 * @see SmartPopulationPlugin
 * @author Filippo Muzzini
 *
 */
public class SmartAgentModule extends AbstractModule implements StartupListener {
	
	/**
	 * Create the plugins list with SmartPopulationPlugin
	 * 
	 * @param config1 Configuration instance
	 * @return list of plugins
	 */
	@Provides
	Collection<AbstractQSimModule> provideQSimPlugins(Config config1) {
		Collection<AbstractQSimModule> modules = new LinkedList<>(QSimModule.getDefaultQSimModules());
		modules.removeIf(PopulationModule.class::isInstance);
		modules.removeIf(QNetsimEngineModule.class::isInstance);
		modules.add(new SmartPopulationPlugin());
		modules.add(new OppositeLinkModule());
		return modules;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		bind(FacilityWithMoreEntranceParkingManager.class).asEagerSingleton();
		bind(ParkingSearchManager.class).to(FacilityWithMoreEntranceParkingManager.class);
		bind(WalkLegFactory.class).asEagerSingleton();
		addControlerListenerBinding().to(ParkingListener.class);
		bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
		
		bind(ParksContainer.class).asEagerSingleton();;		
		addControlerListenerBinding().to(this.getClass());
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		Injector inj = event.getServices().getInjector();
		ParksContainer cont = inj.getInstance(ParksContainer.class);
		
		List<ParkData> parksList = getParksListFromConfig(event.getServices().getConfig(), inj);
		for (ParkData camera : parksList) {
			String className = camera.getClassName();
			Object[] params = {camera.getCameraId(), camera.getParkId()};
			CameraPark cameraInst = InstantationUtils.instantiateForNameWithParams(inj, className, params);
			cont.addCamera(camera.getCameraId(), cameraInst);
		}
	}

	/**
	 * @param config
	 * @param inj
	 * @return
	 */
	private List<ParkData> getParksListFromConfig(Config config, Injector inj) {
		if (config.getModules().containsKey(PerceptionConfigGroup.GRUOPNAME)) {
			PerceptionConfigGroup group = ConfigUtils.addOrGetModule(config, PerceptionConfigGroup.GRUOPNAME, PerceptionConfigGroup.class);
			String fileName = group.getCameraFile();
			CameraXMLReader reader = new CameraXMLReader();
			inj.injectMembers(reader);
			URL fileURL = ConfigGroup.getInputFileURL(config.getContext(), fileName);
			reader.readFile(fileURL.getFile());
			return reader.getParkList();
		} else  {
			return new ArrayList<>();
		}
	}

}
