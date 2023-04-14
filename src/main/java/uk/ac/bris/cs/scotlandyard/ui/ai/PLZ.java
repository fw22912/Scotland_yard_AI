package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class PLZ implements Ai {

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
		Move finalMove = mrXBestMove(gameState.getSetup().graph, board, 7);
		return finalMove;
	}

	//return an updated board after particular move
	private Board updatedBoard(Board board, Move move) {
		return ((Board.GameState) board).advance(move);
	}

	//MiniMax
	public Integer minimax(Board board, Move move, int depth, int alpha, int beta, boolean checkMrX) {
		if (depth == 0 || !board.getWinner().isEmpty()) {
			return updateLocation(move);
		}
		if (checkMrX) {
			int maxEval = Integer.MIN_VALUE;
			for (Move child : board.getAvailableMoves().asList()) {
				int eval = minimax(board, child, depth - 1, alpha, beta, false);
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, maxEval);
				if (beta <= alpha) {
					break;
				}
			}
			return maxEval;
		}
		else {
			int minEval = Integer.MAX_VALUE;
			for (Move child : board.getAvailableMoves().asList()) {
				int eval = minimax(board, child, depth - 1, alpha, beta, true);
				minEval = Math.min(minEval, eval);
				beta = Math.min(minEval, beta);
				if (beta <= alpha) {
					break;
				}
			}
			return minEval;
		}
	}

	private Integer SingleOrDouble(Board board, Move move){
		return move.accept(new Move.Visitor<>() {
			final Board.GameState gameState = (Board.GameState) board;
			@Override
			public Integer visit(Move.SingleMove move) {
				return calculateDistance(board, move, gameState.getSetup().graph);
			}

			@Override
			public Integer visit(Move.DoubleMove move) {
				// TODO when double move
//				return calculateDistance(board, move, gameState.getSetup().graph);
				return 0;
			}
		});
	}



	//chooses the best move for MrX
	private List<Move> getOptimalMoves(Board board, int depth) {
		int minEval = (int) Double.POSITIVE_INFINITY;
		int alpha = (int) Double.NEGATIVE_INFINITY;
		int beta = (int) Double.POSITIVE_INFINITY;
		List<Move> optimalMoves = new ArrayList<>();

		for (Move move : board.getAvailableMoves()) {
			Board updated = updatedBoard(board, move);
			//do minimax with updatedBoard after designated move
			int eval = minimax(updated, move, depth, alpha, beta, true);
			if (minEval > eval) {
				minEval = eval;
				optimalMoves.clear();
				optimalMoves.add(move);
			} else if (minEval == eval) {
				optimalMoves.add(move);
			}
		}
		return optimalMoves;
	}


	//return the best move
	private Move mrXBestMove(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Board board, int depth) {
		Move finalMove;
		int score = 0;
		Random ran = new Random();
		List<Move> optimalMoves = getOptimalMoves(board, depth);
		List<Move> noAdjacent = checkAdjacent(board, optimalMoves);
		List<Move> highestScore = new ArrayList<>();
		List<Move> finalMoves = new ArrayList<>();
		//iterate through
		if(noAdjacent.isEmpty()){
			int randomIndex = ran.nextInt(optimalMoves.size());
			finalMove = optimalMoves.get(randomIndex);
		}
		else{
			for (Move move : noAdjacent) {
				int thisScore = SingleOrDouble(board, move);
				if(thisScore > score){
					highestScore.clear();
					score = thisScore;
					highestScore.add(move);
				}
				else if(thisScore == score){
					highestScore.add(move);
				}
			}

			int score2 = 0;
			//If there are more than possible moves, randomly choose among those moves

			if(highestScore.size() > 1){
				for(Move move : highestScore) {
					int thisScore2 = transportationCost(board, updateTicket(move)) + graph.adjacentNodes(updateLocation(move)).size();
					if (thisScore2 > score2){
						score2 = thisScore2;
						finalMoves.clear();
						finalMoves.add(move);
					}
					else if(thisScore2 == score2){
						finalMoves.add(move);
					}
				}
				int randomIndex = ran.nextInt(finalMoves.size());
				finalMove = finalMoves.get(randomIndex);
			}
			else finalMove = highestScore.get(0);
		}
		return finalMove;
	}


	//a function that checks whether this move is safe or not
	//it returns a list of the nodes that are not adjacent to detectives' locations
	private List<Move> checkAdjacent(Board board, List<Move> bestMoves){
		List<Move> possible = new ArrayList<>();
		for(Move move : bestMoves){
			Set<Integer> occupation = detectiveAdjacent(move, board);
			//if there are no detectives around add the move to the list
			if(occupation.add(updateLocation(move))){
				possible.add(move);
			}
		}
		return possible;
	}



	//a method that returns all adjacent nodes from detectives' current location
	private Set<Integer> detectiveAdjacent(Move move, Board board){
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
			final List<ScotlandYard.Ticket> newTicket = new ArrayList<>();
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


	private Integer transportationCost(Board board, List<ScotlandYard.Ticket> tickets) {
		List<LogEntry> mrXLog = board.getMrXTravelLog();
		int ticketVal = 0;
		for(ScotlandYard.Ticket ticket : tickets){
			if(ticket.equals(ScotlandYard.Ticket.TAXI)) ticketVal += 2;
			if(ticket.equals(ScotlandYard.Ticket.BUS)) ticketVal += 4;
			if(ticket.equals(ScotlandYard.Ticket.UNDERGROUND)) ticketVal += 8;
			if (mrXLog.size() != 0)
				if(ticket.equals(ScotlandYard.Ticket.SECRET)) {
				if(board.getSetup().moves.get(mrXLog.size() - 1)) {ticketVal += 20;}
			}
		}
		return ticketVal;
	}


	//Scoring method, uses Dijkstra's algorithm
	//It returns the distance from the detectives' location to mrX's destination
	private Integer calculateDistance(Board board, Move move, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
		List<Integer> detectivesLocation = getDetectivesLocation(board);
		int size;
		List<List<Integer>> shortPath = new ArrayList<>();
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
			if(path.size() < 5){
				shortPath.add(path);
			}
			allPath.add(path);
		}
		size = shortPath.isEmpty() ? allPath.stream().mapToInt(List::size).sum() : shortPath.stream().mapToInt(List::size).sum();
		return size;
	}
}