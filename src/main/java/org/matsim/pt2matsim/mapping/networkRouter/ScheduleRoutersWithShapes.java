package org.matsim.pt2matsim.mapping.networkRouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.gtfs.lib.ShapeSchedule;
import org.matsim.pt2matsim.mapping.linkCandidateCreation.LinkCandidate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author polettif
 */
public class ScheduleRoutersWithShapes implements ScheduleRouters {

	protected static Logger log = Logger.getLogger(ScheduleRoutersWithShapes.class);

	private final PublicTransitMappingConfigGroup config;
	private final ShapeSchedule shapeSchedule;
	private Map<TransitLine, Map<TransitRoute, Router>> routers = new HashMap<>();
	private Map<String, Router> routersByShape = new HashMap<>();
	private Network network;

	public ScheduleRoutersWithShapes(PublicTransitMappingConfigGroup config, ShapeSchedule shapeSchedule, Network network) {
		this.config = config;
		this.shapeSchedule = shapeSchedule;
		this.network = network;

		init();
	}

	public void init() {
		RouterShapes.setTravelCostType(config.getTravelCostType());

		for(TransitLine transitLine : this.shapeSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				log.info("Initiating network and router for transit route " + transitRoute.getId() + " (line " + transitLine.getId() + ")");
				String shapeId = shapeSchedule.getShape(transitLine.getId(), transitRoute.getId()).getId();
				Router tmpRouter = routersByShape.get(shapeId);
				if(tmpRouter == null) {
					log.info("New router for shape \"" + shapeId + "\"");
					Set<String> networkTransportModes = config.getModeRoutingAssignment().get(transitRoute.getTransportMode());
					tmpRouter = new RouterShapes(network, networkTransportModes, shapeSchedule.getShape(transitLine.getId(), transitRoute.getId()));
					routersByShape.put(shapeId, tmpRouter);
				}
				MapUtils.getMap(transitLine, routers).put(transitRoute, tmpRouter);
			}
		}
	}

	/**
	 * Either extract the router or call this method
	 */
	@Override
	public LeastCostPathCalculator.Path calcLeastCostPath(LinkCandidate fromLinkCandidate, LinkCandidate toLinkCandidate, TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute).calcLeastCostPath(fromLinkCandidate.getToNodeId(), toLinkCandidate.getFromNodeId());
	}

	@Override
	public LeastCostPathCalculator.Path calcLeastCostPath(Id<Node> fromNodeId, Id<Node> toNodeId, TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute).calcLeastCostPath(fromNodeId, toNodeId);
	}

	@Override
	public Router getRouter(TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute);
	}

	@Override
	public Router getRouter(String scheduleMode) {
		return null;
	}

	@Override
	public double getMinimalTravelCost(TransitRouteStop fromTransitRouteStop, TransitRouteStop toTransitRouteStop, TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute).getMinimalTravelCost(fromTransitRouteStop, toTransitRouteStop);
	}

	@Override
	public double getArtificialLinkFreeSpeed(double maxAllowedTravelCost, LinkCandidate linkCandidateCurrent, LinkCandidate linkCandidateNext, TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute).getArtificialLinkFreeSpeed(maxAllowedTravelCost, linkCandidateCurrent, linkCandidateNext);
	}

	@Override
	public double getArtificialLinkLength(double maxAllowedTravelCost, LinkCandidate linkCandidateCurrent, LinkCandidate linkCandidateNext, TransitLine transitLine, TransitRoute transitRoute) {
		return routers.get(transitLine).get(transitRoute).getArtificialLinkLength(maxAllowedTravelCost, linkCandidateCurrent, linkCandidateNext);
	}

}
