package InternationalChess.GUI;
import javax.swing.*;
import java.awt.*;

import static InternationalChess.GUI.Game.loadPGNFile;


/**
 * 这个类表示游戏过程中的整个游戏界面，是一切的载体
 */
public class Login extends JFrame {
    private final int WIDTH;
    private final int HEIGHT;

    public Login(int width, int height) {
        setTitle("International Chess"); //设置标题
        this.WIDTH = width;
        this.HEIGHT = height;

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //设置程序关闭按键，如果点击右上方的叉就游戏全部关闭了
        setLayout(null);


        addLabel();
        addPvpButton();
        addPvcButton();
    }

    /**
     * 在游戏面板中添加标签
     */
    private void addLabel() {
        JLabel statusLabel = new JLabel("Welcome to International Chess!");
        statusLabel.setLocation(HEIGHT / 7 +20, HEIGHT / 4);
        statusLabel.setSize(500, 200);
        statusLabel.setFont(new Font("Rockwell", Font.BOLD, 25));
        add(statusLabel);
    }

    /**
     * 在游戏面板中增加一个按钮，如果按下的话就会显示Hello, world!
     */

    private void addPvpButton() {
        JButton button = new JButton("New Game");
        button.setLocation(HEIGHT / 7, HEIGHT / 10 + 340);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
            /*以下3行：
             关闭原来的界面
             */
            JComponent comp = (JComponent) e.getSource();
            Window win = SwingUtilities.getWindowAncestor(comp);
            win.dispose();
            Game.get().show();
        });
    }

    private void addPvcButton() {
        JButton button = new JButton("Load Game");
        button.setLocation(HEIGHT / 7 + 300, HEIGHT / 10 + 340);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
           /*以下3行：
             关闭原来的界面
             */
            JComponent comp = (JComponent) e.getSource();
            Window win = SwingUtilities.getWindowAncestor(comp);
            win.dispose();
                JFileChooser chooser = new JFileChooser();
                int option = chooser.showOpenDialog(Game.get().getGameFrame());
                if (option == JFileChooser.APPROVE_OPTION) {
                    loadPGNFile(chooser.getSelectedFile());
                }
        });
    }

}
