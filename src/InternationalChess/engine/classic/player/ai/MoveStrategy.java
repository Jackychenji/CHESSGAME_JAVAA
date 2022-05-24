package InternationalChess.engine.classic.player.ai;

import InternationalChess.engine.classic.board.Board;
import InternationalChess.engine.classic.board.Move;

public interface MoveStrategy {


    Move execute(Board board);

}
