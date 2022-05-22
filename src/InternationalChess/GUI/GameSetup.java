package InternationalChess.GUI;

import InternationalChess.engine.classic.Alliance;
import InternationalChess.engine.classic.player.Player;
import InternationalChess.GUI.Game.PlayerType;
import javafx.stage.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class GameSetup extends JDialog {

    private PlayerType whitePlayerType;
    private PlayerType blackPlayerType;
    private JSpinner searchDepthSpinner;
    private static final String COMPUTER_WHITE = "White";
    private static final String COMPUTER_BLACK = "Black";

    GameSetup(final JFrame frame,
              final boolean modal) {
        super(frame, modal);
        final JPanel myPanel = new JPanel(new GridLayout(0, 1));
        final JRadioButton whiteComputerButton = new JRadioButton(COMPUTER_WHITE);
        final JRadioButton blackComputerButton = new JRadioButton(COMPUTER_BLACK);

        final ButtonGroup computerColor = new ButtonGroup();
        computerColor.add(whiteComputerButton);
        computerColor.add(blackComputerButton);
        blackComputerButton.setSelected(true);

        getContentPane().add(myPanel);
        myPanel.add(new JLabel("Choose the color for computer"));
        myPanel.add(whiteComputerButton);
        myPanel.add(blackComputerButton);

        final JButton okButton = new JButton("OK");

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                whitePlayerType = whiteComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
                blackPlayerType = blackComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
                GameSetup.this.setVisible(false);
            }
        });

        myPanel.add(okButton);

        setLocation(550,300);
        setSize(300,300);
        setVisible(false);
    }

    void promptUser() {
        setVisible(true);
        repaint();
    }

    boolean isAIPlayer(final Player player) {
        if(player.getAlliance() == Alliance.WHITE) {
            return getWhitePlayerType() == PlayerType.COMPUTER;
        }
        return getBlackPlayerType() == PlayerType.COMPUTER;
    }

    PlayerType getWhitePlayerType() {
        return this.whitePlayerType;
    }

    PlayerType getBlackPlayerType() {
        return this.blackPlayerType;
    }

    private static JSpinner addLabeledSpinner(final Container c,
                                              final String label,
                                              final SpinnerModel model) {
        final JLabel l = new JLabel(label);
        c.add(l);
        final JSpinner spinner = new JSpinner(model);
        l.setLabelFor(spinner);
        c.add(spinner);
        return spinner;
    }

    int getSearchDepth() {
        /*return (Integer)this.searchDepthSpinner.getValue();*/
        return 1;
    }
}
