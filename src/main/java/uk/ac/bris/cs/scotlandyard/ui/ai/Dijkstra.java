package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.*;

import java.util.*;

public class Dijkstra {
    static Integer calculateDistance(List<Integer> detectivesLocation, Integer mrXLocation, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
//		List<Integer> detectivesLocation = getDetectivesLocation(board);
//		int mrXLocation = updateLocation(mrXMove);
        System.out.println("DETECTIVES' LOC:" + detectivesLocation);
        int size;
//		System.out.println("MRX's LOC: " + mrXLocation);
        List<List<Integer>> shortPath = new ArrayList<>();
        List<List<Integer>> allPath = new ArrayList<>();
        List<List<Integer>> longPath = new ArrayList<>();
        //calculate the distance from each detective to mrX's expected location
        for (Integer detectiveLocation : detectivesLocation) {
            Map<Integer, Integer> distance = new HashMap<>();
            Map<Integer, Integer> preNode = new HashMap<>();
            //using Dijkstra's algorithm
            for (Integer node : graph.nodes()) {
                distance.put(node, Integer.MAX_VALUE);
                preNode.put(node, null);
            }
            //setting the distance from source to source as 0
            distance.put(detectiveLocation, 0);

            PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));
            queue.add(detectiveLocation);
            //using Dijkstra's algorithm to find the shortest path from the detective to mrX
            while (!queue.isEmpty()) {
                Integer currentNode = queue.poll();
                if (currentNode.equals(mrXLocation)) break;
                for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
                    Integer neighbour = edge.adjacentNode(currentNode);
//                    Integer weight = Moriarty.transportationCost(currentNode, mrXLocation, graph);
                    int newDistance = distance.get(currentNode) + 1;
                    if (newDistance < distance.get(neighbour)) {
                        distance.replace(neighbour, newDistance);
                        preNode.replace(neighbour, currentNode);
                        queue.remove(neighbour);
                        queue.add(neighbour);
                    }
                }
            }
            //store the path from detective's location to mrX's expected location
            List<Integer> path = new ArrayList<>();
            Integer node = mrXLocation;
            while (node != null) {
                path.add(node);
                node = preNode.get(node);
            }
            //focus on the detectives who are close enough to consider
            if (path.size() < 5) {
                shortPath.add(path);
            }
//			else{
//				longPath.add(path);
//			}
//
//			if (shortPath.size() < 4){
//				size = longPath.size();
//			}
            //add every detective's path on the list
            allPath.add(path);
//			System.out.println("PATH: " + path);
            System.out.println("PATH: " + path + " " + path.size());
        }
        //calculate the total size of the paths
        size = shortPath.isEmpty() ? allPath.stream().mapToInt(List::size).sum() : shortPath.stream().mapToInt(List::size).sum();
        System.out.println("SIZE: " + size);
        System.out.println("===================================================================================");
        return size;
    }
}