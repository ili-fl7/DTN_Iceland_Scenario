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
 * Tour plan is to go straight to east without any stops at attractions.
 * Applies to fifth group with ID 'N'.
 */
public class MyRingRoadToEastMovement extends MyMapBasedMovement implements 
	SwitchableMovement {
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Points Of Interest handler */
	private PointsOfInterest pois;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyRingRoadToEastMovement(Settings settings) {
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
	protected MyRingRoadToEastMovement(MyRingRoadToEastMovement mbm) {
		super(mbm);
		this.pathFinder = mbm.pathFinder;
		this.pois = mbm.pois;
	}
	
	static boolean tourIsFinished = false;
	@Override
	public Path getPath() {
		boolean alreadyOnSpot = false;
		int nextStopIndex = -1;
		int nodeAtCounter = -1;
		Coord prevStop = lastMapNode.getLocation();
		Path p = new Path(generateSpeed());
		MapNode to;
		Coord[] counterLocation = {
				new Coord(1744.143, 46869.218),		//C0, before stop 1
				new Coord(7493.507, 39560.275),		//C1, before stop 1
				new Coord(18466.993, 24194.281),	//C2, before stop 1
				new Coord(25853.721, 18780.89),		//C3, before stop 2
				new Coord(37790.483, 14619.119),	//C4, before stop 2
				new Coord(56118.833, 7845.725),		//C5, before stop 3
				new Coord(67821.448, 5293.879),		//C6, before stop 4
				new Coord(89942.131, 7314.48),		//C7, before stop 5
				new Coord(108638.817, 36795.369)};	//C8, before stop 5
		
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
					alreadyOnSpot = true;
			}
		}
		
		if (!alreadyOnSpot) {	//If the initial location is not on attraction spots
			for (int i=0; i<9; i++) {	//Check if it is the same as counters locations
				if (prevStop.equals(getConvertedCoord(counterLocation[i]))) {
						nodeAtCounter = i;
				}
			}
			switch (nodeAtCounter) {
				case 3: nextStopIndex = 1;
					break;
				case 4: nextStopIndex = 1;
					break;
				case 5: nextStopIndex = 2;
					break;
				case 6: nextStopIndex = 3;
					break;
				case 7: nextStopIndex = 4;
					break;
				case 8: nextStopIndex = 4;
					break;
				default: nextStopIndex = 0;
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
	public MyRingRoadToEastMovement replicate() {
		return new MyRingRoadToEastMovement(this);
	}

}
