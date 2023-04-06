package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Node;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "PLZ"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		ImmutableSet<Piece> allPlayers = board.getPlayers();
		var moves = board.getAvailableMoves().asList();
		var currentState = (Board.GameState) board;
		return moves.get(new Random().nextInt(moves.size()));
	}

	//returning a new game state with one move ahead
	private Board updatedBoard(Board board, Move move){
		return ((Board.GameState) board).advance(move);
	}

	//check whether the player is mrX or not
	private Boolean PlayerIsMrX(Piece piece){
		if(piece.isMrX()) return true;
		else return false;
	}

	//MiniMax
	private Integer miniMax(Board board, int depth, int alpha, int beta, Boolean checkMrX){
		if(depth == 0 || !board.getWinner().isEmpty()){
//			getScore()
//			return scores from a given board
		}
		if(checkMrX){
			int maxEval = (int)Double.NEGATIVE_INFINITY;
			for(Move move : board.getAvailableMoves()){
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				//can I compare double and integer!!!!
				maxEval = Math.max(maxEval, eval);
				if(beta <= alpha) break;
			}
			return maxEval;
		}
		else{
			int minEval = (int)Double.POSITIVE_INFINITY;
			for(Move move : board.getAvailableMoves()){
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, true);
				minEval = Math.min(minEval, eval);
				if(beta >= alpha) break;
			}
			return minEval;
		}
	}

	//apply miniMax
	//assign score to each location
	private Move bestMove(Board board, int depth){
			//setting maxVal, max, min values
		int maxVal = (int)Double.NEGATIVE_INFINITY;
		int alpha = (int)Double.NEGATIVE_INFINITY;
		int beta = (int)Double.POSITIVE_INFINITY;

		for(Move move : board.getAvailableMoves()){
			Board newBoard = updatedBoard(board, move);
			int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
		}
		return null;
	}


	//helper method that returns a list of detectives' pieces
	private Set<Piece> getDetectives(List<Piece> allPlayers){
		Set<Piece> allDetectives = new HashSet<>();
		for(Piece piece : allPlayers){
			if(piece.isDetective()){
				allDetectives.add(piece);
			}
		}
		return allDetectives;
	}

	//helper method that returns mrX's piece
	private Set<Piece> getMrX(List<Piece> allPlayers){
		Set<Piece> thisMrX = new HashSet<>();
		for(Piece piece : allPlayers){
			if(piece.isMrX()){
				thisMrX.add(piece);
			}
		}
		return thisMrX;
	}


	//helper method for getting player's location
	public static Integer updateLocation(Move move){
		return move.accept(new Move.Visitor<>() {
			List<Integer> newDestination = new ArrayList<>();
			@Override
			public Integer visit(Move.SingleMove move) {
				return move.destination;
			}
			@Override
			public Integer visit(Move.DoubleMove move) {
				return move.destination2;
			}
		});
	}

	private static Integer getTransportCost(Integer destination,Integer source,ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
		int ticketVal = 0;
		for(ScotlandYard.Transport t : graph.edgeValueOrDefault(source, destination, ImmutableSet.of())){
			switch (t.requiredTicket()){
				case TAXI -> ticketVal += 1;
				case BUS -> ticketVal += 2;
				case UNDERGROUND -> ticketVal += 3;
				case SECRET -> ticketVal += 4;
			}
		}
		return ticketVal;
	}


	static List<Integer> score(
			ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
			List<Integer> sources,
			Integer destination,
			Board board) {

		Map<Integer, Integer> distance = new HashMap<>();
		List<Integer> shortestRoute = new ArrayList<>();
		int score , count= 0;
		//Fill every distance with maximum value
		graph.nodes().forEach(node -> distance.put(node, Integer.MAX_VALUE));
		graph.nodes().forEach(node -> shortestRoute.add(node, null));

		//add the source on distance
		sources.forEach(source -> distance.put(source, 0));

		//
		PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));
		sources.forEach(source -> queue.add(source));

		List<Integer> possibleNodes = new ArrayList<>();
		for(Move move : board.getAvailableMoves()){
			possibleNodes.add(updateLocation(move));
		}
		//Running Dijkstra's algorithm
		Integer current = 0;

		while(distance.size() > current){
			Integer allNodes = queue.poll();
			//finding the shortest path to mrX
			for(Integer movedLocation : possibleNodes){
				if(!distance.containsKey(movedLocation)){
					Integer value = distance.get(current) + getTransportCost(destination, sources.get(current),graph);
					distance.put(destination, value);
				}
				else{

				}
			}
			count++;
		}

//		int current =
//		for(Integer possible : possibleNodes){
//			int possibleDestination = possible;
//			if (!distance.containsKey(possibleDestination)) {
//				distance.put(possibleDestination, getTransportCost(possibleDestination, current, graph))
//			}
//		}
	return null;
	}

	private static int edgeToDistance(EndpointPair<Integer> edge) {
//		ImmutableSet<ScotlandYard.Transport> transports = edge.value().get();
//		return transports.stream().mapToInt(ScotlandYard.Transport::getTransportCost).sum();
		return 0;
	}



}
