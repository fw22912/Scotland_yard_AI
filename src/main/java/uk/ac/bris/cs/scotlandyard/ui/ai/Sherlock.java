package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Sherlock implements Ai {

    @Nonnull
    @Override
    public String name() {
        return "Sherlock";
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        int minEval = Integer.MAX_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 3;
        List<Move> availableMoves = board.getAvailableMoves().asList();
        List<Move> optimalMoves = new ArrayList<>();

        //iterate through all the available moves and get a move with the highest minimax score
        for (Move move : availableMoves) {
            Board updated = updatedBoard(board, move);
            //do minimax with updatedBoard after designated move
            getMrXLocation(board);
            int eval = minimax(updated, move, depth - 1, alpha, beta, board, move, timeoutPair.left());
            if (minEval > eval) {
                minEval = eval;
                optimalMoves.clear();
                optimalMoves.add(move);
            } else if (minEval == eval) {
                optimalMoves.add(move);
            }
        }

        return returnBestMove(board, optimalMoves, board);
    }



    private Optional<Integer> getMrXLocation(Board board){
        List<LogEntry> log = board.getMrXTravelLog();
        LogEntry currLog = log.get(log.size() - 1);
        int mrXLocation = 0;
        if (log.contains(Piece.MrX.MRX)) {
            if (currLog.location().isPresent()) throw new NoSuchElementException();
            else mrXLocation = currLog.location().get();
        }
        return Optional.of(mrXLocation);
    }


    private boolean checkMrX(Board board){
        List<Move> allMoves = board.getAvailableMoves().asList();
        boolean check = false;
        for(Move move : allMoves){
            if(move.commencedBy() != Piece.MrX.MRX){
                check = true;
                break;
            }
        }
        return check;
    }

    private Move returnBestMove(Board board, List<Move> optimalMoves, Board originalBoard){
//		Random ran = new Random();
        List<Move> finalMoves = new ArrayList<>();
        int maxScore = Integer.MIN_VALUE;
        for (Move move : optimalMoves) {
            int score2 = scoreMove(board, move, getMrXLocation(board).get(), move.commencedBy());
            if (score2 > maxScore) {
                finalMoves.clear();
                finalMoves.add(move);
                maxScore = score2;
            } else if (score2 == maxScore) {
                finalMoves.add(move);
            }
        }
        return finalMoves.get(0);
    }



    //a method that return an updated board after particular move
    private Board updatedBoard(Board board, Move move) {return ((Board.GameState) board).advance(move);}


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


    //returns the amount of current tickets
    private Integer ticketsLeft(Board board, Piece piece, ScotlandYard.Ticket currTicket){
        Board.TicketBoard ticketBoard = board.getPlayerTickets(piece).get();
        return ticketBoard.getCount(currTicket);
    }


    //a helper method that weights transportations
    //TODO transportationCost
    private Integer transportationCost(Integer source, Integer destination, Board board, Piece piece) {
        Board.GameState gameState = (Board.GameState) board;
        int ticketVal = 0;
        //returning different ticket values by transportation respectively
        for (ScotlandYard.Transport t : gameState.getSetup().graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
            switch(t.requiredTicket()){
                case TAXI -> ticketVal += 20 * ticketsLeft(board, piece, ScotlandYard.Ticket.TAXI);
                case BUS -> ticketVal += 40 * ticketsLeft(board, piece, ScotlandYard.Ticket.BUS);
                case UNDERGROUND -> ticketVal += 80 * ticketsLeft(board, piece, ScotlandYard.Ticket.UNDERGROUND);
            }
        }
        return ticketVal;
    }


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


    private Integer scoreMove(Board board, Move move, Integer mrXLocation, Piece piece) {
        return transportationCost(move.source(), mrXLocation, board, piece);
    }

    private static List<Integer> getDetectivesLocation(Board board) {
        List<Integer> locations = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            if (!piece.isMrX()) {
                locations.add(board.getDetectiveLocation((Piece.Detective) piece).get());
            }
        }
        return locations;
    }

    private Integer evaluate(Board board, Move move, Board originalBoard) {
        //elements
        Board.GameState gameState = (Board.GameState) board;
        int score;

        if (!board.getWinner().isEmpty()) {    //if winner determined
            if (!board.getWinner().contains(Piece.MrX.MRX)) {    //and if MrX, return 1000
                score = 1000;
            }
            else {   //detectives, return -1000
                score = -1000;
            }
        }
        else {   //otherwise calculate the distance with Dijkstra
            score = Dijkstra.calculateDistance(getDetectivesLocation(originalBoard), getMrXLocation(board).get(), gameState.getSetup().graph);
        }
        return score;
    }


    public Integer minimax(Board board, Move move, int depth, int alpha, int beta, Board originalBoard, Move originalMove, Long time) {
        List<Move> moves = board.getAvailableMoves().asList();
        long startTime = System.nanoTime();

        //evaluate if the game has ended
        if (depth == 0
                || !board.getWinner().isEmpty()
                || (startTime - System.nanoTime() > (time - 50))) {     //or if it has not been done in a designated time
            return evaluate(board, originalMove, originalBoard);
        }
        if (moves.get(0).commencedBy() == Piece.MrX.MRX) {
            int minEval = Integer.MAX_VALUE;
            for (Move child : moves) {
                Board updated = updatedBoard(board, child);
                int eval = minimax(updated, child, depth - 1, alpha, beta, originalBoard, originalMove, time);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, minEval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
        else {
            boolean detectiveRemaining = checkOnlyOneInRemaining(board);  //check the remaining of the detectives
            int maxEval = Integer.MIN_VALUE;
            if (detectiveRemaining) {
                for (Move child : moves) {
                    if (moves.get(0).commencedBy() == child.commencedBy()){
                        Board updated = updatedBoard(board, child);
                        int eval = minimax(updated, move, depth - 1, alpha, beta, originalBoard, originalMove, time);
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(maxEval, alpha);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            else {
                for (Move child : moves) {
                    if (moves.get(0).commencedBy() == child.commencedBy()) {
                        Board updated = updatedBoard(board, child);
                        int eval = minimax(updated, move, depth, alpha, beta, originalBoard, originalMove, time);
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(maxEval, alpha);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return maxEval;
        }
    }
}