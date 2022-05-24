package InternationalChess;
import InternationalChess.GUI.Login;
import javax.swing.*;


public class Main {

    public static Play play;
    public static void main(String[] args) {
        ImageIcon ic = new ImageIcon("images/background.png");
        SwingUtilities.invokeLater(() -> {
            Login mainFrame;
            mainFrame = new Login(ic.getIconWidth(), ic.getIconHeight());
            mainFrame.setVisible(true);
        });
        String file = "music/music1.mp3";

        play = new Play(file);
        //播放音乐
        play.start();
        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


