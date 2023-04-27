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

public class MORIATY implements Ai {

	@Nonnull
	@Override
	public String name() {
		return "MORIATY";
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
		List<Move> availableMoves = board.getAvailableMoves().asList();
		System.out.println("AVAILABLE MOVES: " + availableMoves) ;
		List<Move> farMoves = farthestMove(board, availableMoves);
		System.out.println("size: " + farMoves.size());
		List<Move> optimalMoves = new ArrayList<>();
		System.out.println("FARMOVES: " + farMoves);
		Move bestMove = null;

		//iterate through all the available moves and get a move with the highest minimax score
		for (Move move : farMoves) {
			Board updated = updatedBoard(board, move);
			//do minimax with updatedBoard after designated move
			int eval = minimax(updated, move, depth - 1, alpha, beta, board, move);
			System.out.println("Move: " + move + "   EVAL: " + eval);
			if (maxEval < eval) {
				maxEval = eval;
				optimalMoves.clear();
				optimalMoves.add(move);
			} else if (maxEval == eval) {
				optimalMoves.add(move);
			}
		}
		System.out.println("OPTIMALMOVES: " + optimalMoves);
		//optimal Moves에서는 이미 dijkstra와 score2 << evaluate 된 move들만 들어있음

		return returnBestMove(board, optimalMoves, board);
	}



	private Integer scoreMove(Board board, Move move) {
		Board.GameState gameState = (Board.GameState) board;
		int transportCost = transportationCost(board, updateTicket(move));
		int adjacent = gameState.getSetup().graph.adjacentNodes(updateLocation(move)).size();
		int nearby = detectivesNearby(board, move);
		int score = transportCost + adjacent + nearby;
		return score;
	}


	private List<Move> farthestMove(Board board, List<Move> availableMoves){
		List<Integer> DLocation = getDetectivesLocation(board);
		Board.GameState gameState = (Board.GameState) board;
		List<Move> farMoves = new ArrayList<>();

		int maxDistance = 0;
		for(Move move : availableMoves){
			int eval = calculateDistance(DLocation, updateLocation(move), gameState.getSetup().graph);
			if(eval > maxDistance){
				maxDistance = eval;
				farMoves.clear();
				farMoves.add(move);
			}
			else if(eval == maxDistance){
				farMoves.add(move);
			}
		}
		return farMoves;
	}


	private Move returnBestMove(Board board, List<Move> optimalMoves, Board originalBoard){
//		Random ran = new Random();
		Move bestMove;
		List<Move> highestMoves = new ArrayList<>();
		List<Move> noAdjacentMoves = checkAdjacent(board, optimalMoves);
		List<Move> alternativeMoves = new ArrayList<>();
		List<Move> finalMoves = new ArrayList<>();
		if (noAdjacentMoves.isEmpty()) {
			alternativeMoves.addAll(filterSingleDouble(board, optimalMoves));
			bestMove = alternativeMoves.get(0);
		}
		else {
			//NEW LOOP
			if (noAdjacentMoves.size() == 1) {bestMove = noAdjacentMoves.get(0);}
			else {
				finalMoves.addAll(filterSingleDouble(board, noAdjacentMoves));
				bestMove = finalMoves.get(0);
				System.out.println("FINALMOVES: " + finalMoves);
			}
		}

		System.out.println("BESTMOVE: " + bestMove);
		return bestMove;
	}

	//추가!!
	private Set<Integer> returnAllDestinations(List<Move> highestMoves){
		Set<Integer> destination = new HashSet<>();
		for(Move move : highestMoves){
			destination.add(updateLocation(move));
		}
		return destination;
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
	private Integer detectivesNearby(Board board, Move move){
		List<Integer> listAdjacent = adjacentNodes(board, move);
		List<Integer> detectivesLocation = getDetectivesLocation(board);
		AtomicInteger count = new AtomicInteger();

		for(int location : detectivesLocation){
			listAdjacent.forEach(place -> {
				if (place == location) count.getAndIncrement();
			});
		}

		return count.intValue();
	}


	//getting adjacent nodes from the board after the move and the adjacent nodes from there
	private List<Integer> adjacentNodes(Board board, Move move){
		Board.GameState gameState = (Board.GameState) board;
//		Board updated = updatedBoard(board, move);
		List<Move> adjacentMoves = board.getAvailableMoves().asList();
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

	private boolean checkOnlyOneInRemaining(Board board) {
		ArrayList<Move> moves = new ArrayList<>(board.getAvailableMoves().asList());
		Piece checkFirst = moves.get(0).commencedBy();
		boolean check = true;
		if (!moves.isEmpty()){
			for (Move move : moves) {
				if (move.commencedBy() != checkFirst) {
					check = false;
					break;
				}
			}
		}
		return check;
	}


	//TODO minimax
	//MINIMAX//
	public Integer minimax(Board board, Move move, int depth, int alpha, int beta, Board originalBoard, Move originalMove) {
		Board.GameState gameState = (Board.GameState) board;
		List<Move> moves = board.getAvailableMoves().asList();
		boolean detectiveRemaining = checkOnlyOneInRemaining(board);
		int maxEval = Integer.MIN_VALUE;
		int minEval = Integer.MAX_VALUE;
//		System.out.println(move);
		if (depth == 0 || !board.getWinner().isEmpty()) {
			System.out.println("-----------------------------NEW MOVE------------------------");
			System.out.println("MOVE: " + move + " SCORE: " + evaluate(board, move, originalBoard));
			return evaluate(board, originalMove, originalBoard);
		}
		if (moves.get(0).commencedBy() == Piece.MrX.MRX) {
			System.out.println("MrX: " + moves);
			for (Move child : moves) {
//				System.out.println("MRX TURN CHILD: " + child);
				Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 T 보드업데이트됨");
				int eval = minimax(updated, child, depth - 1, alpha, beta, originalBoard, originalMove);
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, maxEval);
				if (beta <= alpha) {
					break;
				}
			}
			return maxEval;
		}
		else {
//			System.out.println("numOfDetectives: " + numOfDetectives);
			if (detectiveRemaining) {
				System.out.println("Detectives: " + moves);
				for (Move child : moves) {
//					System.out.println("DETECTIVES TURN CHILD BUT ONLY ONE REMAINING: " + child);
					if (moves.get(0).commencedBy() == child.commencedBy()){
						Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 F 보드업데이트됨");
						int eval = minimax(updated, move, depth - 1, alpha, beta, originalBoard, originalMove);
						minEval = Math.min(minEval, eval);
						beta = Math.min(minEval, beta);
						if (beta <= alpha) {
							break;
						}
					}
				}
				return minEval;
			}
			else {
//				int minEval = Integer.MAX_VALUE;
				System.out.println("Detectives: " + moves);
				for (Move child : moves) {
//					System.out.println("DETECTIVES TURN CHILD ELSE: " + child);
					if (moves.get(0).commencedBy() == child.commencedBy()) {
						Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 F 보드업데이트됨");
						int eval = minimax(updated, move, depth, alpha, beta, originalBoard, originalMove);
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
	}

	//a method that returns the score based on the type of the move
	private Integer filterDouble(Board board, Move move) {
		return move.accept(new Move.Visitor<>() {
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



	private Integer evaluate(Board board, Move move, Board originalBoard) {
		int score = 0;
		Board.GameState gameState = (Board.GameState) board;

		if (!board.getWinner().isEmpty()) {
			if (board.getWinner().contains(Piece.MrX.MRX)) {
				score = 1000;
			}
			else {
				score = -1000;
			}
		}
		else {
			score = scoreMove(originalBoard, move);
		}
		return score;
	}






	//a function that checks whether this move is safe or not
	//it returns a list of the nodes that are not adjacent to detectives' locations]//수정
	private boolean checkAdjacentBool(Board board, List<Move> bestMoves) {
		List<Move> possible = new ArrayList<>();
		for (Move move : bestMoves) {
			Set<Integer> occupation = detectiveAdjacent(move, board);
			//if there are no detectives around add the move to the list
			if (!occupation.add(updateLocation(move))) {
				return true;
			}
		}
		return false;
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


	//추가!!!
	private Integer adjacentScore(Board board, List<Move> bestMoves){
		boolean check = checkAdjacentBool(board, bestMoves);
		if(check) return 60;
		else return 0;
	}

//================================================================CHANGED TILL HERE===================================================================


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
					if (mrXLog.size() > 1){
						//if mrX's log size is larger than 0 and is after reveal
						if(board.getSetup().moves.get(mrXLog.size() - 1)) {
//							System.out.println("SIZE: " + mrXLog.size());
							System.out.println("Came here to give 200 points!");
							System.out.println("LOG SIZE: " + (mrXLog.size() - 1));
							ticketVal += 200;}
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