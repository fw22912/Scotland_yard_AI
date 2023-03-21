package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "SIGMA"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
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
		System.out.println(allDetectives);
		return allDetectives;
	}

	//returns a list of detectives' location
	private List<Integer> detectiveLocation(Board board, List<Piece> detectives){
		List<Integer> allLocation = new ArrayList<>();
		for(Piece piece : detectives){
			var location = board.getDetectiveLocation((Piece.Detective) piece).get();
			allLocation.add(location);
		}
		return allLocation;
	}



	private Integer getScore(Board.GameState state, Board board, List<Integer> detectiveLocation, Move move){

		return null;
	}

}
