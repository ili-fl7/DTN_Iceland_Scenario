/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.List;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.PointsOfInterest;
import core.Coord;
import core.Settings;

/**
 * Keeps the road counters on their location.
 * Applies to the first group with ID 'C'.
 */
public class MyReceiverLocation extends MyMapBasedMovement implements 
	SwitchableMovement {
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Points Of Interest handler */
	private PointsOfInterest pois;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyReceiverLocation(Settings settings) {
		super(settings);
		this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		this.pois = new PointsOfInterest(getMap(), getOkMapNodeTypes(),
				settings, rng);
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The ShortestPathMapBasedMovement prototype to base 
	 * the new object to 
	 */
	protected MyReceiverLocation(MyReceiverLocation mbm) {
		super(mbm);
		this.pathFinder = mbm.pathFinder;
		this.pois = mbm.pois;
	}
	
	boolean tourIsFinished = false;
	@Override
	public Path getPath() {
		int nextStopIndex = -1;
		Coord prevStop = lastMapNode.getLocation();
		Path p = new Path(generateSpeed());
		MapNode to;
		Coord[] tourStop = {
				new Coord(310.118, 47616.681),		//Hella 0
				new Coord(20538.307, 24673.543),	//Seljalandsfoss 1
				new Coord(44049.796, 13917.869),	//Skogsfoss 2
				new Coord(67572.482, 7.617),		//Reynisfjara 3
				new Coord(69618.766, 1562.244),		//Vik 4
				new Coord(113513.213, 40405.067)};	//kirkju 5
		
		if (tourIsFinished) {
			host.update(!isActive());	// Deactivate the host when it gets to the end of the map
		}
		
		for (int i=0; i<6; i++) {
			if (prevStop.equals(getConvertedCoord(tourStop[i]))) {
					nextStopIndex = i;
			}
		}
		
		if (nextStopIndex == 5) {
			nextStopIndex = 4;
			tourIsFinished = true;
		}
		
		to = getMap().getNodeByCoord(getConvertedCoord(tourStop[nextStopIndex+1]));
		
		List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);
		
		// this assertion should never fire if the map is checked in read phase
				assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
					to + ". The simulation map isn't fully connected";
				
		for (MapNode node : nodePath) { // create a Path from the shortest path
			p.addWaypoint(node.getLocation());
		}
			
		lastMapNode = to;
		return p;
	}	
	
	@Override
	public MyReceiverLocation replicate() {
		return new MyReceiverLocation(this);
	}

}
