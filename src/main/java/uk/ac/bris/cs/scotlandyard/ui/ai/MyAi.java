package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.awt.*;
import java.util.*;
import java.util.List;
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
import org.checkerframework.checker.units.qual.A;
import org.w3c.dom.Node;
import uk.ac.bris.cs.scotlandyard.event.GameOver;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull
	@Override
	public String name() {
		return "PLZ";
	}

	@Nonnull
	@Override
	public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		Board.GameState gameState = (Board.GameState) board;
		return mrXBestMove(gameState.getSetup().graph, board, 3);
	}

	//updating the board status
	private Board updatedBoard(Board board, Move move) {
		return ((Board.GameState) board).advance(move);
	}

	//MiniMax method(alpha beta pruning)
	private Integer miniMax(Board board, int depth, int alpha, int beta, Boolean checkMrX) {
		Board.GameState gameState = (Board.GameState) board;
		//when the game is over
		if (depth == 0 || !board.getWinner().isEmpty()) {return (int)Double.NEGATIVE_INFINITY;}
		//if the player is mrX(maximising player)
		if (checkMrX) {
			int maxEval = (int) Double.NEGATIVE_INFINITY;
			//iterate through all the moves and choose the maximum value
			for (Move move : board.getAvailableMoves()) {
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
				//
				if (beta <= alpha) break;
			}
			return maxEval;
		}
		//if the players are detectives choose the minimum value
		else {
			int minEval = (int) Double.POSITIVE_INFINITY;
			for (Move move : board.getAvailableMoves()) {
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, true);
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, eval);
				if (beta <= alpha) break;
			}
			return minEval;
		}
	}

	//a method that choose the best move based on the score
	private Move mrXBestMove(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Board board, int depth) {
		int maxEval = (int) Double.NEGATIVE_INFINITY;
		int alpha = (int) Double.NEGATIVE_INFINITY;
		int beta = (int) Double.POSITIVE_INFINITY;
		List<Move> bestMoves = new ArrayList<>();
		//iterate through all possible moves and
		for (Move move : board.getAvailableMoves()) {
			Board updated = updatedBoard(board, move);
			int eval = miniMax(board, depth - 1, alpha, beta, false);
			if (maxEval < eval) {
				maxEval = eval;
				bestMoves.clear();
				bestMoves.add(move);
			} else if (maxEval == eval) {
				bestMoves.add(move);
			}
		}

		Move finalMove = null;
		int score = 0;

		List<Move> finalMoves = checkAdjacent(board, bestMoves);
		List<Move> possible = new ArrayList<>();

		for (Move move : finalMoves) {
			int thisScore = calculateDistance(board, move, graph);
			if(thisScore > score){
				possible.clear();
				score = thisScore;
				possible.add(move);
			}
			else if(thisScore == score){
				possible.add(move);
			}
		}

		if(possible.size() > 1){
			Random ran = new Random();
			int randomIndex = ran.nextInt(possible.size());
			finalMove = possible.get(randomIndex);
		}
		else if(possible.isEmpty()){
			Random ran = new Random();
			int randomIndex = ran.nextInt(bestMoves.size());
			finalMove = bestMoves.get(randomIndex);
		}
		else finalMove = possible.get(0);
		return finalMove;
	}

	//a function that checks whether this move is safe or not
	private List<Move> checkAdjacent(Board board, List<Move> bestMoves){
		Board.GameState gameState = (Board.GameState) board;
		Set<Integer> occupation = detectiveAdjacent(board, gameState.getSetup().graph);
		List<Move> finalMoves = new ArrayList<>();
		for(Move move : bestMoves){
			Integer currentNode = updateLocation(move);
			//if there are no detectives around add the move to the list
			if(occupation.add(updateLocation(move))){
				finalMoves.add(move);
			}
		}
		return finalMoves;
	}

	//a method that returns all adjacent nodes from detectives' current location
	private Set<Integer> detectiveAdjacent(Board board, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
		List<Integer> currentLocation = returnLocation(board);
		Set<Integer> possibleLocation = new HashSet<>();
		for(Integer node : currentLocation){
			possibleLocation.addAll(graph.adjacentNodes(node));
		}
		return possibleLocation;
	}


	//a helper method that gathers all detectives' locations
	private static List<Integer> returnLocation(Board board) {
		List<Integer> locations = new ArrayList<>();
		for (Piece piece : board.getPlayers()) {
			if (!piece.isMrX()) {
				locations.add(board.getDetectiveLocation((Piece.Detective) piece).get());
			}
		}
		return locations;
	}


	//helper method that returns the destination of the move
	public static Integer updateLocation(Move move) {
		return move.accept(new Move.Visitor<>() {
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

	//helper method that returns used tickets for the move
	public static List<ScotlandYard.Ticket> updateTicket(Move move){
		return move.accept(new Move.Visitor<>() {
			List<ScotlandYard.Ticket> newTicket = new ArrayList<>();
			@Override
			public List<ScotlandYard.Ticket> visit(Move.SingleMove move) {
				newTicket.add(move.ticket);
				return newTicket;
			}
			@Override
			public List<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
				newTicket.add(move.ticket1);
				newTicket.add(move.ticket2);
				return newTicket;
			}
		});
	}


	//Weighting transportation tickets
	private Integer transportationCost(Board board, ScotlandYard.Ticket ticket) {
		int ticketVal = 0;
		switch (ticket) {
			case TAXI -> ticketVal += 1;
			case BUS -> ticketVal += 4;
			case UNDERGROUND -> ticketVal += 7;
			case DOUBLE -> ticketVal -= 6;
			case SECRET -> ticketVal -= 6;
		}
		return ticketVal;
	}

	private List<Move> checkAdjacent(List<Move> bestMoves){

		return null;
	}

	//Scoring method, uses Dijkstra's algorithm
	//It returns the distance from the detectives' location to mrX's destination
	private Integer calculateDistance(Board board,
									  Move move,
									  ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
		List<Integer> detectiveLocation = returnLocation(board);
		int totalVal = 0;

		//calculating the distance from every detective's location
		for (Integer location : detectiveLocation) {
			// Instantiate distance
			Map<Integer, Integer> distance = new HashMap<>();
			// Instantiate preNode
			Map<Integer, Integer> preNode = new HashMap<>();

			//put the maximum value of incident edges(values) so that we could overwrite the
			//smaller value everytime they show up
			for (Integer node : graph.nodes()) {
				distance.put(node, Integer.MAX_VALUE);
				preNode.put(node, null);
			}
			distance.put(location, 0);
			// Create priority queue and add source node with distance 0
			PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));
			queue.add(location);

			// Setting the destination
			int destination = updateLocation(move);

			// Run Dijkstra's Algorithm
			while (!queue.isEmpty()) {
				Integer currentNode = queue.poll();
				if (currentNode.equals(destination)) break;

				for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
					Integer neighbour = edge.adjacentNode(currentNode);
					// Calculate new distance
					int ticketVal = 0;

					//Calculating the ticket value
					for(ScotlandYard.Ticket ticket : updateTicket(move)){
						ticketVal += transportationCost(board, ticket);
					}

					//add up the ticket value to newDistance
					int newDistance = distance.get(currentNode) + ticketVal;

					//update the shortest path to mrX's location
					if (newDistance < distance.get(neighbour)) {
						// Update distance and previous node
						distance.put(neighbour, newDistance);
						preNode.put(neighbour, currentNode);
						queue.remove(neighbour);
						queue.add(neighbour);
					}
				}
			}
			totalVal += distance.get(updateLocation(move));
		}
		return totalVal;
	}
}