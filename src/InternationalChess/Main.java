package InternationalChess;
import InternationalChess.GUI.Login;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login mainFrame = new Login(700, 700);
            mainFrame.setVisible(true);
        });
    }
}
