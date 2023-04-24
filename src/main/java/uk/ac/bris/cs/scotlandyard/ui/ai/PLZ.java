package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
		int maxEval = Integer.MIN_VALUE;
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		int depth = 3;
		List<Move> noAdjacent = checkAdjacent(board, board.getAvailableMoves().asList());
		List<Move> highestMoves = new ArrayList<>();
		List<Move> scoredMoves = new ArrayList<>();
		Move bestMove = null;

		Random ran = new Random();
		if (noAdjacent.isEmpty()) {
			int ranIndex = ran.nextInt(board.getAvailableMoves().size());
			System.out.println("=====================================NO ADJACENT===============================================");
			highestMoves.add(board.getAvailableMoves().asList().get(ranIndex));
		}
		else {
			//iterate through all the available moves and get a move with the highest minimax score
			for (Move move : noAdjacent) {
				Board updated = updatedBoard(board, move);
				//do minimax with updatedBoard after designated move
				int eval = minimax(updated, move, depth - 1, alpha, beta);
				if (maxEval < eval) {
					maxEval = eval;
					highestMoves.clear();
					highestMoves.add(move);
				} else if (maxEval == eval) {
					highestMoves.add(move);
				}
			}
		}
		return returnBestMove(board, highestMoves);
	}



	private Move returnBestMove(Board board, List<Move> highestMoves){
		Random ran = new Random();
		Board.GameState gameState = (Board.GameState) board;
		Move bestMove;
		List<Move> scoredMoves = new ArrayList<>();
		System.out.println("HIGHEST MOVE: " + highestMoves);
		int maxScore = Integer.MIN_VALUE;
		if(highestMoves.size() == 1) {
			bestMove = highestMoves.get(0);
		}
		else{
			for (Move scoredMove : highestMoves) {
				int score2 = transportationCost(board, updateTicket(scoredMove))
						+ gameState.getSetup().graph.adjacentNodes(updateLocation(scoredMove)).size()
						- DetectivesNearby(board, scoredMove);
				System.out.println("MOVE: " + scoredMove + " score: " + score2);
				if(score2 > maxScore){
					scoredMoves.clear();
					scoredMoves.add(scoredMove);
					maxScore = score2;
				}
				else if(score2 == maxScore) {scoredMoves.add(scoredMove);}
			}
			System.out.println("SCOREDMOVES: " + scoredMoves);
			//NEW LOOP
			List<Move> finalMove = new ArrayList<>();
			if (scoredMoves.size() == 1) {bestMove = scoredMoves.get(0);}
			else {
				finalMove.addAll(filterSingleDouble(board, scoredMoves));
				int ranFinal = ran.nextInt(finalMove.size());
				bestMove = finalMove.get(ranFinal);
				System.out.println("FINALMOVES: " + finalMove);
			}
		}
		System.out.println("BESTMOVE: " + bestMove);
		return bestMove;
	}


	//returns filtered Single or Double
	//if single exists, return only single and both otherwise
	private List<Move> filterSingleDouble(Board board, List<Move> scoredMoves){
		int totalVal = valueListMoves(board, scoredMoves);
		if(totalVal != 0) return getOnlySingle(board, scoredMoves);
		else return scoredMoves;
	}


	//filters and returns singleMove only
	private List<Move> getOnlySingle(Board board, List<Move> scoredMoves){
		List<Move> onlySingle = new ArrayList<>();
		for(Move move : scoredMoves) {
			if (filterDouble(board, move) == 1) {
				onlySingle.add(move);
			}
		}
		return onlySingle;
	}


	//returns the number of single moves
	private Integer valueListMoves(Board board, List<Move> scoredMoves){
		int totalVal = 0;
		for(Move move : scoredMoves){
			totalVal += filterDouble(board, move);
		}
		return totalVal;
	}


	//returns the number of the detectives that are located in the adjacent X 2 nodes
	private Integer DetectivesNearby(Board board, Move move){
//		List<Integer> listAdjacent = adjacentNodes(board, move);
		List<Integer> detectivesLocation = getDetectivesLocation(board);
		AtomicInteger count = new AtomicInteger();

		for(int location : detectivesLocation){
			detectivesLocation.forEach(place -> {
				if (place == location) count.getAndIncrement();
			});
		}

		return count.intValue();
	}


	private List<Integer> adjacentNodes(Board board, Move move){
		Board.GameState gameState = (Board.GameState) board;
		Board updated = updatedBoard(board, move);
		List<Move> adjacentMoves = updated.getAvailableMoves().asList();
		Set<Integer> adjacentNodes = new HashSet<>();
		Set<Integer> farNodes = new HashSet<>();

		for(Move adjacent : adjacentMoves){
			adjacentNodes.add(updateLocation(adjacent));
		}

		for(Integer node : adjacentNodes){
			farNodes.addAll(gameState.getSetup().graph.adjacentNodes(node));
		}
		return new ArrayList<>(farNodes);
	}

	//a method that return an updated board after particular move
	private Board updatedBoard(Board board, Move move) {
		return ((Board.GameState) board).advance(move);
	}


	//TODO minimax
	//MINIMAX//
	public Integer minimax(Board board, Move move, int depth, int alpha, int beta) {
		Board.GameState gameState = (Board.GameState) board;
		List<Move> moves = board.getAvailableMoves().asList();
//		System.out.println(move);
		if (depth == 0 || !board.getWinner().isEmpty()) {
//			System.out.println("depth 0 여기 오나요 제발");
//			System.out.println("MOVE: " + move + " SCORE: " + calculateMoveDistance(board, move));
//			return calculateMoveDistance(board, move);
			System.out.println("MOVE: " + move + " SCORE: " + calculateDistance(getDetectivesLocation(board), updateLocation(move), gameState.getSetup().graph));
			return evaluate(board, move);
		}
		if (moves.get(0).commencedBy() == Piece.MrX.MRX) {
			int maxEval = Integer.MIN_VALUE;
			for (Move child : moves) {
				Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 T 보드업데이트됨");
				int eval = minimax(updated, child, depth - 1, alpha, beta);
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
			for (Move child : moves) {
				if (moves.get(0).commencedBy() == child.commencedBy()){
					Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 F 보드업데이트됨");
					int eval = minimax(updated, move, depth - 1, alpha, beta);
					minEval = Math.min(minEval, eval);
					beta = Math.min(minEval, beta);
					if (beta <= alpha) {
						break;
					}
				}
			}
			return minEval;
		}
	}

	//a method that returns the score based on the type of the move
	private Integer filterDouble(Board board, Move move) {
		return move.accept(new Move.Visitor<>() {
			final Board.GameState gameState = (Board.GameState) board;

			@Override
			public Integer visit(Move.SingleMove move) {
				return 1;
			}

			//if it is double move, set it to the default value 0 whenever there are any possible singleMoves
			@Override
			public Integer visit(Move.DoubleMove move) {
				return 0;
			}
		});
	}

	private Integer evaluate(Board board, Move move) {
		int score = 0;
		Board.GameState gameState = (Board.GameState) board;

		if (!board.getWinner().isEmpty()) {
			if (board.getWinner().contains(Piece.MrX.MRX)) {
				score = +500;
			}
			else {
				score = -500;
			}
		}
		else {
			score = calculateDistance(getDetectivesLocation(board), updateLocation(move), gameState.getSetup().graph);
		}
		return score;
	}


	//a function that checks whether this move is safe or not
	//it returns a list of the nodes that are not adjacent to detectives' locations
	private List<Move> checkAdjacent(Board board, List<Move> bestMoves) {
		List<Move> possible = new ArrayList<>();
		for (Move move : bestMoves) {
			Set<Integer> occupation = detectiveAdjacent(move, board);
			//if there are no detectives around add the move to the list
			if (occupation.add(updateLocation(move))) {
				possible.add(move);
			}
		}
		return possible;
	}


	//HELPER METHODS STARTS HERE//
	//a helper method that returns all adjacent nodes from detectives' current location
	private Set<Integer> detectiveAdjacent(Move move, Board board) {
		Board newBoard = updatedBoard(board, move);
		Set<Integer> availableLocation = new HashSet<>();
		//places where detectives can go
		for (Move move2 : newBoard.getAvailableMoves()) {
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
	public static List<ScotlandYard.Ticket> updateTicket(Move move) {
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


	//a helper method that weights transportations
	//TODO transportationCost
	private Integer transportationCost(Integer source, Integer destination, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
		int ticketVal = 0;
		//returning different ticket values by transportation respectively
		for (ScotlandYard.Transport t : graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
			switch(t.requiredTicket()){
				case TAXI -> ticketVal += 2;
				case BUS -> ticketVal += 4;
				case UNDERGROUND -> ticketVal += 8;
			}
		}
		return ticketVal;
	}


	private Integer transportationCost(Board board, List<ScotlandYard.Ticket> tickets) {
		List<LogEntry> mrXLog = board.getMrXTravelLog();
		int ticketVal = 0;
		//returning different ticket values by transportation respectively
		for(ScotlandYard.Ticket ticket : tickets){
			switch (ticket){
				case TAXI -> ticketVal += 2;
				case BUS -> ticketVal += 4;
				case UNDERGROUND -> ticketVal += 8;
				case SECRET -> {
					if (mrXLog.size() > 2 && ticket.equals(ScotlandYard.Ticket.SECRET)){
						//if mrX's log size is larger than 0 and is after reveal
						if(board.getSetup().moves.get(mrXLog.size() - 1)) {
//							System.out.println("SIZE: " + mrXLog.size());
							ticketVal += Integer.MAX_VALUE;}
					}
				}
			}
		}
		return ticketVal;
	}
//	private Integer checkDouble(Board board, List<ScotlandYard.Ticket> tickets) {
//		int eval = 0;
//		//returning different ticket values by transportation respectively
//		for(ScotlandYard.Ticket ticket : tickets){
//			if (Objects.requireNonNull(ticket) == ScotlandYard.Ticket.DOUBLE) {
//				eval = 0;
//			}
//		}
//		return eval;
//	}
	//HELPER METHODS ENDS HERE//

	//Scoring method, returns the distance from the detectives' location to mrX's destination
	//TODO Dijkstra
	private Integer calculateDistance(List<Integer> detectivesLocation, Integer mrXLocation, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
//		List<Integer> detectivesLocation = getDetectivesLocation(board);
//		int mrXLocation = updateLocation(mrXMove);
		System.out.println("DETECTIVES' LOC:" + detectivesLocation);
		int size;
//		System.out.println("MRX's LOC: " + mrXLocation);
		List<List<Integer>> shortPath = new ArrayList<>();
		List<List<Integer>> allPath = new ArrayList<>();
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
					Integer weight = transportationCost(currentNode, mrXLocation, graph);
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
			if (path.size() < 4) {
				shortPath.add(path);
			}
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