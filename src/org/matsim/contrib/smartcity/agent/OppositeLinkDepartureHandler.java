/**
 * 
 */
package org.matsim.contrib.smartcity.agent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.network.NetworkUtils;
import com.google.inject.Inject;

/**
 * @author Filippo Muzzini
 *
 */
public class OppositeLinkDepartureHandler implements DepartureHandler {
 	
	@Inject private Network network;

	private DepartureHandler delegate;

	/**
	 * @param provider
	 */
	@Inject
	public OppositeLinkDepartureHandler(QNetsimEngine engine) {
		this.delegate = engine.getDepartureHandler();
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (agent instanceof DynAgent) {
			DynAgent dynAgent = (DynAgent) agent;
			if (dynAgent.getCurrentAction() instanceof DriverDynLeg) {
				Id<Link> linkIdTmp = dynAgent.chooseNextLinkId();
				if (linkIdTmp != null && (isOpposite(linkIdTmp, linkId) || linkIdTmp.equals(linkId))) {
					linkId = linkIdTmp;
					dynAgent.notifyArrivalOnLinkByNonNetworkMode(linkIdTmp);
				}
			}
			
		}
		
		return this.delegate.handleDeparture(now, agent, linkId);
	}

	/**
	 * @param linkIdTmp
	 * @return
	 */
	private boolean isOpposite(Id<Link> linkIdTmp, Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);
		Link linkTmp = network.getLinks().get(linkIdTmp);
		Link opposite = NetworkUtils.getConnectingLink(link.getToNode(), link.getFromNode());
		if (opposite == null) {
			return false;
		}
		try {
			if (opposite.getId().equals(linkIdTmp)
					&& opposite.getAttributes().getAttribute(NetworkUtils.ORIGID).equals(linkTmp.getAttributes().getAttribute(NetworkUtils.ORIGID))) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		
		return false;
	}

}

