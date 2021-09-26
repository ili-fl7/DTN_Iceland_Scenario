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
 * Map based movement model that uses Dijkstra's algorithm to find shortest
 * paths.
 * Tour plan is to visit Seljalandsfoss, Árjánurfoss, and Skógafoss only.
 * Applies to fourth group with ID 'B'.
 */
public class MyTouristMovement3 extends MyMapBasedMovement implements 
	SwitchableMovement {
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Points Of Interest handler */
	private PointsOfInterest pois;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyTouristMovement3(Settings settings) {
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
	protected MyTouristMovement3(MyTouristMovement3 mbm) {
		super(mbm);
		this.pathFinder = mbm.pathFinder;
		this.pois = mbm.pois;
	}
	
	boolean tourIsFinished = false;
	@Override
	public Path getPath() {
		boolean alreadyOnSpot = false;
		int nextStopIndex = -1;
		Path p = new Path(generateSpeed());
		MapNode to = pois.selectDestination();
		Coord prevStop = lastMapNode.getLocation();
		Coord[] tourStop = {
				new Coord(310.118, 47616.681),		//Hella 0
				new Coord(20538.307, 24673.543),	//Seljalandsfoss 1
				new Coord(28848.231, 18902.748),	//Árjánurfoss 2
				new Coord(44049.796, 13917.869)};	//Skogafoss 3
		
		if (tourIsFinished) {
			host.update(!isActive());	// Deactivate the host when it gets to the end of the map
		}
		
		for (int i=0; i<4; i++) {
			if (prevStop.equals(getConvertedCoord(tourStop[i]))) { 
					nextStopIndex = i;
					alreadyOnSpot = true;
			}
		}
		
		/** Only one situation in our project where hosts with this mobility pattern,
		 *  start from elsewhere that Hella, so they have to visit Skogafoss and turn back.
		*/
		if(!alreadyOnSpot) { // If node is located elsewhere than these points
			nextStopIndex = 2; // Then it is located before Skogafoss
		}
		
		if (nextStopIndex == 3)		// Check if the last attraction is visited
			tourIsFinished = true;
		
		
		if (tourIsFinished) {
			nextStopIndex = -1;
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
	public MyTouristMovement3 replicate() {
		return new MyTouristMovement3(this);
	}

}
