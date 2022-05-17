package InternationalChess.engine.classic.player.ai;

import InternationalChess.engine.classic.board.Board;

public interface BoardEvaluator {

    int evaluate(Board board, int depth);

}
