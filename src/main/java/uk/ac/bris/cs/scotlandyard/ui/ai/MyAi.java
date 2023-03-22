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

	@Nonnull @Override public String name() { return "SIGMA"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		ImmutableSet<Piece> allPlayers = board.getPlayers();
		var moves = board.getAvailableMoves().asList();
		var detectiveList = getDetectives(board);
		var locations = detectiveLocation(board, detectiveList);

		Board.GameState currentState = (Board.GameState) board;
		Set<Board.GameState> nextState = nextGameState(currentState, board);

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

	private Set<Board.GameState> nextGameState(Board.GameState state, Board board){
		Set<Move> mrXMoves = new HashSet<>(board.getAvailableMoves());
		Set<Board.GameState> updatedStates = new HashSet<>();
		for(Move eachMove : mrXMoves){
			updatedStates.add(state.advance(eachMove));
		}
		System.out.println("updated states: " + updatedStates);
		return updatedStates;
	}


	//get available moves for detectives
	private Set<Move> getDetectiveMoves(Board.GameState state, Board board, ImmutableSet<Piece> players){
		Set<Board.GameState> possibleState = new HashSet<>();
		Set<Move> mrXMoves = new HashSet<>(board.getAvailableMoves());
		for(Move currentMove : mrXMoves){
			possibleState.add(state.advance(currentMove));
		}
		for(Board.GameState state1 : possibleState){

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
