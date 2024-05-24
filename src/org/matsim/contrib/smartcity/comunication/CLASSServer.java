/**
 * 
 */
package org.matsim.contrib.smartcity.comunication;

import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.smartcity.comunication.wrapper.ComunicationFixedWrapper;
import org.matsim.contrib.smartcity.perception.TrafficFlow;
import org.matsim.contrib.smartcity.perception.camera.ActiveCamera;
import org.matsim.contrib.smartcity.perception.camera.Camera;
import org.matsim.contrib.smartcity.perception.camera.CameraListener;
import org.matsim.contrib.smartcity.perception.camera.CameraStatus;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class CLASSServer implements ComunicationServer, CameraListener {

	private static final double CRITICAL_FLOW = 0.75;

	private static final double BUFFER_TIME = 60;
	
	private ComunicationFixedWrapper wrapper;
	@Inject Network network;
	private TrafficFlow flow = new TrafficFlow();
	private HashMap<Id<Link>, Double> flowTime = new HashMap<Id<Link>, Double>();
	
	public CLASSServer(ComunicationFixedWrapper wrapper, ComunicationServerFactory.ServerData data) {
		Set<Coord> positions = data.coord;
		Set<Camera> cameras = data.cameras;
		this.wrapper = wrapper;
		this.wrapper.addFixedComunicator(this, positions);
		for (Camera camera : cameras) {
			if (camera instanceof ActiveCamera)
				((ActiveCamera)camera).addCameraListener(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.comunication.ComunicationServer#sendToMe(org.matsim.contrib.smartcity.comunication.ComunicationMessage)
	 */
	@Override
	public void sendToMe(ComunicationMessage message) {
		if (message instanceof FlowRequest) {
			message.getSender().sendToMe(createFlowMessage());
		}
	}

	/**
	 * @return
	 */
	private ComunicationMessage createFlowMessage() {
		return new TrafficFlowMessage(this, flow);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.smartcity.perception.camera.CameraListener#pushCameraStatus(org.matsim.contrib.smartcity.perception.camera.CameraStatus)
	 */
	@Override
	public void pushCameraStatus(CameraStatus status, double time) {
		Id<Link> linkId = status.getIdLink();
		Link link = network.getLinks().get(linkId);
		int vehs = status.getLinkStatus().getTotal();
		double flow = new Double(vehs);
		double criticalFlow = getCriticalFlow(link);
		
		if (flow >= criticalFlow) {
			this.flow.addFlow(linkId, flow);
			ComunicationMessage message = createFlowMessage();
			wrapper.broadcast(message);
			return;
//			double time = sim.getSimTimer().getTimeOfDay();
//			if (this.flowTime.get(linkId) == null) {
//				this.flowTime.put(linkId, time);
//			} else if (time - this.flowTime.get(linkId) > BUFFER_TIME) {
//				ComunicationMessage message = createFlowMessage();
//				wrapper.broadcast(message);
//				return;
//			}
		} else if (flow < criticalFlow) {
			this.flow.addFlow(linkId, null);
			if (this.flowTime.get(linkId) != null) {
				this.flowTime.remove(linkId);
			}
		}
	}

	/**
	 * @param link
	 * @return
	 */
	private double getCriticalFlow(Link link) {
		double maxCap = link.getCapacity() / network.getCapacityPeriod();
		if (maxCap < 10) {
			return Double.POSITIVE_INFINITY;
		}
		return maxCap * CRITICAL_FLOW;		
	}

}
