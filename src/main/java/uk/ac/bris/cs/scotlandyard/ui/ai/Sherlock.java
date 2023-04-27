package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
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
        Board.GameState gameState = (Board.GameState) board;
        int maxEval = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 3;
        List<Move> availableMoves = board.getAvailableMoves().asList();
        List<Move> optimalMoves = new ArrayList<>();
        Move bestMove = null;

        //iterate through all the available moves and get a move with the highest minimax score
        for (Move move : availableMoves) {
            Board updated = updatedBoard(board, move);
            //do minimax with updatedBoard after designated move
            int eval = minimax(updated, move, depth - 1, alpha, beta, board, move);
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


    //a function that checks whether this move is safe or not
    //it returns a list of the nodes that are not adjacent to detectives' locations
    private List<Move> checkAdjacent(Board board, List<Move> bestMoves) {
        List<Move> possible = new ArrayList<>();
        for (Move move : bestMoves) {
            Set<Integer> occupation = detectiveAdjacent(move, board);
            //if there are no detectives around add the move to the list
            if (!occupation.add(updateLocation(move))) {
                possible.add(move);
            }
        }
        return possible;
    }


    private Move returnBestMove(Board board, List<Move> optimalMoves, Board originalBoard){
//		Random ran = new Random();
        Move bestMove;
        List<Move> highestMoves = new ArrayList<>();
        List<Move> noAdjacentMoves = checkAdjacent(board, optimalMoves);
        List<Move> alternativeMoves = new ArrayList<>();
        List<Move> finalMoves = new ArrayList<>();
        if (noAdjacentMoves.isEmpty()) {
            bestMove = board.getAvailableMoves().asList().get(0);
        }
        else {
            //NEW LOOP
            if (noAdjacentMoves.size() == 1) {bestMove = noAdjacentMoves.get(0);}
            else {
                bestMove = noAdjacentMoves.get(0);
                System.out.println("FINALMOVES: " + finalMoves);
            }
        }

        System.out.println("BESTMOVE: " + bestMove);
        return bestMove;
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
        List<ScotlandYard.Ticket> tickets = new ArrayList<>();
        int i = 0;
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
                case TAXI -> ticketVal += 20 / ticketsLeft(board, piece, ScotlandYard.Ticket.TAXI);
                case BUS -> ticketVal += 40 / ticketsLeft(board, piece, ScotlandYard.Ticket.BUS);
                case UNDERGROUND -> ticketVal += 80 / ticketsLeft(board, piece, ScotlandYard.Ticket.UNDERGROUND);
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


    private Integer scoreMove(Board board, Move move, Integer mrXLocation, Piece piece) {
        Board.GameState gameState = (Board.GameState) board;
        int transportCost = transportationCost(move.source(), mrXLocation, board, piece);
        int adjacent = gameState.getSetup().graph.adjacentNodes(updateLocation(move)).size();
        int score = transportCost + adjacent;
        return score;
    }



    private Integer evaluate(Board board, Move move, Board originalBoard) {
        int score = 0;
        Board.GameState gameState = (Board.GameState) board;

        if (!board.getWinner().isEmpty()) {
            if (board.getWinner().contains(Piece.MrX.MRX)) {
                score = -1000;
            }
            else {
                score = 1000;
            }
        }
        else {
            score = scoreMove(originalBoard, move, move.source(), board.getAvailableMoves().asList().get(0).commencedBy());
        }
        return score;
    }


    public Integer minimax(Board board, Move move, int depth, int alpha, int beta, Board originalBoard, Move originalMove) {
        Board.GameState gameState = (Board.GameState) board;
        List<Move> moves = board.getAvailableMoves().asList();
        boolean detectiveRemaining = checkOnlyOneInRemaining(board);
//		System.out.println(move);
        if (depth == 0 || !board.getWinner().isEmpty()) {
            System.out.println("-----------------------------NEW MOVE------------------------");
            return evaluate(board, originalMove, originalBoard);
        }
        if (moves.get(0).commencedBy() == Piece.MrX.MRX) {
            int minEval = Integer.MIN_VALUE;
            System.out.println("MrX: " + moves);
            for (Move child : moves) {
//				System.out.println("MRX TURN CHILD: " + child);
                Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 T 보드업데이트됨");
                int eval = minimax(updated, child, depth - 1, alpha, beta, originalBoard, originalMove);
                minEval = Math.min(minEval, eval);
                alpha = Math.min(alpha, minEval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
        else {
//			System.out.println("numOfDetectives: " + numOfDetectives);
            int maxEval = Integer.MAX_VALUE;
            System.out.println("Detectives: " + moves);
            if (detectiveRemaining) {
                for (Move child : moves) {
//					System.out.println("DETECTIVES TURN CHILD BUT ONLY ONE REMAINING: " + child);
                    if (moves.get(0).commencedBy() == child.commencedBy()){
                        Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 F 보드업데이트됨");
                        int eval = minimax(updated, move, depth - 1, alpha, beta, originalBoard, originalMove);
                        maxEval = Math.max(maxEval, eval);
                        beta = Math.max(maxEval, beta);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            else {
                for (Move child : moves) {
//					System.out.println("DETECTIVES TURN CHILD ELSE: " + child);
                    if (moves.get(0).commencedBy() == child.commencedBy()) {
                        Board updated = updatedBoard(board, child);
//				System.out.println("체크미스터엑스 F 보드업데이트됨");
                        int eval = minimax(updated, move, depth, alpha, beta, originalBoard, originalMove);
                        maxEval = Math.max(maxEval, eval);
                        beta = Math.max(maxEval, beta);
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