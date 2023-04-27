package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.List;

public class Minimax extends MORIARTY{

    private Board board;
    private Board updated;
    private Move move;
    private int depth;
    private int score;
    private final long maxTime;
    private Move originalMove;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    private final long startTime;

    Minimax(long maxTime,
            Board updated,
            Move move,
            int depth,
            int alpha,
            int beta,
            Board board,
            Move originalMove){
        this.score = minimax(updated, move, depth, alpha, beta, board, originalMove);
        this.maxTime = maxTime;
        this.board = board;
        this.updated = updated;
        this.move = move;
        this.originalMove = originalMove;
        this.depth = depth;
        this.startTime = System.currentTimeMillis();
    }




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
}
