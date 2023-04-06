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
		Move finalMove = mrXBestMove(graph, board, 3);
		return finalMove;
	}


	private Move bestMove(Board board, Player player, int depth) {
		return null;
	}

	private Board updatedBoard(Board board, Move move) {
		return ((Board.GameState) board).advance(move);
	}

	//MiniMax
	private Integer miniMax(Board board, int depth, int alpha, int beta, Boolean checkMrX) {
		if (depth == 0 || !board.getWinner().isEmpty()) {
//         getScore()
//         return scores from a given board
		}
		if (checkMrX) {
			int maxEval = (int) Double.NEGATIVE_INFINITY;
			for (Move move : board.getAvailableMoves()) {
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				//can I compare double and integer!!!!
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
				if (beta <= alpha) break;
			}
			return maxEval;
		} else {
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

	//chooses the best move for MrX
	private Move mrXBestMove(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Board board, int depth) {
		int maxEval = (int) Double.NEGATIVE_INFINITY;
		int alpha = (int) Double.NEGATIVE_INFINITY;
		int beta = (int) Double.POSITIVE_INFINITY;
		List<Move> bestMoves = new ArrayList<>();

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

		for (Move move : bestMoves) {
			int thisScore = calculateDistance(board, move, graph);
			if(thisScore > score){
				finalMove = move;
			}
		}
		return finalMove;
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
			case BUS -> ticketVal += 2;
			case TAXI -> ticketVal += 4;
			case UNDERGROUND -> ticketVal += 6;
			case SECRET -> ticketVal += 12;
		}
		return ticketVal;
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
