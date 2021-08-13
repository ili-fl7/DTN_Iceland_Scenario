/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import input.WKTMapReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;
import core.SettingsError;
import core.SimError;

/**
 * Map based movement model which gives out Paths that use the
 * roads of a SimMap. 
 */
public class MyMapBasedMovement extends MovementModel implements SwitchableMovement {
	/** sim map for the model */
	private SimMap map = null;
	/** node where the last path ended or node next to initial placement */
	protected MapNode lastMapNode;
	/**  max nrof map nodes to travel/path */
	protected int maxPathLength = 100;
	/**  min nrof map nodes to travel/path */
	protected int minPathLength = 10;
	/** May a node choose to move back the same way it came at a crossing */
	protected boolean backAllowed;
	/** map based movement model's settings namespace ({@value})*/
	public static final String MAP_BASE_MOVEMENT_NS = "MapBasedMovement";
	/** number of map files -setting id ({@value})*/
	public static final String NROF_FILES_S = "nrofMapFiles";
	/** map file -setting id ({@value})*/
	public static final String FILE_S = "mapFile";
	
	/** 
	 * Per node group setting for selecting map node types that are OK for
	 * this node group to traverse trough. Value must be a comma separated list
	 * of integers in range of [1,31]. Values reference to map file indexes
	 * (see {@link #FILE_S}). If setting is not defined, all map nodes are 
	 * considered OK.
	 */
	public static final String MAP_SELECT_S = "okMaps";
	
	/** the indexes of the OK map files or null if all maps are OK */
	private int [] okMapNodeTypes;
	
	/** how many map files are read */
	private int nrofMapFilesRead = 0;
	/** map cache -- in case last mm read the same map, use it without loading*/
	private static SimMap cachedMap = null;
	/** names of the previously cached map's files (for hit comparison) */
	private static List<String> cachedMapFiles = null;
	
	/**
	 * Creates a new MapBasedMovement based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MyMapBasedMovement(Settings settings) {
		super(settings);
		map = readMap();
		readOkMapNodeTypes(settings);
		maxPathLength = 100;
		minPathLength = 10;
		backAllowed = false;
	}

	/**
	 * Creates a new MapBasedMovement based on a Settings object's settings
	 * but with different SimMap
	 * @param settings The Settings object where the settings are read from
	 * @param newMap The SimMap to use
	 * @param nrofMaps How many map "files" are in the map
	 */	
	public MyMapBasedMovement(Settings settings, SimMap newMap, int nrofMaps) {
		super(settings);
		map = newMap;
		this.nrofMapFilesRead = nrofMaps;
		readOkMapNodeTypes(settings);
		maxPathLength = 100;
		minPathLength = 10;
		backAllowed = false;
	}
	
	/**
	 * Reads the OK map node types from settings
	 * @param settings The settings where the types are read
	 */
	private void readOkMapNodeTypes(Settings settings) {
		if (settings.contains(MAP_SELECT_S)) {
			this.okMapNodeTypes = settings.getCsvInts(MAP_SELECT_S);
			for (int i : okMapNodeTypes) {
				if (i < MapNode.MIN_TYPE || i > MapNode.MAX_TYPE) {
					throw new SettingsError("Map type selection '" + i + 
							"' is out of range for setting " + 
							settings.getFullPropertyName(MAP_SELECT_S));
				}
				if (i > nrofMapFilesRead) {
					throw new SettingsError("Can't use map type selection '" + i
							+ "' for setting " + 
							settings.getFullPropertyName(MAP_SELECT_S)
							+ " because only " + nrofMapFilesRead + 
							" map files are read");
				}
			}
		}
		else {
			this.okMapNodeTypes = null;
		}		
	}
	
	/**
	 * Copyconstructor.
	 * @param mbm The MapBasedMovement object to base the new object to 
	 */
	protected MyMapBasedMovement(MyMapBasedMovement mbm) {
		super(mbm);
		this.okMapNodeTypes = mbm.okMapNodeTypes;
		this.map = mbm.map;
		this.minPathLength = mbm.minPathLength;
		this.maxPathLength = mbm.maxPathLength;
		this.backAllowed = mbm.backAllowed;
	}
	
	/** Node Identifier and current node specifier */
	public static int nodeID = -1;
	public int currentNode = -1;
	
	/**
	 * Restores a real location to the converted one in the ONE, by mirroring the Y coordinate
	 * @param loc: location to be restored
	 */
	public Coord getConvertedCoord(Coord loc) {
		double yBound = map.getMaxBound().getY();
		return new Coord(loc.getX(), (yBound - loc.getY()));
	}
	
	/**
	 * Returns a (custom) coordinate for the counters and
	 * calls specific methods for mobile nodes
	 */
	@Override
	public Coord getInitialLocation() {
		MapNode n;
		Coord currentNodeCoord;
		String currentNodeID = host.getNodeID();
		int dayTimeID = this.getDayTime();
		
		Coord[] counterLocation = {
				new Coord(1744.143, 46869.218),		//C0
				new Coord(7493.507, 39560.275),		//C1
				new Coord(18466.993, 24194.281),	//C2
				new Coord(25853.721, 18780.89),		//C3
				new Coord(37790.483, 14619.119),	//C4
				new Coord(56118.833, 7845.725),		//C5
				new Coord(67821.448, 5293.879),		//C6
				new Coord(89942.131, 7314.48),		//C7
				new Coord(108638.817, 36795.369)};	//C8
		
		nodeID +=1;
		currentNode = nodeID;
		
		if (nodeID<9) { // Select and place C0,C1,C2,...,C8 as counters
			currentNodeCoord = getConvertedCoord(counterLocation[currentNode]);
		}
		
		else {
			switch (dayTimeID) {
			case 1:
				currentNodeCoord = getNoonInitialLocation();
				break;
			case 2:
				currentNodeCoord = getEveningInitialLocation();
				break;
			case 3:
				currentNodeCoord = getNightInitialLocation();
				break;
			default:
				currentNodeCoord = getMorningInitialLocation();
			}
		}
		
		n = map.getNodeByCoord(currentNodeCoord);
		this.lastMapNode = n;
		
		System.out.printf("\nCoord of host " + currentNodeID + 
				"%d: (%f, %f)", nodeID, currentNodeCoord.getX(), currentNodeCoord.getY());
		
		return currentNodeCoord;
	}
	
	/**
	 * Returns the hosts' initial coordinates based on the mobility pattern of April 11th, morning.
	 * @return initial location of a host for morning scenario
	 */
	public Coord getMorningInitialLocation() {
		Coord currentNodeCoord;
		
		if (nodeID>56) {
			Coord loc = new Coord(113513.213, 40405.067);	//Kirkju
			currentNodeCoord = getConvertedCoord(loc);
		}
		
		else {
			Coord loc = new Coord(310.118, 47616.681);		//Hella
			currentNodeCoord = getConvertedCoord(loc);	
		}
		
		return currentNodeCoord;
	}
	
	/**
	 * Returns the hosts' initial coordinates based on the mobility pattern of April 10th, noon.
	 * @return initial location of a host for noon scenario
	 */
	public Coord getNoonInitialLocation() {
		String currentNodeID = host.getNodeID();
		Coord currentNodeCoord;
		Coord[] noonInitLocations = {
				new Coord(310.118, 47616.681),		//Hella for gid A,B,T,N
				new Coord(37790.483, 14619.119),	//C4 for gid E,W
				new Coord(56118.833, 7845.725),		//C5 for gid G
				new Coord(89942.131, 7314.48),		//C7 for gid F
				new Coord(113513.213, 40405.067)};	//Kirkju for gid Y
		
		switch (currentNodeID) {
		case "Y":
			currentNodeCoord = getConvertedCoord(noonInitLocations[4]);
			break;
		case "G":
			currentNodeCoord = getConvertedCoord(noonInitLocations[2]);
			break;
		case "F":
			currentNodeCoord = getConvertedCoord(noonInitLocations[3]);
			break;
		case "E":
			currentNodeCoord = getConvertedCoord(noonInitLocations[1]);
			break;
		case "W":
			currentNodeCoord = getConvertedCoord(noonInitLocations[1]);
			break;
		default: currentNodeCoord = getConvertedCoord(noonInitLocations[0]);	//For A,B,T,N
		}
		
		return currentNodeCoord;
	}
	
	/**
	 * Returns the hosts' initial coordinates based on the mobility pattern of April 10th, evening.
	 * @return initial location of a host for evening scenario
	 */
	public Coord getEveningInitialLocation() {
		String currentNodeID = host.getNodeID();
		Coord currentNodeCoord;
		Coord[] eveningInitLocations = {
				new Coord(310.118, 47616.681),		//Hella for gid A,B,T,N
				new Coord(56118.833, 7845.725),		//C5 for gid G
				new Coord(89942.131, 7314.48),		//C7 for gid E
				new Coord(113513.213, 40405.067)};	//Kirkju for gid Y
		
		switch (currentNodeID) {
		case "Y":
			currentNodeCoord = getConvertedCoord(eveningInitLocations[3]);
			break;
		case "G":
			currentNodeCoord = getConvertedCoord(eveningInitLocations[1]);
			break;
		case "E":
			currentNodeCoord = getConvertedCoord(eveningInitLocations[2]);
			break;
		default: currentNodeCoord = getConvertedCoord(eveningInitLocations[0]); //For A,B,T,N
		}
		
		return currentNodeCoord;
	}
	
	/**
	 * Returns the hosts' initial coordinates based on the mobility pattern of April 10th, evening.
	 * @return initial location of a host for evening scenario
	 */
	public Coord getNightInitialLocation() {
		String currentNodeID = host.getNodeID();
		Coord currentNodeCoord;
		Coord[] nightInitLocations = {
				new Coord(310.118, 47616.681),		//Hella for gid A,B,T,N
				new Coord(25853.721, 18780.89),		//C3 for gid E,W
				new Coord(56118.833, 7845.725),		//C5 for gid G
				new Coord(89942.131, 7314.48),		//C7 for gid F
				new Coord(113513.213, 40405.067)};	//Kirkju for gid Y
		
		switch (currentNodeID) {
		case "Y":
			currentNodeCoord = getConvertedCoord(nightInitLocations[4]);
			break;
		case "E":
			currentNodeCoord = getConvertedCoord(nightInitLocations[1]);
			break;
		case "W":
			currentNodeCoord = getConvertedCoord(nightInitLocations[1]);
			break;
		case "G":
			currentNodeCoord = getConvertedCoord(nightInitLocations[2]);
			break;
		case "F":
			currentNodeCoord = getConvertedCoord(nightInitLocations[3]);
			break;
		default: currentNodeCoord = getConvertedCoord(nightInitLocations[0]); //For A,B,T,N
		}
		
		return currentNodeCoord;
	}
	
	/**
	 * Returns map node types that are OK for this movement model in an array
	 * or null if all values are considered ok
	 * @return map node types that are OK for this movement model in an array
	 */
	protected int[] getOkMapNodeTypes() {
		return okMapNodeTypes;
	}
	
	@Override
	public Path getPath() {
		Path p = new Path(generateSpeed());
		MapNode curNode = lastMapNode;
		MapNode prevNode = lastMapNode;
		MapNode nextNode = null;	
		List<MapNode> neighbors;
		Coord nextCoord;
		
		assert lastMapNode != null: "Tried to get a path before placement";
		
		// start paths from current node 
		p.addWaypoint(curNode.getLocation());
		
		int pathLength = rng.nextInt(maxPathLength-minPathLength) + 
			minPathLength;

		for (int i=0; i<pathLength; i++) {
			neighbors = curNode.getNeighbors();
			Vector<MapNode> n2 = new Vector<MapNode>(neighbors);
			if (!this.backAllowed) {
				n2.remove(prevNode); // to prevent going back
			}	
				
			if (okMapNodeTypes != null) { //remove neighbor nodes that aren't ok
				for (int j=0; j < n2.size(); ){
					if (!n2.get(j).isType(okMapNodeTypes)) {
						n2.remove(j);
					}
					else {
						j++;
					}
				}
			}
			
			if (n2.size() == 0) { // only option is to go back
				nextNode = prevNode;
			}
			else { // choose a random node from remaining neighbors
				nextNode = n2.get(rng.nextInt(n2.size()));
			}
			
			prevNode = curNode;
		
			nextCoord = nextNode.getLocation();
			curNode = nextNode;
			
			p.addWaypoint(nextCoord);
		}
		
		lastMapNode = curNode;

		return p;
	}
	
	/**
	 * Selects and returns a random node that is OK from a list of nodes.
	 * Whether node is OK, is determined by the okMapNodeTypes list.
	 * If okMapNodeTypes are defined, the given list <strong>must</strong>
	 * contain at least one OK node to prevent infinite looping.
	 * @param nodes The list of nodes to choose from.
	 * @return A random node from the list (that is OK if ok list is defined)
	 */
	protected MapNode selectRandomOkNode(List<MapNode> nodes) {
		MapNode n;
		do {
			n = nodes.get(rng.nextInt(nodes.size()));
		} while (okMapNodeTypes != null && !n.isType(okMapNodeTypes));

		return n;
	}
	
	/**
	 * Returns the SimMap this movement model uses
	 * @return The SimMap this movement model uses
	 */
	public SimMap getMap() {
		return map;
	}
	
	/**
	 * Reads a sim map from location set to the settings, mirrors the map and
	 * moves its upper left corner to origo.
	 * @return A new SimMap based on the settings
	 */
	private SimMap readMap() {
		SimMap simMap;
		Settings settings = new Settings(MAP_BASE_MOVEMENT_NS);
		WKTMapReader r = new WKTMapReader(true);
		
		if (cachedMap == null) {
			cachedMapFiles = new ArrayList<String>(); // no cache present
		}
		else { // something in cache
			// check out if previously asked map was asked again
			SimMap cached = checkCache(settings);
			if (cached != null) {
				nrofMapFilesRead = cachedMapFiles.size();
				return cached; // we had right map cached -> return it
			}
			else { // no hit -> reset cache
				cachedMapFiles = new ArrayList<String>();
				cachedMap = null;
			}
		}

		try {
			int nrofMapFiles = settings.getInt(NROF_FILES_S);

			for (int i = 1; i <= nrofMapFiles; i++ ) {
				String pathFile = settings.getSetting(FILE_S + i);
				cachedMapFiles.add(pathFile);
				r.addPaths(new File(pathFile), i);
			}
			
			nrofMapFilesRead = nrofMapFiles;
		} catch (IOException e) {
			throw new SimError(e.toString(),e);
		}

		simMap = r.getMap();
		checkMapConnectedness(simMap.getNodes());
		// mirrors the map (y' = -y) and moves its upper left corner to origo
		simMap.mirror();
		Coord offset = simMap.getMinBound().clone();		
		simMap.translate(-offset.getX(), -offset.getY());
		checkCoordValidity(simMap.getNodes());
		
		cachedMap = simMap;
		return simMap;
	}
	
	/**
	 * Checks that all map nodes can be reached from all other map nodes
	 * @param nodes The list of nodes to check
	 * @throws SettingsError if all map nodes are not connected
	 */
	private void checkMapConnectedness(List<MapNode> nodes) {
		Set<MapNode> visited = new HashSet<MapNode>();
		Queue<MapNode> unvisited = new LinkedList<MapNode>();
		MapNode firstNode;
		MapNode next = null;
		
		if (nodes.size() == 0) {
			throw new SimError("No map nodes in the given map");
		}
		
		firstNode = nodes.get(0);
		
		visited.add(firstNode);
		unvisited.addAll(firstNode.getNeighbors());
		
		while ((next = unvisited.poll()) != null) {
			visited.add(next);
			for (MapNode n: next.getNeighbors()) {
				if (!visited.contains(n) && ! unvisited.contains(n)) {
					unvisited.add(n);
				}
			}
		}
		
		if (visited.size() != nodes.size()) { // some node couldn't be reached
			MapNode disconnected = null;
			for (MapNode n : nodes) { // find an example node
				if (!visited.contains(n)) {
					disconnected = n;
					break;
				}
			}
			throw new SettingsError("SimMap is not fully connected. Only " + 
					visited.size() + " out of " + nodes.size() + " map nodes " +
					"can be reached from " + firstNode + ". E.g. " + 
					disconnected + " can't be reached");
		}
	}
	
	/**
	 * Checks that all coordinates of map nodes are within the min&max limits
	 * of the movement model
	 * @param nodes The list of nodes to check
	 * @throws SettingsError if some map node is out of bounds
	 */
	private void checkCoordValidity(List<MapNode> nodes) {
		 // Check that all map nodes are within world limits
		for (MapNode n : nodes) {
			double x = n.getLocation().getX();
			double y = n.getLocation().getY();
			if (x < 0 || x > getMaxX() || y < 0 || y > getMaxY()) {
				throw new SettingsError("Map node " + n.getLocation() + 
						" is out of world  bounds "+
						"(x: 0..." + getMaxX() + " y: 0..." + getMaxY() + ")");
			}
		}
	}
	
	/**
	 * Checks map cache if the requested map file(s) match to the cached
	 * sim map
	 * @param settings The Settings where map file names are found 
	 * @return A cached map or null if the cached map didn't match
	 */
	private SimMap checkCache(Settings settings) {
		int nrofMapFiles = settings.getInt(NROF_FILES_S);

		if (nrofMapFiles != cachedMapFiles.size() || cachedMap == null) {
			return null; // wrong number of files
		}
		
		for (int i = 1; i <= nrofMapFiles; i++ ) {
			String pathFile = settings.getSetting(FILE_S + i);
			if (!pathFile.equals(cachedMapFiles.get(i-1))) {
				return null;	// found wrong file name
			}
		}
		
		// all files matched -> return cached map
		return cachedMap;
	}
	
	@Override
	public MyMapBasedMovement replicate() {
		return new MyMapBasedMovement(this);
	}
	
	public Coord getLastLocation() {
		if (lastMapNode != null) {
			return lastMapNode.getLocation();
		} else {
			return null;
		}
	}

	public void setLocation(Coord lastWaypoint) {
		// TODO: This should be optimized
		MapNode nearest = null;
		double minDistance = Double.MAX_VALUE;
		Iterator<MapNode> iterator = getMap().getNodes().iterator();
		while (iterator.hasNext()) {
			MapNode temp = iterator.next();
			double distance = temp.getLocation().distance(lastWaypoint);
			if (distance < minDistance) {
				minDistance = distance;
				nearest = temp;
			}
		}
		lastMapNode = nearest;
	}

	public boolean isReady() {
		return true;
	}
	
}
