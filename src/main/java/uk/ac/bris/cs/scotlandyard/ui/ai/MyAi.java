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
		Move finalMove = mrXBestMove(gameState.getSetup().graph, board, 5);
		System.out.println("=====================================LOOP ENDS================================================");
		return finalMove;
	}

	//return an updated board after particular move
	private Board updatedBoard(Board board, Move move) {
		return ((Board.GameState) board).advance(move);
	}

	//MiniMax
	private Integer miniMax(Board board, int depth, int alpha, int beta, Boolean checkMrX) {
		Board.GameState gameState = (Board.GameState) board;
		if (depth == 0 || !board.getWinner().isEmpty()) {
			return (int)Double.NEGATIVE_INFINITY;
		}
		if (checkMrX) {
			int maxEval = (int) Double.NEGATIVE_INFINITY;
			for (Move move : board.getAvailableMoves()) {
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				//can I compare double and integer!!!!
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, maxEval);
				if (beta <= alpha) break;
			}
			return maxEval;
		}
		else {
			int minEval = (int) Double.POSITIVE_INFINITY;
			for (Move move : board.getAvailableMoves()) {
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, true);
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, minEval);
				if (beta <= alpha) break;
			}
			return minEval;
		}
	}


	//chooses the best move for MrX
	private List<Move> scoreToMoves(Board board, int depth) {
		int minEval = (int) Double.POSITIVE_INFINITY;
		int alpha = (int) Double.NEGATIVE_INFINITY;
		int beta = (int) Double.POSITIVE_INFINITY;
		List<Move> firstMoves = new ArrayList<>();

		for (Move move : board.getAvailableMoves()) {
			Board updated = updatedBoard(board, move);
			//do minimax with updatedBoard after designated move
			int eval = miniMax(updated, depth - 1, alpha, beta, true);
			if (minEval > eval) {
				minEval = eval;
				firstMoves.clear();
				firstMoves.add(move);
			} else if (minEval == eval) {
				firstMoves.add(move);
			}
		}
		return firstMoves;
	}


	//return the best move
	private Move mrXBestMove(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Board board, int depth) {
		Move finalMove;
		int score = 0;
		List<Move> firstMoves = scoreToMoves(board, depth);
		System.out.println("firstMoves: " + firstMoves);
		List<Move> noAdjacent = checkAdjacent(board, firstMoves);
		System.out.println("noAdjacent: " + noAdjacent);
		List<Move> highestScore = new ArrayList<>();
		List<Move> finalMoves = new ArrayList<>();
		//iterate through
		for (Move move : noAdjacent) {
			int thisScore = calculateDistance(board, move, graph);
			System.out.println("MOVE: " + move + "  score: " + thisScore);
			if(thisScore > score){
				highestScore.clear();
				score = thisScore;
				highestScore.add(move);
			}
			else if(thisScore == score){
				highestScore.add(move);
			}
		}
		System.out.println("highestScore: " + highestScore);
		Random ran = new Random();
		int score2 = 0;
		//If there are more than possible moves, randomly choose among those moves
		if(noAdjacent.isEmpty()){
			int randomIndex = ran.nextInt(firstMoves.size());
			finalMove = firstMoves.get(randomIndex);
		}
		else if(highestScore.size() > 1){
			// TODO : 파이널무브즈 수 하나 이상일 때 겟어베일러블무브 수 비교해서 가장 높은 걸로
			for(Move move : highestScore) {
				int thisScore2 = graph.adjacentNodes(updateLocation(move)).size();
				if (thisScore2 > score2){
					finalMoves.clear();
					score2 = thisScore2;
					finalMoves.add(move);
				}
				else if(thisScore2 == score2){
					finalMoves.add(move);
//					System.out.println("Among highestMove: " + move + " getAvailablenodescore: " + thisScore2);
				}
			}
			int randomIndex = ran.nextInt(finalMoves.size());
			System.out.println("finalMoves: " + finalMoves);
			finalMove = finalMoves.get(randomIndex);
		}
		else finalMove = highestScore.get(0);
		System.out.println("finalMove: " + finalMove);
		return finalMove;
	}

//	private Integer numberOfAvailableMoves(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Move move) {
//		return graph.incidentEdges(updateLocation(move)).size();
//	}

	//a function that checks whether this move is safe or not
	//it returns a list of the nodes that are not adjacent to detectives' locations
	private List<Move> checkAdjacent(Board board, List<Move> bestMoves){
		Board.GameState gameState = (Board.GameState) board;
		List<Move> possible = new ArrayList<>();
		for(Move move : bestMoves){
			Set<Integer> occupation = detectiveAdjacent(move, board, gameState.getSetup().graph);
			//if there are no detectives around add the move to the list
			if(occupation.add(updateLocation(move))){
				possible.add(move);
			}
		}
		return possible;
	}


	//a method that returns all adjacent nodes from detectives' current location
	private Set<Integer> detectiveAdjacent(Move move, Board board, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
		Board newBoard = updatedBoard(board, move);
		Set<Integer> availableLocation = new HashSet<>();
		//places where detectives can go
		for(Move move2 : newBoard.getAvailableMoves()){
			availableLocation.add(updateLocation(move2));
		}
		return availableLocation;
	}


	//a helper method that gathers all detectives' locations
	private static List<Integer> getDetectivesLocation(Board board) {
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
	private Integer transportationCost(Board board, List<ScotlandYard.Ticket> tickets) {
		int ticketVal = 0;
		for(ScotlandYard.Ticket ticket : tickets){
			switch (ticket) {
				case TAXI -> ticketVal += 2;
				case BUS -> ticketVal += 4;
				case UNDERGROUND -> ticketVal += 8;
				case SECRET -> ticketVal += 5;
			}
		}
		return ticketVal;
	}


	//Scoring method, uses Dijkstra's algorithm
	//It returns the distance from the detectives' location to mrX's destination
	private Integer calculateDistance(Board board, Move move, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
		List<Integer> detectivesLocation = getDetectivesLocation(board);
		int totalVal = 0;
		int size = 0;
		List<List<Integer>> listOfPath = new ArrayList<>();
		List<List<Integer>> allPath = new ArrayList<>();

		for (Integer detectiveLocation : detectivesLocation) {
			Map<Integer, Integer> distance = new HashMap<>();
			Map<Integer, Integer> preNode = new HashMap<>();
			for (Integer node : graph.nodes()) {
				distance.put(node, Integer.MAX_VALUE);
				preNode.put(node, null);
			}
			distance.put(detectiveLocation, 0);
			PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));
			queue.add(detectiveLocation);
			int mrXLocation = updateLocation(move);
			while (!queue.isEmpty()) {
				Integer currentNode = queue.poll();
				if (currentNode.equals(mrXLocation)) break;
				for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
					Integer neighbour = edge.adjacentNode(currentNode);
					int newDistance = distance.get(currentNode);
					if (newDistance < distance.get(neighbour)) {
						distance.put(neighbour, transportationCost(board, updateTicket(move)));
						preNode.replace(neighbour, currentNode);
						queue.remove(neighbour);
						queue.add(neighbour);
					}
				}
			}
			List<Integer> path = new ArrayList<>();
			Integer node = mrXLocation;
			while(node != null){
				path.add(node);
				node = preNode.get(node);
			}
			if(path.size() < 4){
				listOfPath.add(path);
			}
			allPath.add(path);
			System.out.println("path: " + path);
		}
		size = listOfPath.isEmpty() ? allPath.stream().mapToInt(List::size).sum() : listOfPath.stream().mapToInt(List::size).sum();
		return size;
	}
}