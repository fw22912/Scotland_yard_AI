package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class Dijkstra {
    static Integer calculateDistance(List<Integer> detectivesLocation, Integer mrXLocation, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        int size;
        List<List<Integer>> shortPath = new ArrayList<>();
        List<List<Integer>> allPath = new ArrayList<>();
//        List<List<Integer>> longPath = new ArrayList<>();
        //calculate the distance from each detective to mrX's expected location
        for (Integer detectiveLocation : detectivesLocation) {
            Map<Integer, Integer> distance = new HashMap<>();
            Map<Integer, Integer> preNode = new HashMap<>();
            //iterating through all the nodes and adding the up to the empty list
            for (Integer node : graph.nodes()) {
                distance.put(node, Integer.MAX_VALUE);
                preNode.put(node, null);
            }
            //setting the distance from source to source as 0
            distance.put(detectiveLocation, 0);
            //storing the nodes in the graph that has not been visited
            PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));  //in ascending order
            queue.add(detectiveLocation);   //removing the head of the queue
            //using Dijkstra's algorithm to find the shortest path from the detective to mrX
            while (!queue.isEmpty()) {
                Integer currentNode = queue.poll();
                if (currentNode.equals(mrXLocation)) break;
                for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
                    Integer neighbour = edge.adjacentNode(currentNode);

//                  Integer weight = Moriarty.transportationCost(currentNode, mrXLocation, graph);
                    int newDistance = distance.get(currentNode) + 1;
                    if (newDistance < distance.get(neighbour)) {     //if new distance to neighbouring node is smaller than the current
                        distance.replace(neighbour, newDistance);    //update new distance
                        preNode.replace(neighbour, currentNode);   //updating the parent node for neighbouring node
                        queue.remove(neighbour);    //removing duplicates
                        queue.add(neighbour);     //re-adding to the queue with new smaller distance val
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
            if (path.size() < 5) {   //only add the path size is smaller than 5 to shortestPath
                shortPath.add(path);
            }
            allPath.add(path);
        }
        //calculate the total size of the paths
        size = shortPath.isEmpty() ? allPath.stream().mapToInt(List::size).sum() : shortPath.stream().mapToInt(List::size).sum();
        return size;
    }
}