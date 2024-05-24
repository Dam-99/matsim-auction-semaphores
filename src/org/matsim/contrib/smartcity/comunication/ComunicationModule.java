/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.net.URL;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.smartcity.InstantationUtils;
import org.matsim.contrib.smartcity.agent.parking.CameraPark;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationWrapper;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.collections.Tuple;

import com.google.inject.Injector;

/**
 * Module for agents comunication
 * @author Filippo Muzzini
 *
 */
public class ComunicationModule extends AbstractModule implements StartupListener {

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		Config config = getConfig();
		if (!config.getModules().containsKey(ComunicationConfigGroup.GROUPNAME)) {
			return;
		}
		ComunicationConfigGroup configGroup = ConfigUtils.addOrGetModule(config, ComunicationConfigGroup.class);
		String wrapperName = configGroup.getParams().get(ComunicationConfigGroup.WRAPPER);
		Class<ComunicationWrapper> wrapperClass = InstantationUtils.getClassForName(wrapperName);
		
		bind(ComunicationWrapper.class).to(wrapperClass);
		try {
			Class<? extends EventHandler> eventWrapper = wrapperClass.asSubclass(EventHandler.class);
			addEventHandlerBinding().to(eventWrapper);
		} catch (ClassCastException e) {
			
		}
		
		bind(wrapperClass).asEagerSingleton();
		
		bind(ComunicationServerFactory.class).to(ComunicationServerFactoryImpl.class);
		
		addControlerListenerBinding().to(this.getClass());
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		Config config = event.getServices().getConfig();
		ComunicationConfigGroup configGroup = ConfigUtils.addOrGetModule(config, ComunicationConfigGroup.class);
		String serverList = configGroup.getParams().get(ComunicationConfigGroup.SERVERLIST);
		if(serverList.equals("null")) {
			return;
		}
		
		ComunicationServerListXMLReader reader = new ComunicationServerListXMLReader();
		event.getServices().getInjector().injectMembers(reader);
		URL fileURL = ConfigGroup.getInputFileURL(config.getContext(), serverList);
		reader.readFile(fileURL.getFile());
		
		Injector inj = event.getServices().getInjector();
		ComunicationServerFactory factory = inj.getInstance(ComunicationServerFactory.class);
		for (Tuple<String, String> server : reader.getServerList()) {
			String serverId = server.getFirst();
			String serverClass = server.getSecond();
			Set<Coord> coord = reader.getServerCoord(server);
			Set<Id<Camera>> cameras = reader.getServerCameras(server);
			Set<Id<CameraPark>> parks = reader.getServerParks(server);
			factory.instantiateServer(serverId, serverClass, coord, cameras, parks);
		}
		
	}

}
