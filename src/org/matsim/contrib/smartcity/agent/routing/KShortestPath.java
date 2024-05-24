/**
 * 
 */
package org.matsim.contrib.smartcity.agent.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.smartcity.perception.TrafficFlow;
import org.matsim.core.network.algorithms.NetworkInverter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author Filippo Muzzini
 *
 */
public class KShortestPath {

	private Network network;
	private int k;
	private TreeSet<List<Id<Link>>> paths;
	//private Id<Node> dest;
	//private Id<Node> source;
	private NetworkInverter inverter;
	//private double startTime;
	private YensAlgorithm Yen;
	private TravelDisutility travel;
	private Person person;
	private Vehicle veh;
	private TravelTime time;
	private Network originalNet;

	public KShortestPath(NetworkInverter inverter, Network original, TrafficFlow flow, int k) {
		this.network = inverter.getInvertedNetwork();
		this.inverter = inverter;
		this.originalNet = original;
		this.k = k;
		this.Yen = new YensAlgorithm(network, k);
	}
	
	public void setTravelDisutility(TravelDisutility travel) {
		this.Yen.setTravelDisutility(travel);
		this.travel = travel;;
	}
	
	public void setTravelTime(TravelTime time) {
		this.Yen.setTravelTime(time);
		this.time = time;
	}

	/**
	 * @param source 
	 * @param dest 
	 * @param startTime 
	 * @param person 
	 * @param veh 
	 * 
	 */
	public void route(List<Id<Node>> sources, List<Id<Node>> dests, double startTime, Person person, Vehicle veh) {
		if (this.paths == null) {
			this.paths = new TreeSet<List<Id<Link>>>((p1, p2) -> Double.compare(
					calcCostToEnd(p1, p1.get(0), originalNet ,startTime), 
					calcCostToEnd(p2, p2.get(0), originalNet, startTime)));
		}
		synchronized(this.paths) {
			this.paths = new TreeSet<List<Id<Link>>>((p1, p2) -> Double.compare(
					calcCostToEnd(p1, p1.get(0), originalNet ,startTime), 
					calcCostToEnd(p2, p2.get(0), originalNet, startTime)));
			this.person = person;
			this.veh = veh;
			
			for (Id<Node> source : sources) {
				for (Id<Node> dest : dests) {
					List<Path> P = this.Yen.routeInverted(source, dest, startTime, person, veh);
					addToPaths(P, sources, dests, startTime);
				}
			}
			
			TreeSet<List<Id<Link>>> onlyK = new TreeSet<List<Id<Link>>>((p1, p2) -> Double.compare(
					calcCostToEnd(p1, p1.get(0), originalNet ,startTime), 
					calcCostToEnd(p2, p2.get(0), originalNet, startTime)));
			for (int i=this.k; i>0 && !this.paths.isEmpty(); i--) {
				onlyK.add(this.paths.pollFirst());
			}
			this.paths = onlyK;
		}
		
		
//		PriorityQueue<Path> P = new PriorityQueue<Path>(new Path());
//		PriorityQueue<Path> B = new PriorityQueue<Path>(new Path());
//		HashMap<Id<Node>, Integer> count = new HashMap<Id<Node>, Integer>();
//		count.put(dest, 0);
//		ArrayList<Id<Node>> ps = new ArrayList<Id<Node>>();
//		ps.add(source);
//		B.add(new Path(ps, 0.0));
//		while (!B.isEmpty() && count.get(dest) < k) {
//			Path pu = B.poll();
//			Id<Node> u = pu.path.get(pu.path.size()-1);
//			Integer countU = count.get(u);
//			if (countU == null) {
//				countU = 1;
//			} else {
//				countU++;
//			}
//			count.put(u, countU);
//			if (u.equals(this.dest)) {
//				P.add(pu);
//			}
//			if (countU <= k) {
//				for (Id<Node> v : adjacent(u)) {
//					if (!pu.contains(v)) {
//						ArrayList<Id<Node>> p = new ArrayList<Id<Node>>(pu.path);
//						Node nodeU = network.getNodes().get(u);
//						Node nodeV = network.getNodes().get(v);
//						p.add(v);
//						Double cost = pu.cost + calcCost(NetworkUtils.getConnectingLink(nodeU, nodeV));
//						Path pv = new Path(p, cost);
//						B.add(pv);
//					}
//				}
//			}
//		}		
	}
	
	/**
	 * @param  startTime 
	 * @param p 
	 * @return 
	 * 
	 */
	private boolean filterPaths(List<Id<Link>> p, List<Id<Node>> sources, List<Id<Node>> dests, double startTime) {
		//cut the paths to possible start/end
		for (Id<Node> source : sources) {
			for (Id<Node> dest : dests) {
				cutPath(p, source, dest);
			}
		}
		//if two paths are equals don't insert
		for (List<Id<Link>> p2 : this.paths) {
			if (p2 == p) {
				continue;
			} else if (p2.equals(p)) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @param p
	 * @param source
	 * @param dest
	 */
	private void cutPath(List<Id<Link>> p, Id<Node> source, Id<Node> dest) {
		ArrayList<Node> sourceDestList = new ArrayList<Node>();
		sourceDestList.add(network.getNodes().get(source));
		sourceDestList.add(network.getNodes().get(dest));
		List<Link> original = inverter.convertInvertedNodesToLinks(sourceDestList);
		Link oSource = original.get(0);
		Link oDest = original.get(1);
		
		int sourceIndex = p.indexOf(oSource.getId());
		for (int i=sourceIndex-1; i>=0; i--) {
			p.remove(i);
		}
		
		int destIndex = p.indexOf(oDest.getId());
		if (destIndex == -1) {
			return;
		}
		for (int i=p.size()-1; i>destIndex; i--) {
			p.remove(i);
		}
	}

	/**
	 * @param sources 
	 * @param dests 
	 * @param startTime 
	 * @param p
	 */
	private void addToPaths(List<Path> P, List<Id<Node>> sources, List<Id<Node>> dests, double startTime) {
		for (org.matsim.core.router.util.LeastCostPathCalculator.Path p : P) {
			if (p == null) {
				continue;
			}
			List<Node> invPath = p.nodes;
			List<Id<Link>> path = inverter.convertInvertedNodesToLinks(invPath).stream().
					map(l -> l.getId()).collect(Collectors.toList());
			if (filterPaths(path, sources, dests, startTime)) {
				this.paths.add(path);
			}
		}		
	}

	/**
	 * @param time 
	 * @param person 
	 * @param vehicle 
	 * @param connectingLink
	 * @return
	 */
	private Double calcCost(Link link, double time, Person person, Vehicle vehicle) {
		return this.travel.getLinkTravelDisutility(link, time, person, vehicle);
		
	}

	/**
	 * @param u
	 * @return
	 */
//	private List<Id<Node>> adjacent(Id<Node> u) {
//		Node node = network.getNodes().get(u);
//		return node.getOutLinks().values().stream().map(l -> l.getToNode().getId()).collect(Collectors.toList());
//	}
	
	public HashMap<Id<Link>, Double> getNextLinks(Id<Link> actualLink, Network net, double actualTime){
		HashMap<Id<Link>, Double> nextLinks = new HashMap<Id<Link>, Double>();
		synchronized(this.paths) {
			System.out.println("size="+this.paths.size());
			for (List<Id<Link>> path : paths) {
				int n = actualLink == null ? 0 : path.indexOf(actualLink);
				if (n == path.size()-1 || n == -1) {
					continue;
				}
				Id<Link> nextLink = actualLink == null ? path.get(n) : path.get(n+1);
				Double nextFlow = calcCostToEnd(path, nextLink, net, actualTime);
				if (nextFlow == null) {
					nextFlow = 0.0;
				}
				Double previousFlowSameLink = nextLinks.get(nextLink);
				if (previousFlowSameLink == null || previousFlowSameLink > nextFlow) {
					nextLinks.put(nextLink, nextFlow);
				}
			}
		}
		
		System.out.println("links size="+nextLinks.size()+" actual="+actualLink);
		
		return nextLinks;
	}
	
	/**
	 * @param nextLink
	 * @param net
	 * @return
	 */
	private Double calcCostToEnd(List<Id<Link>> path, Id<Link> nextLink, Network net, double time) {
		Double res = 0.0;
		int n = path.indexOf(nextLink);
		for (int i=n; i<path.size(); i++) {
			Link link = net.getLinks().get(path.get(i));
			res += calcCost(link, time, this.person, this.veh);
			time += this.time.getLinkTravelTime(link, time, this.person, this.veh);
		}
		
		return res;
	}
	
//	private class Path implements Comparator<Path> {
//		List<Id<Node>> path;
//		Double cost;
//		
//		public Path() {
//			
//		}
//		
//		/**
//		 * @param v
//		 * @return
//		 */
//		public boolean contains(Id<Node> v) {
//			return path.contains(v);
//		}
//
//		public Path(List<Id<Node>> path, Double cost) {
//			this.path = path;
//			this.cost = cost;
//		}
//
//		/* (non-Javadoc)
//		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
//		 */
//		@Override
//		public int compare(Path arg0, Path arg1) {
//			return Double.compare(arg0.cost, arg1.cost);
//		}
//	}
	
}
