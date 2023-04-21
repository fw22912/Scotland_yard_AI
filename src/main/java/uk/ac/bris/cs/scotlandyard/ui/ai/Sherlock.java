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

public class Sherlock implements Ai {

    @Nonnull
    @Override
    public String name() {
        return "Sherlock";
    }

    @Nonnull
    @Override
    public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        Board.GameState gameState = (Board.GameState) board;
        return mrXBestMove(gameState.getSetup().graph, board, 7);
    }

    //a method that return an updated board after particular move
    private Board updatedBoard(Board board, Move move) {
        return ((Board.GameState) board).advance(move);
    }

    private Piece getCurrentPlayer(Board board, Move move){
        for(Piece piece : board.getPlayers()){
            if(move.commencedBy() == piece) return piece;
        }
        return null;
    }

    //MINIMAX//
    public Integer minimax(Board board, Move move, int depth, int alpha, int beta, boolean checkDetective) {
        if (depth == 0 || !board.getWinner().isEmpty()) {
            return updateLocation(move);
        }
        if (checkDetective) {
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

    //a method that returns the score based on the type of the move
    private Integer calculateMoveDistance(Board board, Move move){
        return move.accept(new Move.Visitor<>() {
            final Board.GameState gameState = (Board.GameState) board;
            @Override
            public Integer visit(Move.SingleMove move) {
                return calculateDistance(board, move, gameState.getSetup().graph);
            }
            //if it is double move, set it to the default value 0 whenever there are any possible singleMoves
            @Override
            public Integer visit(Move.DoubleMove move) {
                return 0;
            }
        });
    }


    //chooses the best move for MrX
    private List<Move> getOptimalMoves(Board board, int depth) {
        int minEval = Integer.MAX_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        List<Move> optimalMoves = new ArrayList<>();
        //iterate through all the available moves and get a move with the highest minimax score
        for (Move move : board.getAvailableMoves()) {
            Board updated = updatedBoard(board, move);
            //do minimax with updatedBoard after designated move
            int eval = minimax(updated, move, depth, alpha, beta, true);
            if (minEval > eval) {
                minEval = eval;
                optimalMoves.clear();
                optimalMoves.add(move);
            }
            else if (minEval == eval) {
                optimalMoves.add(move);
            }
        }
        return optimalMoves;
    }


    //return the best move
    private Move mrXBestMove(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, Board board, int depth) {
        Move bestMove;
        int score = 0;
        Random ran = new Random();
        List<Move> optimalMoves = getOptimalMoves(board, depth);
        List<Move> noAdjacent = checkAdjacent(board, optimalMoves);
        List<Move> highestScore = new ArrayList<>();
        List<Move> finalMoves = new ArrayList<>();
        boolean checkDoubleMove = false;
        //if mrX is cornered by detectives, choose any move
        if(noAdjacent.isEmpty()){
            int randomIndex = ran.nextInt(optimalMoves.size());
            bestMove = optimalMoves.get(randomIndex);
        }
        else{
            for (Move move : noAdjacent) {
                int thisScore = calculateMoveDistance(board, move);
                //if it's new highest score
                if(thisScore > score){
                    score = thisScore;
                    highestScore.clear();
                    highestScore.add(move);
                }
                //if the score is equal to the current highest score
                else if(thisScore == score){
                    highestScore.add(move);
                }
            }
            //check if there are only double moves in highestScore
            if(calculateMoveDistance(board, highestScore.get(0)) == 0){
                checkDoubleMove = true;
            }
            int score2 = 0;
			/*If there are more than one possible moves, randomly choose among those moves
			  and contains only Single moves*/
            if(highestScore.size() > 1 && !checkDoubleMove){
                for(Move move : highestScore) {
                    int thisScore2 = transportationCost(board, updateTicket(move)) + graph.adjacentNodes(updateLocation(move)).size();
                    //if any score is higher than score2 clear finalMoves and add the new move
                    if (thisScore2 > score2){
                        score2 = thisScore2;
                        finalMoves.clear();
                        finalMoves.add(move);
                    }
                    //if same score, add it to the finalMoves
                    else if(thisScore2 == score2){
                        finalMoves.add(move);
                    }
                }
                int randomIndex = ran.nextInt(finalMoves.size());
                bestMove = finalMoves.get(randomIndex);
            }
            //if there are no other moves than double moves, return the farthest one from the detectives
            else if(checkDoubleMove){
                int score3 = 0;
                //get the move with the highest score
                for(Move move: highestScore){
                    int doubleScore = calculateDistance(board, move, graph);
                    if(score3 < doubleScore){
                        score3 = doubleScore;
                        finalMoves.clear();
                        finalMoves.add(move);
                    }
                    else if(score3 == doubleScore){
                        finalMoves.add(move);
                    }
                }
                int randomIndex = ran.nextInt(finalMoves.size());
                bestMove = finalMoves.get(randomIndex);
            }
            else bestMove = highestScore.get(0);
        }
        return bestMove;
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


    //HELPER METHODS STARTS HERE//
    //a helper method that returns all adjacent nodes from detectives' current location
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


    //a helper method that weights transportations
    private Integer transportationCost(Board board, List<ScotlandYard.Ticket> tickets) {
        List<LogEntry> mrXLog = board.getMrXTravelLog();
        int ticketVal = 0;
        //returning different ticket values by transportation respectively
        for(ScotlandYard.Ticket ticket : tickets){
            if(ticket.equals(ScotlandYard.Ticket.TAXI)) ticketVal += 2;
            if(ticket.equals(ScotlandYard.Ticket.BUS)) ticketVal += 4;
            if(ticket.equals(ScotlandYard.Ticket.UNDERGROUND)) ticketVal += 8;
            if (mrXLog.size() != 0){
                if(ticket.equals(ScotlandYard.Ticket.SECRET)) {
                    //if mrX's log size is larger than 0 and is after reveal
                    if(board.getSetup().moves.get(mrXLog.size() - 1)) {ticketVal += 20;}
                }
            }
        }
        return ticketVal;
    }
    //HELPER METHODS ENDS HERE//


    //Scoring method, returns the distance from the detectives' location to mrX's destination
    private Integer calculateDistance(Board board, Move move, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        List<Integer> detectivesLocation = getDetectivesLocation(board);
        int size;
        List<List<Integer>> shortPath = new ArrayList<>();
        List<List<Integer>> allPath = new ArrayList<>();
        Board update = updatedBoard(board, move);
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
            int mrXLocation = updateLocation(move);
            //using Dijkstra's algorithm to find the shortest path from the detective to mrX
            while (!queue.isEmpty()) {
                Integer currentNode = queue.poll();
                if (currentNode.equals(mrXLocation)) break;
                for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
                    Integer neighbour = edge.adjacentNode(currentNode);
                    int newDistance = distance.get(currentNode);
                    if (newDistance < distance.get(neighbour)) {
                        distance.put(neighbour, transportationCost(update, updateTicket(move)));
                        preNode.replace(neighbour, currentNode);
                        queue.remove(neighbour);
                        queue.add(neighbour);
                    }
                }
            }
            //store the path from detective's location to mrX's expected location
            List<Integer> path = new ArrayList<>();
            Integer node = mrXLocation;
            while(node != null){
                path.add(node);
                node = preNode.get(node);
            }
            //focus on the detectives who are close enough to consider
            if(path.size() < 5){
                shortPath.add(path);
            }
            //if not add every detective's path on the list
            allPath.add(path);
        }
        //calculate the total size of the paths
        size = shortPath.isEmpty() ? allPath.stream().mapToInt(List::size).sum() : shortPath.stream().mapToInt(List::size).sum();
        return size;
    }
}