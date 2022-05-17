package InternationalChess.pgn;

import InternationalChess.engine.classic.board.Board;
import InternationalChess.engine.classic.board.Move;
import InternationalChess.engine.classic.player.Player;

public interface PGNPersistence {

    void persistGame(Game game);

    Move getNextBestMove(Board board, Player player, String gameText);

}
