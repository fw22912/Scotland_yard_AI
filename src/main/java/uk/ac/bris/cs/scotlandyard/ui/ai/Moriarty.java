package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class Moriarty implements Ai {

    @Nonnull
    @Override
    public String name() {
        return "Moriarty";
    }


    @Nonnull
    @Override
    public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        int maxEval = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 3;
        List<Move> availableMoves = board.getAvailableMoves().asList();
        List<Move> optimalMoves = new ArrayList<>();
//		List<Move> availableMoves = new ArrayList<>();
        Move bestMove = null;

        //iterate through all the available moves and get a move with the highest minimax score
        for (Move move : availableMoves) {
            Board updated = updatedBoard(board, move);
            //do minimax with updatedBoard after designated move
            int eval = minimax(timeoutPair.left(), updated, move, depth - 1, alpha, beta, move, board);
            //if new best high value, clear the list and add the move
            if (maxEval < eval) {
                maxEval = eval;
                optimalMoves.clear();
                optimalMoves.add(move);
            } else if (maxEval == eval) {  //if same, add the move
                optimalMoves.add(move);
            }
        }
        return returnBestMove(board, optimalMoves);
    }



    private Move returnBestMove(Board board, List<Move> optimalMoves){
        //elements for storing the highest move after evaluation
        Move bestMove;
        Board.GameState gameState = (Board.GameState) board;
        List<Move> highestMoves = new ArrayList<>();
        int maxScore = Integer.MIN_VALUE;

        //if only one move, pick it
        if(optimalMoves.size() == 1) {
            bestMove = optimalMoves.get(0);
        }
        else{
            for (Move optimalMove : optimalMoves) {
                //new score by adding up transportation cost, number of adjacent nodes
                // and subtracting the num of nearby detectives
                int score2 = transportationCost(board, updateTicket(optimalMove))
                        + gameState.getSetup().graph.adjacentNodes(updateLocation(optimalMove)).size()
                        - (detectivesNearby(board, optimalMove) * 3);
                //find the highest score and add it to the list
                if (score2 > maxScore) {
                    highestMoves.clear();
                    highestMoves.add(optimalMove);
                    maxScore = score2;
                }
                else if (score2 == maxScore) {
                    highestMoves.add(optimalMove);
                }
            }

            //new elements for checking if there are no adjacent detectives
            //in the neighbouring nodes after MrX has moved
            //run checkAdjacent which only returns the moves that are safe
            List<Move> noAdjacentMoves = checkAdjacent(board, highestMoves);
            List<Move> alternativeMoves = new ArrayList<>();
            List<Move> finalMoves = new ArrayList<>();

            //if no such move, filter the moves
            //if single and double both available, choose from only single moves
            if (noAdjacentMoves.isEmpty()) {
                alternativeMoves.addAll(filterSingleDouble(board, highestMoves));
                bestMove = alternativeMoves.get(0);
            }
            else {
                //if only one move, return
                if (noAdjacentMoves.size() == 1) {
                    bestMove = noAdjacentMoves.get(0);
                }
                //if not filter moves and add, choose the first one
                else {
                    finalMoves.addAll(filterSingleDouble(board, noAdjacentMoves));
                    bestMove = finalMoves.get(0);
                }
            }
        }
        return bestMove;
    }


    //returns filtered Single or Double
    //if single exists, return only single and both otherwise
    private List<Move> filterSingleDouble(Board board, List<Move> scoredMoves){
        int totalVal = valueListMoves(board, scoredMoves);
        if(totalVal != 0) return getOnlySingle(board, scoredMoves);  //if no double, return only single
        else return scoredMoves;  //otherwise return all moves
    }


    //filters and returns singleMove only
    //returns the number of single moves
    private Integer valueListMoves(Board board, List<Move> scoredMoves){
        int totalVal = 0;
        for(Move move : scoredMoves){
            totalVal += filterDouble(board, move);
        }
        return totalVal;
    }



    //filters only single moves and return them
    private List<Move> getOnlySingle(Board board, List<Move> scoredMoves){
        List<Move> onlySingle = new ArrayList<>();
        for(Move move : scoredMoves) {
            if (filterDouble(board, move) == 1) {
                onlySingle.add(move);
            }
        }
        return onlySingle;
    }



    //returns the destination of the move
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



    //getting adjacent nodes from the board after the move and the adjacent nodes from there
    //helper method for detectivesNearby
    private List<Integer> adjacentNodes(Board board, Move move){
        Board.GameState gameState = (Board.GameState) board;
        Board updated = updatedBoard(board, move);
        List<Move> adjacentMoves = updated.getAvailableMoves().asList();  //all available moves
        Set<Integer> adjacentNodes = new HashSet<>();
        Set<Integer> farNodes = new HashSet<>();

        //add all the locations of the available moves
        for(Move adjacent : adjacentMoves){
            adjacentNodes.add(updateLocation(adjacent));
        }

        //add all the adjacent nodes from adjacentNodes
        for(Integer node : adjacentNodes){
            farNodes.addAll(gameState.getSetup().graph.adjacentNodes(node));
        }

        //returning double adjacent nodes of the move
        return new ArrayList<>(farNodes);
    }



    //a helper method that gathers all detectives' current locations
    private static List<Integer> getDetectivesLocation(Board board) {
        List<Integer> locations = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            if (!piece.isMrX()) {
                locations.add(board.getDetectiveLocation((Piece.Detective) piece).get());
            }
        }
        return locations;
    }



    //returns the number of the detectives that are located in the adjacent X 2 nodes
    private Integer detectivesNearby(Board board, Move move){
        List<Integer> listAdjacent = adjacentNodes(board, move);  //list of double adjacent nodes
        List<Integer> detectivesLocation = getDetectivesLocation(board);    //getting the detectives' location
        AtomicInteger count = new AtomicInteger();

        //increment count by 1 if there are any detectives in the adjacent nodes
        for(int location : detectivesLocation){
            listAdjacent.forEach(place -> {
                if (place == location) count.getAndIncrement();
            });
        }
        //returns the number of detectives located nearby
        return count.intValue();
    }




    //a method that return an updated board after particular move
    private Board updatedBoard(Board board, Move move) {
        return ((Board.GameState) board).advance(move);
    }



    //a helper method that returns a boolean value of
    // whether there is only one detective left in the remaining
    private boolean checkOnlyOneInRemaining(Board board) {
        //elements
        ArrayList<Move> moves = new ArrayList<>(board.getAvailableMoves().asList()); //get every available moves
        Piece checkFirst = moves.get(0).commencedBy();   //getting the piece that commenced first move
        boolean check = true;

        //if the move is not empty iterate through the moves
        if(!moves.isEmpty()){
            for (Move move : moves) {
                if (move.commencedBy() != checkFirst) {  //if the piece of the first move is different from the current move's piece, check == false
                    check = false;
                    break;
                }
            }
        }
        return check;
    }

    //HELPER METHODS END HERE//


    //MINIMAX//
    public Integer minimax(Long time, Board board, Move move, int depth, int alpha, int beta, Move originalMove, Board originalBoard) {
        //needed elements
        List<Move> moves = board.getAvailableMoves().asList();
        long startTime = System.nanoTime();
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;

        //evaluate if the game has ended
        if (depth == 0
                || !board.getWinner().isEmpty()
                || (totalTime > time)) {     //or if it has not been done in a designated time
            return evaluate(board, originalMove, originalBoard);
        }
        if (moves.get(0).commencedBy() == Piece.MrX.MRX) {   //when it is MrX's turn
            int maxEval = Integer.MIN_VALUE;
            for (Move child : moves) {   //iterate through moves
                Board updated = updatedBoard(board, child);
                int eval = minimax(time, updated, child, depth - 1, alpha, beta, originalMove, originalBoard);  //do minimax for the next detectives' move (subtracting depth by 1)
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, maxEval);
                if (beta <= alpha) {   //using alpha-beta pruning
                    break;
                }
            }
            return maxEval;
        }
        else {
            boolean detectiveRemaining = checkOnlyOneInRemaining(board);  //check the remaining of the detectives
            int minEval = Integer.MAX_VALUE;
            if (detectiveRemaining) {    //if last detective
                for (Move child : moves) {
                    if (moves.get(0).commencedBy() == child.commencedBy()){
                        Board updated = updatedBoard(board, child);
                        //pass down the minimax to MrX by subtracting depth by 1
                        int eval = minimax(time, updated, move, depth - 1, alpha, beta, originalMove, originalBoard);
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(minEval, beta);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            else { //if not the last detective
                for (Move child : moves) {
                    if (moves.get(0).commencedBy() == child.commencedBy()) { //choosing one detective piece and move
                        Board updated = updatedBoard(board, child);
                        //minimax, but not decreasing the depth until the board is updated by all detectives' move
                        int eval = minimax(time , updated, move, depth, alpha, beta, originalMove, originalBoard);
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(minEval, beta);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return minEval;
        }
    }


    //minimax helping function that returns the score
    //score == distance between current detectives' location to destination of MrX's move
    private Integer evaluate(Board board, Move move, Board originalBoard) {
        //elements
        Board.GameState gameState = (Board.GameState) board;
        int score;

        if (!board.getWinner().isEmpty()) {    //if winner determined
            if (board.getWinner().contains(Piece.MrX.MRX)) {    //and if MrX, return 1000
                score = 1000;
            }
            else {   //detectives, return -1000
                score = -1000;
            }
        }
        else {   //otherwise calculate the distance with Dijkstra
            score = Dijkstra.calculateDistance(getDetectivesLocation(originalBoard), updateLocation(move), gameState.getSetup().graph);
        }
        return score;
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




    //a helper method that weights transportations for MrX
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
                    if (mrXLog.size() != 0){
                        if(board.getSetup().moves.get(mrXLog.size() - 1)) {   //if mrX's log size is larger than 0 and is after reveal
                            ticketVal += 200;}
                    }
                }
            }
        }
        return ticketVal;
    }
}