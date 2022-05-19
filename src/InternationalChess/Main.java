package InternationalChess;
import InternationalChess.GUI.Login;


import javax.swing.*;


public class Main {
    public static void main(String[] args){
        ImageIcon ic = new ImageIcon("images/background.png");
        SwingUtilities.invokeLater(() -> {
            Login mainFrame;
            mainFrame = new Login(ic.getIconWidth(), ic.getIconHeight());
            mainFrame.setVisible(true);
        });
    }
}
