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
 * Tour plan is to visit Skógafoss and Seljalandsfoss on the way back.
 * Applies to third group with ID 'T'.
 */
public class MyTouristMovement2 extends MyMapBasedMovement implements 
	SwitchableMovement {
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Points Of Interest handler */
	private PointsOfInterest pois;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyTouristMovement2(Settings settings) {
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
	protected MyTouristMovement2(MyTouristMovement2 mbm) {
		super(mbm);
		this.pathFinder = mbm.pathFinder;
		this.pois = mbm.pois;
	}
	
	boolean tourIsFinished = false;
	@Override
	public Path getPath() {
		
		int currStopIndex = -1;
		Coord prevStop = lastMapNode.getLocation();
		Path p = new Path(generateSpeed());
		MapNode to;
		Coord[] tourStop = {
				new Coord(310.118, 47616.681),		//Hella 0
				new Coord(28848.231, 18902.748),	//Árjánurfoss 1
				new Coord(56690.948, 16157.112),	//Solheimajokull 2
				new Coord(67572.482, 7.617),		//Reynisfjara 3
				new Coord(69618.766, 1562.244),		//Vik 4
				new Coord(44049.796, 13917.869),	//Skogsfoss 5
				new Coord(20538.307, 24673.543)};	//Seljalandsfoss 6
		
		if (tourIsFinished) {
			host.update(!isActive());	// Deactivate the host when it gets to the end of the map
		}
		
		for (int i=0; i<7; i++) {
			if (prevStop.equals(getConvertedCoord(tourStop[i]))) 
					currStopIndex = i;
		}
		
		if (currStopIndex == 6) 	// Check if the last attraction is visited
			tourIsFinished = true;
		
		if (tourIsFinished) {
			currStopIndex = -1;
		}
		
		to = getMap().getNodeByCoord(getConvertedCoord(tourStop[currStopIndex+1]));
		
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
	public MyTouristMovement2 replicate() {
		return new MyTouristMovement2(this);
	}

}
