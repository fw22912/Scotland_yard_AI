package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
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


	//returns a list of detectives' piece
	@Nonnull private List<Piece> getDetectives(Board board){
		List<Piece> allDetectives = new ArrayList<>();
		var allPlayers = board.getPlayers();
		for(Piece piece : allPlayers){
			if(piece.isDetective()){
				allDetectives.add(piece);
			}
		}
		System.out.println("Get detectives: " +  allDetectives);
		return allDetectives;
	}

	//returning a new game state with one move ahead
	private Board updatedBoard(Board board, Move move){
		return ((Board.GameState) board).advance(move);
	}

	//get Scores
	private Integer makeScore(Board board){
		return null;
	}

	//returns mrX's location
	private Set<Integer> getMrXLocation(Board board, List<Piece> players){
		Set<Integer> mrXLocation = new HashSet<>();
		for(Piece piece : players){
			if(piece.isMrX()){
				Set<Move> getMrXMoves = board.getAvailableMoves();
				for(Move move : getMrXMoves){
					mrXLocation.add(move.source());
					break;
				}
			}
		}
		System.out.println("getMrXLocation: " + mrXLocation);
		return mrXLocation;
	}


	//returns a list of detectives' location
	private Set<Integer> detectiveLocation(Board board, List<Piece> players){
		Set<Integer> allLocation = new HashSet<>();
		for(Piece piece : players){
			if(piece.isDetective()){
				var location = board.getDetectiveLocation((Piece.Detective) piece).get();
				allLocation.add(location);
			}
		}
		System.out.println("detectiveLocation: " + allLocation);
		return allLocation;
	}

	//get available moves for detectives
	private Set<Move> getDetectiveMoves(Board board, ImmutableSet<Piece> players){
		Set<Move> detectiveMoves = new HashSet<>();
		Set<Move> allMoves = board.getAvailableMoves();
		for (Move move : allMoves) {
			if (move.commencedBy() != Piece.MrX.MRX) {detectiveMoves.add(move);}
		}
		System.out.println("getDetectiveMoves: " + detectiveMoves);
		return detectiveMoves;
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
			double maxEval = Double.NEGATIVE_INFINITY;
			for(Move move : board.getAvailableMoves()){
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				maxEval = ma

			}
		}
		return null;
	}

	//assign score to each location
	//should I use miniMax here?
	//should use Dijkstra as well

	private Integer getScore(Board.GameState state, Board board, Set<Integer> detectiveLocation, Set<Integer> mrXLocation, Set<Move> detectiveMoves){

		return null;
	}
}
