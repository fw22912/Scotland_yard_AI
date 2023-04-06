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
import org.checkerframework.checker.units.qual.A;
import org.w3c.dom.Node;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "PLZ"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}


	private Move bestMove(Board board, Player player, int depth){
		return null;
	}

	private Board updatedBoard(Board board, Move move){
		return ((Board.GameState) board).advance(move);
	}

	//MiniMax
	private Integer miniMax(Board board, int depth, int alpha, int beta, Boolean checkMrX){
		if(depth == 0 || !board.getWinner().isEmpty()){
//         getScore()
//         return scores from a given board
		}
		if(checkMrX){
			int maxEval = (int)Double.NEGATIVE_INFINITY;
			for(Move move : board.getAvailableMoves()){
				Board newBoard = updatedBoard(board, move);
				int eval = miniMax(newBoard, depth - 1, alpha, beta, false);
				//can I compare double and integer!!!!
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
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
				beta = Math.min(beta, eval);
				if(beta <= alpha) break;
			}
			return minEval;
		}
	}

	private Move mrXBestMove(Board board, int depth){
		int maxEval = (int)Double.NEGATIVE_INFINITY;
		int alpha = (int)Double.NEGATIVE_INFINITY;
		int beta = (int)Double.POSITIVE_INFINITY;
		List<Move> bestMoves =  new ArrayList<>();

		for(Move move: board.getAvailableMoves()){
			Board updated = updatedBoard(board, move);
			int eval = miniMax(board, depth -1, alpha, beta, false);
			if(maxEval < eval){
				maxEval = eval;
				bestMoves.clear();
				bestMoves.add(move);
			}
			else if(maxEval == eval){
				bestMoves.add(move);
			}
		}
		return null;
	}

	private static List<Integer> returnLocation(Board board){
		List<Integer> locations = new ArrayList<>();
		for(Piece piece : board.getPlayers()){
			if(!piece.isMrX()){
				locations.add(board.getDetectiveLocation((Piece.Detective) piece).get());
			}
		}
		return locations;
	}


	private Integer calculateDistance(Board board, Move move){

		return null;
	}
}
