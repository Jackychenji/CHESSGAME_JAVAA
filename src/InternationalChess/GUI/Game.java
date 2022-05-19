package InternationalChess.GUI;

import InternationalChess.engine.classic.Alliance;
import InternationalChess.engine.classic.board.*;
import InternationalChess.engine.classic.board.Move.MoveFactory;
import InternationalChess.engine.classic.pieces.Piece;
import InternationalChess.engine.classic.player.Player;
import InternationalChess.engine.classic.player.ai.StandardBoardEvaluator;
import InternationalChess.engine.classic.player.ai.StockAlphaBeta;
import InternationalChess.pgn.FenUtilities;
import InternationalChess.pgn.MySqlGamePersistence;
import com.google.common.collect.Lists;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import static InternationalChess.pgn.PGNUtilities.persistPGNFile;
import static InternationalChess.pgn.PGNUtilities.writeGameToPGNFile;
import static javax.swing.JFrame.setDefaultLookAndFeelDecorated;
import static javax.swing.SwingUtilities.*;

public final class Game extends Observable {

    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;//人机对战
    private Board chessBoard;
    //private JButton black;
    //private JButton white;
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

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(1100, 1100);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(750, 700);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static final Game INSTANCE = new Game();

    private Game() {
        this.gameFrame = new JFrame("Wizard Chess");
        final JMenuBar tableMenuBar = new JMenuBar();
        populateMenuBar(tableMenuBar);
        final JButton reset = new JButton("reset");//重新开始
        reset.setLocation(800, 200);
        reset.setFont(new Font("Rockwell", Font.BOLD, 25));
        reset.setSize(200, 60);
        reset.addActionListener(e -> {
            undoAllMoves();

        });
        this.gameFrame.add(reset);
        final JButton undo = new JButton("undo");//悔棋
        undo.setLocation(800, 300);
        undo.setFont(new Font("Rockwell", Font.BOLD, 25));
        undo.setSize(200, 60);
        undo.addActionListener(e -> {
            if(Game.get().getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        this.gameFrame.add(undo);

        /*black = new JButton("black");//黑棋
        black.setLocation(800, 600);
        black.setFont(new Font("Rockwell", Font.BOLD, 25));
        black.setSize(200, 60);
        black.setVisible(false);
        this.gameFrame.add(black);
        white = new JButton("white");//白棋
        white.setLocation(800, 700);
        white.setFont(new Font("Rockwell", Font.BOLD, 25));
        white.setSize(200, 60);
        white.setVisible(true);
        this.gameFrame.add(white);*/

        state = new JLabel();
        state.setLocation(800,100);
        state.setText("WHITE");
        state.setFont(new Font("Rockwell", Font.BOLD, 25));
        state.setSize(200,60);
        state.setVisible(true);
        this.gameFrame.add(state);

        final JButton save = new JButton("save");//悔棋
        save.setLocation(800, 400);
        save.setFont(new Font("Rockwell", Font.BOLD, 25));
        save.setSize(200, 60);
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
                savePGNFile(chooser.getSelectedFile());
            }
        });
        this.gameFrame.add(save);
        surrender = new JButton("surrender");
        surrender.setLocation(800,600);
        surrender.setFont(new Font("Rockwell", Font.BOLD, 25));
        surrender.setSize(200,60);
        surrender.addActionListener(e -> {
            JOptionPane.showMessageDialog(Game.get().getBoardPanel(),
                    "Game Over: Player " + Game.get().getGameBoard().currentPlayer() + " surrenders, "
                    + "Player " + Game.get().getGameBoard().currentPlayer().getOpponent() + " wins!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        this.gameFrame.add(surrender);

        final JButton flip = new JButton("flip");//翻转棋盘
        flip.setLocation(800,500);
        flip.setFont(new Font("Rockwell", Font.BOLD, 25));
        flip.setSize(200,60);
        this.gameFrame.add(flip);
        this.gameFrame.setJMenuBar(tableMenuBar);//上面
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
        //this.useBook = false;
        this.moveLog = new MoveLog();
        this.addObserver(new TableGameAIWatcher());
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.gameFrame.add(this.boardPanel, BorderLayout.WEST);
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
         state.setText("WHITE");
         state.setVisible(true);
         //black.setVisible(false);
         //white.setVisible(true);
     }else {
         state.setText("BLACK");
         state.setVisible(true);
         //white.setVisible(false);
         //black.setVisible(true);
     }
    }

   /* private GameHistoryPanel getGameHistoryPanel() {
        return this.gameHistoryPanel;
    }*/

    /*private TakenPiecesPanel getTakenPiecesPanel() {
        return this.takenPiecesPanel;
    }*/

    /*private DebugPanel getDebugPanel() {
        return this.debugPanel;
    }*/

    private GameSetup getGameSetup() {
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
        //Game.get().getState();
        //Game.get().getGameHistoryPanel().redo(chessBoard, Game.get().getMoveLog());
        //Game.get().getTakenPiecesPanel().redo(Game.get().getMoveLog());
        Game.get().getBoardPanel().drawBoard(Game.get().getGameBoard());

        //Table.get().getDebugPanel().redo();
    }

    private void populateMenuBar(final JMenuBar tableMenuBar) {
        tableMenuBar.add(createFileMenu());
        //tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
    }

    private static void center(final JFrame frame) {
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = frame.getSize().width;
        final int h = frame.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        frame.setLocation(x, y);
    }

    private JMenu createFileMenu() {
        final JMenu filesMenu = new JMenu("File");
        filesMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem openPGN = new JMenuItem("PGN File", KeyEvent.VK_O);
        openPGN.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(Game.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                loadPGNFile(chooser.getSelectedFile());
            }
        });
        filesMenu.add(openPGN);

        final JMenuItem openFEN = new JMenuItem("Load FEN File", KeyEvent.VK_F);
        openFEN.addActionListener(e -> {
//            String fenString = JOptionPane.showInputDialog("Input FEN");
//            if(fenString != null) {
//                undoAllMoves();
//                chessBoard = FenUtilities.createGameFromFEN(fenString);
//                Game.get().getBoardPanel().drawBoard(chessBoard);
//            }
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(Game.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                String fenString = loadFENFile(chooser.getSelectedFile());
                if (fenString != null) {
                    undoAllMoves();
                    chessBoard = FenUtilities.createGameFromFEN(fenString);
                    Game.get().getBoardPanel().drawBoard(chessBoard);

                }
            }
        });
        filesMenu.add(openFEN);

        final JMenuItem saveToPGN = new JMenuItem("Save Game", KeyEvent.VK_S);
        saveToPGN.addActionListener(e -> {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return ".pgn";
                }
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith("pgn");
                }
            });
            final int option = chooser.showSaveDialog(Game.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                savePGNFile(chooser.getSelectedFile());
            }
        });
        filesMenu.add(saveToPGN);

        final JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(e -> {
            Game.get().getGameFrame().dispose();
            System.exit(0);
        });
        filesMenu.add(exitMenuItem);

        return filesMenu;
    }

    private JMenu createOptionsMenu() {

        final JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);

        final JMenuItem resetMenuItem = new JMenuItem("New Game", KeyEvent.VK_P);
        resetMenuItem.addActionListener(e -> undoAllMoves());
        optionsMenu.add(resetMenuItem);

        final JMenuItem evaluateBoardMenuItem = new JMenuItem("Evaluate Board", KeyEvent.VK_E);
        evaluateBoardMenuItem.addActionListener(e -> System.out.println(StandardBoardEvaluator.get().evaluationDetails(chessBoard, gameSetup.getSearchDepth())));
        optionsMenu.add(evaluateBoardMenuItem);

        final JMenuItem escapeAnalysis = new JMenuItem("Escape Analysis Score", KeyEvent.VK_S);
        escapeAnalysis.addActionListener(e -> {
            final Move lastMove = moveLog.getMoves().get(moveLog.size() - 1);
            if(lastMove != null) {
                System.out.println(MoveUtils.exchangeScore(lastMove));
            }

        });
        optionsMenu.add(escapeAnalysis);

        final JMenuItem legalMovesMenuItem = new JMenuItem("Current State", KeyEvent.VK_L);

        legalMovesMenuItem.addActionListener(e -> {
            System.out.println(chessBoard.getWhitePieces());
            System.out.println(chessBoard.getBlackPieces());
            System.out.println(playerInfo(chessBoard.currentPlayer()));
        });
        optionsMenu.add(legalMovesMenuItem);

        final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move", KeyEvent.VK_M);
        undoMoveMenuItem.addActionListener(e -> {
            if(Game.get().getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        optionsMenu.add(undoMoveMenuItem);

        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game", KeyEvent.VK_S);
        setupGameMenuItem.addActionListener(e -> {
            Game.get().getGameSetup().promptUser();
            Game.get().setupUpdate(Game.get().getGameSetup());
        });
        optionsMenu.add(setupGameMenuItem);

        return optionsMenu;
    }

    /*private JMenu createPreferencesMenu() {

        final JMenu preferencesMenu = new JMenu("Preferences");

        final JMenu colorChooserSubMenu = new JMenu("Choose Colors");
        colorChooserSubMenu.setMnemonic(KeyEvent.VK_S);

        final JMenuItem chooseDarkMenuItem = new JMenuItem("Choose Dark Tile Color");
        colorChooserSubMenu.add(chooseDarkMenuItem);

        final JMenuItem chooseLightMenuItem = new JMenuItem("Choose Light Tile Color");
        colorChooserSubMenu.add(chooseLightMenuItem);

        final JMenuItem chooseLegalHighlightMenuItem = new JMenuItem(
                "Choose Legal Move Highlight Color");
        colorChooserSubMenu.add(chooseLegalHighlightMenuItem);

        preferencesMenu.add(colorChooserSubMenu);

        chooseDarkMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Game.get().getGameFrame(), "Choose Dark Tile Color",
                    Game.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Game.get().getBoardPanel().setTileDarkColor(chessBoard, colorChoice);
            }
        });

        chooseLightMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Game.get().getGameFrame(), "Choose Light Tile Color",
                    Game.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Game.get().getBoardPanel().setTileLightColor(chessBoard, colorChoice);
            }
        });

        final JMenu chessMenChoiceSubMenu = new JMenu("Choose Chess Men Image Set");

        final JMenuItem holyWarriorsMenuItem = new JMenuItem("Holy Warriors");
        chessMenChoiceSubMenu.add(holyWarriorsMenuItem);

        final JMenuItem rockMenMenuItem = new JMenuItem("Rock Men");
        chessMenChoiceSubMenu.add(rockMenMenuItem);

        final JMenuItem abstractMenMenuItem = new JMenuItem("Abstract Men");
        chessMenChoiceSubMenu.add(abstractMenMenuItem);

        final JMenuItem woodMenMenuItem = new JMenuItem("Wood Men");
        chessMenChoiceSubMenu.add(woodMenMenuItem);

        final JMenuItem fancyMenMenuItem = new JMenuItem("Fancy Men");
        chessMenChoiceSubMenu.add(fancyMenMenuItem);

        final JMenuItem fancyMenMenuItem2 = new JMenuItem("Fancy Men 2");
        chessMenChoiceSubMenu.add(fancyMenMenuItem2);

        woodMenMenuItem.addActionListener(e -> {
            System.out.println("implement me");
            Game.get().getGameFrame().repaint();
        });

        holyWarriorsMenuItem.addActionListener(e -> {
            pieceIconPath = "art/holywarriors/";
            Game.get().getBoardPanel().drawBoard(chessBoard);
        });

        rockMenMenuItem.addActionListener(e -> {
        });

        abstractMenMenuItem.addActionListener(e -> {
            pieceIconPath = "art/simple/";
            Game.get().getBoardPanel().drawBoard(chessBoard);
        });

        fancyMenMenuItem2.addActionListener(e -> {
            pieceIconPath = "art/fancy2/";
            Game.get().getBoardPanel().drawBoard(chessBoard);
        });

        fancyMenMenuItem.addActionListener(e -> {
            pieceIconPath = "art/fancy/";
            Game.get().getBoardPanel().drawBoard(chessBoard);
        });

        preferencesMenu.add(chessMenChoiceSubMenu);

        chooseLegalHighlightMenuItem.addActionListener(e -> {
            System.out.println("implement me");
            Game.get().getGameFrame().repaint();
        });

        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip board");

        flipBoardMenuItem.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            boardPanel.drawBoard(chessBoard);
        });

        preferencesMenu.add(flipBoardMenuItem);
        preferencesMenu.addSeparator();


        final JCheckBoxMenuItem cbLegalMoveHighlighter = new JCheckBoxMenuItem(
                "Highlight Legal Moves", false);

        cbLegalMoveHighlighter.addActionListener(e -> highlightLegalMoves = cbLegalMoveHighlighter.isSelected());

        preferencesMenu.add(cbLegalMoveHighlighter);

        final JCheckBoxMenuItem cbUseBookMoves = new JCheckBoxMenuItem(
                "Use Book Moves", false);

        cbUseBookMoves.addActionListener(e -> useBook = cbUseBookMoves.isSelected());

        preferencesMenu.add(cbUseBookMoves);

        return preferencesMenu;

    }*/

    private static String playerInfo(final Player player) {
        return ("Player is: " +player.getAlliance() + "\nlegal moves (" +player.getLegalMoves().size()+ ") = " +player.getLegalMoves() + "\ninCheck = " +
                player.isInCheck() + "\nisInCheckMate = " +player.isInCheckMate() +
                "\nisCastled = " +player.isCastled())+ "\n";
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
        Game.get().getMoveLog().clear();
        //Game.get().getGameHistoryPanel().redo(chessBoard, Game.get().getMoveLog());
        //Game.get().getTakenPiecesPanel().redo(Game.get().getMoveLog());
        Game.get().getBoardPanel().drawBoard(chessBoard);
        //Table.get().getDebugPanel().redo();
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
    void saveFENFile(final Board board){
        try {
            File testFile = new File("D:\\系统文件\\桌面\\save.txt");
            if (testFile.exists()) {
                testFile.delete();
            }

            testFile.createNewFile();

            BufferedWriter writer=new BufferedWriter(new FileWriter(testFile));
            writer.write(FenUtilities.createFENFromGame(board));
            writer.close();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

    }

    static void loadPGNFile(final File pgnFile) {
        try {
            persistPGNFile(pgnFile);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static void savePGNFile(final File pgnFile) {
        try {
            writeGameToPGNFile(pgnFile, Game.get().getMoveLog());
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
        //Game.get().getGameHistoryPanel().redo(chessBoard, Game.get().getMoveLog());
        //Game.get().getTakenPiecesPanel().redo(Game.get().getMoveLog());
        Game.get().getBoardPanel().drawBoard(chessBoard);
        //Table.get().getDebugPanel().redo();
    }

    private void moveMadeUpdate(final PlayerType playerType) {
        setChanged();
        notifyObservers(playerType);
    }

    private void setupUpdate(final GameSetup gameSetup) {
        setChanged();
        notifyObservers(gameSetup);
    }

    private static class TableGameAIWatcher
            implements Observer {

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
                //strategy.addObserver(Table.get().getDebugPanel());
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
                //Game.get().getGameHistoryPanel().redo(Game.get().getGameBoard(), Game.get().getMoveLog());
                //Game.get().getTakenPiecesPanel().redo(Game.get().getMoveLog());
                Game.get().getBoardPanel().drawBoard(Game.get().getGameBoard());
                //Table.get().getDebugPanel().redo();
                Game.get().moveMadeUpdate(PlayerType.COMPUTER);//不能注释
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class BoardPanel extends JPanel {

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

        void setTileDarkColor(final Board board,
                              final Color darkColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setDarkTileColor(darkColor);
            }
            drawBoard(board);
        }

        void setTileLightColor(final Board board,
                               final Color lightColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setLightTileColor(lightColor);
            }
            drawBoard(board);
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
                        //gameHistoryPanel.redo(chessBoard, moveLog);
                        //takenPiecesPanel.redo(moveLog);
                        //if (gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
                        Game.get().moveMadeUpdate(PlayerType.HUMAN);
                        //}
                        boardPanel.drawBoard(chessBoard);
                        //debugPanel.redo();
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

        void setLightTileColor(final Color color) {
            lightTileColor = color;
        }

        void setDarkTileColor(final Color color) {
            darkTileColor = color;
        }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null &&
                    humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() &&
                    humanMovedPiece.getPiecePosition() == this.tileId) {
                //setBorder(BorderFactory.createLineBorder(Color.cyan));
                setBackground(Color.RED);
            } /*else {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }*/
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

