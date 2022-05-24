package InternationalChess.GUI;

import InternationalChess.Main;
import InternationalChess.Play;
import InternationalChess.engine.classic.Alliance;
import InternationalChess.engine.classic.board.Board;
import InternationalChess.engine.classic.board.BoardUtils;
import InternationalChess.engine.classic.board.Move;
import InternationalChess.engine.classic.board.Move.MoveFactory;
import InternationalChess.engine.classic.board.MoveTransition;
import InternationalChess.engine.classic.pieces.Piece;
import InternationalChess.engine.classic.player.Player;
import InternationalChess.engine.classic.player.ai.StockAlphaBeta;
import InternationalChess.pgn.FenUtilities;
import InternationalChess.pgn.MySqlGamePersistence;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import static javax.swing.JFrame.setDefaultLookAndFeelDecorated;
import static javax.swing.SwingUtilities.*;

public final class Game extends Observable {

    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;//人机对战
    private Board chessBoard;
    private JLabel state;
    private JButton surrender;
    private Move computerMove;
    private Piece sourceTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;
    private String pieceIconPath;
    private boolean highlightLegalMoves;
    private boolean useBook;
    private Color darkTileColor = new Color(181,126,99);
    private Color lightTileColor =  new Color(250,217,181);

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(1100, 900);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(750, 700);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static final Game INSTANCE = new Game();
    public static Play play2;
    public boolean isSurrendered = false;

    private Game() {
        this.gameFrame = new JFrame("Wizard Chess");
        ImageIcon icon = new ImageIcon("images/reset.png");
        icon.setImage(icon.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton reset = new JButton(icon);//重新开始
        reset.setLocation(800, 120);
        reset.setBorderPainted(false);
        reset.setContentAreaFilled(false);
        reset.setOpaque(false);
        reset.setSize(230, 80);
        reset.addActionListener(e -> {
            chessBoard = Board.createStandardBoard();
            setStatement();
            Game.get().getBoardPanel().drawBoard(chessBoard);
            if (isSurrendered){
                Game.play2.stop();
                Main.play.start();
            }
            if (TableGameAIWatcher.isCheckmate){
                TableGameAIWatcher.play2.stop();
                Main.play.start();
            }
        });
        this.gameFrame.add(reset);
        ImageIcon icon2 = new ImageIcon("images/undo.png");
        icon2.setImage(icon2.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton undo = new JButton(icon2);//悔棋
        undo.setLocation(800, 210);
        undo.setBorderPainted(false);
        undo.setContentAreaFilled(false);
        undo.setOpaque(false);
        undo.setSize(230, 80);
        undo.addActionListener(e -> {
            if(Game.get().getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        this.gameFrame.add(undo);


        state = new JLabel();
        state.setLocation(790,50);
        state.setText("WHITE's TURN");
        state.setFont(new Font("Harry P", Font.BOLD, 46));
        state.setSize(250,60);
        state.setVisible(true);
        this.gameFrame.add(state);

        ImageIcon icon3 = new ImageIcon("images/save.png");
        icon3.setImage(icon3.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton save = new JButton(icon3);//储存
        save.setLocation(800, 300);
        save.setBorderPainted(false);
        save.setContentAreaFilled(false);
        save.setOpaque(false);
        save.setSize(230, 80);
        save.addActionListener(e -> {//存档
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return ".txt";
                }
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith("txt");
                }
            });
            final int option = chooser.showSaveDialog(Game.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                saveFENFile(chooser.getSelectedFile());
            }
        });
        this.gameFrame.add(save);
        ImageIcon icon4 = new ImageIcon("images/surrender.png");
        icon4.setImage(icon4.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        surrender = new JButton(icon4);
        surrender.setLocation(800, 570);
        surrender.setBorderPainted(false);
        surrender.setContentAreaFilled(false);
        surrender.setOpaque(false);
        surrender.setSize(230, 80);
        surrender.addActionListener(e -> {
            isSurrendered = true;
            Main.play.stop();
            String file = "music/Patrick Doyle-Hogwart's March.mp3";
            play2 = new Play(file);
            play2.start();
            try {
                Thread.sleep(1);

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                    "Game Over: Player " + Game.get().getGameBoard().currentPlayer() + " surrenders, "
                            + "Player " + Game.get().getGameBoard().currentPlayer().getOpponent() + " wins!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        this.gameFrame.add(surrender);

        ImageIcon icon5 = new ImageIcon("images/flip.png");
        icon5.setImage(icon5.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton flip = new JButton(icon5);//翻转棋盘
        flip.setLocation(800, 480);
        flip.setBorderPainted(false);
        flip.setContentAreaFilled(false);
        flip.setOpaque(false);
        flip.setSize(230, 80);
        this.gameFrame.add(flip);

        ImageIcon icon6 = new ImageIcon("images/exit.png");
        icon6.setImage(icon6.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton exit = new JButton(icon6);//退出,返回主界面
        exit.setLocation(800, 660);
        exit.setBorderPainted(false);
        exit.setContentAreaFilled(false);
        exit.setOpaque(false);
        exit.setSize(230, 80);
        exit.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> System.exit(0));
        });
        this.gameFrame.add(exit);

        ImageIcon icon7 = new ImageIcon("images/load.png");
        icon7.setImage(icon7.getImage().getScaledInstance(230,80,Image.SCALE_DEFAULT));
        final JButton load = new JButton(icon7);//
        load.setLocation(800, 390);
        load.setBorderPainted(false);
        load.setContentAreaFilled(false);
        load.setOpaque(false);
        load.setSize(230, 80);
        load.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(Game.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                String fenString = loadFENFile(chooser.getSelectedFile());
                if (fenString != null) {
                    boolean truth = true;
                    String[] fenstr = fenString.split("/");
                    for (int i = 0; i< fenstr.length-1; i++){
                        if (fenstr[i].length()>8|(fenstr[i].length() == 1&&Integer.parseInt(fenstr[i])>8)|fenstr.length!=8){
                            truth = false;
                        }
                    }
                    if (truth) {
                        undoAllMoves();
                        chessBoard = FenUtilities.createGameFromFEN(fenString);
                        setStatement();
                        Game.get().getBoardPanel().drawBoard(chessBoard);
                    }else {
                        JOptionPane.showMessageDialog(InternationalChess.GUI.Game.get().getBoardPanel(),
                                "错误编码：101", "Warning",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        this.gameFrame.add(load);

        this.gameFrame.setLayout(new BorderLayout());
        this.chessBoard = Board.createStandardBoard();
        this.boardDirection = BoardDirection.NORMAL;
        this.pieceIconPath = "images/fancy/";
        this.boardPanel = new BoardPanel();
        flip.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            boardPanel.drawBoard(chessBoard);
        });
        this.highlightLegalMoves = true;
        this.moveLog = new MoveLog();
        this.addObserver(new TableGameAIWatcher());
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.gameFrame.add(this.boardPanel, BorderLayout.WEST);
        ImageIcon ic = new ImageIcon("images/background2.png");
        ic.setImage(ic.getImage().getScaledInstance(350,800,Image.SCALE_DEFAULT));
        JLabel jLabel = new JLabel(ic);
        jLabel.setBounds(750,0,ic.getIconWidth(),ic.getIconHeight());
        this.gameFrame.add(jLabel);
        setDefaultLookAndFeelDecorated(true);
        this.gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        center(this.gameFrame);
        this.gameFrame.setVisible(true);
    }

    public static Game get() {
        return INSTANCE;
    }

    JFrame getGameFrame() {
        return this.gameFrame;
    }

    private Board getGameBoard() {
        return this.chessBoard;
    }

    private MoveLog getMoveLog() {
        return this.moveLog;
    }

    public BoardPanel getBoardPanel() {
        return this.boardPanel;
    }

    public void setStatement(){
     if (chessBoard.currentPlayer().getAlliance()==Alliance.WHITE){
         state.setText("WHITE's TURN");
         state.setVisible(true);
     }else {
         state.setText("BLACK's TURN");
         state.setVisible(true);
     }
    }


    GameSetup getGameSetup() {
        return this.gameSetup;
    }

    private boolean getHighlightLegalMoves() {
        return this.highlightLegalMoves;
    }

    private boolean getUseBook() {
        return this.useBook;
    }

    public void show() {
        Game.get().getMoveLog().clear();
        Game.get().getBoardPanel().drawBoard(Game.get().getGameBoard());
    }

    private static void center(final JFrame frame) {
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = frame.getSize().width;
        final int h = frame.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        frame.setLocation(x, y);
    }


    private void updateGameBoard(final Board board) {
        this.chessBoard = board;
    }

    private void updateComputerMove(final Move move) {
        this.computerMove = move;
    }

    private void undoAllMoves() {
        for(int i = Game.get().getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = Game.get().getMoveLog().removeMove(Game.get().getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        }
        setStatement();
        this.computerMove = null;
        Game.get().getBoardPanel().drawBoard(chessBoard);
    }

    public static String loadFENFile(final File fenFile){
        try {
            BufferedReader in=new BufferedReader(new FileReader(fenFile));
            if (fenFile.getName().toLowerCase().endsWith("txt")){
                String fenString = in.readLine();
                FenUtilities.createGameFromFEN(fenString);
                return fenString;
            }else {
                JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                        "错误编码 104", "Warning",
                        JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

        }
        catch (final IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    public void saveFENFile(final File fenFile){
        try {
            Board board = this.chessBoard;
            try (final Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fenFile, true)))) {
                writer.write(FenUtilities.createFENFromGame(board));
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

    }




    private void undoLastMove() {
        final Move lastMove = Game.get().getMoveLog().removeMove(Game.get().getMoveLog().size() - 1);
        this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        this.computerMove = null;
        Game.get().getMoveLog().removeMove(lastMove);
        Game.get().getBoardPanel().drawBoard(chessBoard);
    }

    private void moveMadeUpdate(final PlayerType playerType) {
        setChanged();
        notifyObservers(playerType);
    }

    void setupUpdate(final GameSetup gameSetup) {
        setChanged();
        notifyObservers(gameSetup);
    }

    private static class TableGameAIWatcher
            implements Observer {

        public static Play play2;
        public static boolean isCheckmate = false;
        @Override
        public void update(final Observable o,
                           final Object arg) {

            if (Game.get().getGameSetup().isAIPlayer(Game.get().getGameBoard().currentPlayer()) &&
                    !Game.get().getGameBoard().currentPlayer().isInCheckMate() &&
                    !Game.get().getGameBoard().currentPlayer().isInStaleMate()) {
                System.out.println(Game.get().getGameBoard().currentPlayer() + " is set to AI, thinking....");
                final AIThinkTank thinkTank = new AIThinkTank();
                thinkTank.execute();
            }

            if (Game.get().getGameBoard().currentPlayer().isInCheck() &&
                    !Game.get().getGameBoard().currentPlayer().isInCheckMate()) {
                JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                        "Player " + Game.get().getGameBoard().currentPlayer() + " is being checked!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }
            if (Game.get().getGameBoard().currentPlayer().isInCheckMate()) {
                isCheckmate = true;
                Main.play.stop();
                String file = "music/Patrick Doyle-Hogwart's March.mp3";
                play2 = new Play(file);
                play2.start();
                try {
                    Thread.sleep(1);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                        "Game Over: Player " + Game.get().getGameBoard().currentPlayer() + " is in checkmate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (Game.get().getGameBoard().currentPlayer().isInStaleMate()) {
                JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                        "Game Over: Player " + Game.get().getGameBoard().currentPlayer() + " is in stalemate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }

    }

    enum PlayerType {
        HUMAN,
        COMPUTER
    }//人机/人人choose

    private static class AIThinkTank extends SwingWorker<Move, String> {

        private AIThinkTank() {
        }

        @Override
        protected Move doInBackground() {
            final Move bestMove;
            final Move bookMove = Game.get().getUseBook()
                    ? MySqlGamePersistence.get().getNextBestMove(Game.get().getGameBoard(),
                    Game.get().getGameBoard().currentPlayer(),
                    Game.get().getMoveLog().getMoves().toString().replaceAll("\\[", "").replaceAll("]", ""))
                    : MoveFactory.getNullMove();
            if (Game.get().getUseBook() && bookMove != MoveFactory.getNullMove()) {
                bestMove = bookMove;
            }
            else {
                final StockAlphaBeta strategy = new StockAlphaBeta(Game.get().getGameSetup().getSearchDepth());
                bestMove = strategy.execute(Game.get().getGameBoard());
            }
            return bestMove;
        }

        @Override
        public void done() {
            try {
                final Move bestMove = get();
                Game.get().updateComputerMove(bestMove);
                Game.get().updateGameBoard(Game.get().getGameBoard().currentPlayer().makeMove(bestMove).getToBoard());
                Game.get().getMoveLog().addMove(bestMove);
                Game.get().getBoardPanel().drawBoard(Game.get().getGameBoard());
                Game.get().moveMadeUpdate(PlayerType.COMPUTER);//不能注释
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Color.decode("#8B4726"));
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel boardTile : boardDirection.traverse(boardTiles)) {
                boardTile.drawTile(board);
                add(boardTile);
            }
            validate();
            repaint();
        }
    }

    enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();

    }

    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return this.moves;
        }

        void addMove(final Move move) {
            this.moves.add(move);
        }

        public int size() {
            return this.moves.size();
        }

        void clear() {
            this.moves.clear();
        }

        Move removeMove(final int index) {
            return this.moves.remove(index);
        }

        boolean removeMove(final Move move) {
            return this.moves.remove(move);
        }

    }

    private class TilePanel extends JPanel {

        private final int tileId;

        TilePanel(final BoardPanel boardPanel,
                  final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            highlightTileBorder(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {

                    if(Game.get().getGameSetup().isAIPlayer(Game.get().getGameBoard().currentPlayer()) ||
                            BoardUtils.isEndGame(Game.get().getGameBoard())) {
                        return;
                    }

                    if (isRightMouseButton(event)) {
                        sourceTile = null;
                        humanMovedPiece = null;
                    } else if (isLeftMouseButton(event)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getPiece(tileId);
                            humanMovedPiece = sourceTile;
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                        } else {
                            final Move move = MoveFactory.createMove(chessBoard, sourceTile.getPiecePosition(),
                                    tileId);
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {
                                chessBoard = transition.getToBoard();
                                moveLog.addMove(move);
                                setStatement();
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                        }
                    }
                    invokeLater(() -> {
                        Game.get().moveMadeUpdate(PlayerType.HUMAN);
                        boardPanel.drawBoard(chessBoard);
                    });
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (getBackground() == Color.GREEN){
                        setBackground(Color.GREEN);
                    } else if (getBackground() == Color.RED) {
                        setBackground(Color.RED);
                    }else {
                        assignTileColor();
                    }
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (getBackground() == Color.GREEN){
                        setBackground(Color.GREEN);
                    } else if (getBackground() == Color.RED) {
                        setBackground(Color.RED);
                    }else {
                        setBackground(Color.MAGENTA);
                    }

                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }
            });
            validate();
        }

        void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightTileBorder(board);
            highlightLegals(board);
            highlightAIMove();
            validate();
            repaint();
        }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null &&
                    humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() &&
                    humanMovedPiece.getPiecePosition() == this.tileId) {
                setBackground(Color.RED);
            }
        }

        private void highlightAIMove() {
            if(computerMove != null) {
                if(this.tileId == computerMove.getCurrentCoordinate()) {
                    setBackground(Color.pink);
                } else if(this.tileId == computerMove.getDestinationCoordinate()) {
                    setBackground(Color.pink);
                }
            }
        }

        private void highlightLegals(final Board board) {
            if (Game.get().getHighlightLegalMoves()) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        setBackground(Color.GREEN);
                    }
                }
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if(board.getPiece(this.tileId) != null) {
                try{
                    final BufferedImage image = ImageIO.read(new File(pieceIconPath +
                            board.getPiece(this.tileId).getPieceAllegiance().toString().substring(0, 1) + "" +
                            board.getPiece(this.tileId).toString() +
                            ".gif"));
                    add(new JLabel(new ImageIcon(image)));
                } catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void assignTileColor() {
            if (BoardUtils.INSTANCE.FIRST_ROW.get(this.tileId) ||
                    BoardUtils.INSTANCE.THIRD_ROW.get(this.tileId) ||
                    BoardUtils.INSTANCE.FIFTH_ROW.get(this.tileId) ||
                    BoardUtils.INSTANCE.SEVENTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if(BoardUtils.INSTANCE.SECOND_ROW.get(this.tileId) ||
                    BoardUtils.INSTANCE.FOURTH_ROW.get(this.tileId) ||
                    BoardUtils.INSTANCE.SIXTH_ROW.get(this.tileId)  ||
                    BoardUtils.INSTANCE.EIGHTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }
    }
}

