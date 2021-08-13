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
public class MyCounterLocations extends MyMapBasedMovement implements 
	SwitchableMovement {
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Points Of Interest handler */
	private PointsOfInterest pois;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyCounterLocations(Settings settings) {
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
	protected MyCounterLocations(MyCounterLocations mbm) {
		super(mbm);
		this.pathFinder = mbm.pathFinder;
		this.pois = mbm.pois;
	}
	
	@Override
	public Path getPath() {
		Path p = new Path(generateSpeed());
		MapNode to;
		Coord[] counterLocation = {
				new Coord(1744.143, 46869.218),
				new Coord(7493.507, 39560.275),
				new Coord(18466.993, 24194.281),
				new Coord(25853.721, 18780.89),
				new Coord(37790.483, 14619.119),
				new Coord(56118.833, 7845.725),
				new Coord(67821.448, 5293.879),
				new Coord(89942.131, 7314.48),
				new Coord(108638.817, 36795.369)};
		
		int currentCounter = currentNode;
		to = getMap().getNodeByCoord(getConvertedCoord(counterLocation[currentCounter]));
		
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
	public MyCounterLocations replicate() {
		return new MyCounterLocations(this);
	}

}
