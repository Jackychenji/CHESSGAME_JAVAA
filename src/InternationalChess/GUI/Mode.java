package InternationalChess.GUI;
import InternationalChess.pgn.FenUtilities;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * 这个类表示游戏过程中的整个游戏界面，是一切的载体
 */
public class Mode extends JFrame {
    private final int WIDTH;
    private final int HEIGHT;

    public Mode(int width, int height){
        setTitle("Wizard Chess"); //设置标题
        this.WIDTH = width;
        this.HEIGHT = height;
        ImageIcon ic = new ImageIcon("images/background.png");
        JLabel jLabel = new JLabel(ic);
        jLabel.setBounds(0,0,ic.getIconWidth(),ic.getIconHeight());

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //设置程序关闭按键，如果点击右上方的叉就游戏全部关闭了
        setLayout(null);

        addPvpButton();
        addPvcButton();
        getContentPane().add(jLabel);
    }

    private void addPvpButton() {
        ImageIcon icon = new ImageIcon("images/pvp.png");
        icon.setImage(icon.getImage().getScaledInstance(400,150,Image.SCALE_DEFAULT));
        JButton button = new JButton(icon);
        button.setLocation(HEIGHT / 7 + 50, HEIGHT / 10 + 400);
        button.setSize(400, 150);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        add(button);

        button.addActionListener(e -> {
            Game.get().show();
            /*以下3行：
             关闭原来的界面
             */
            JComponent comp = (JComponent) e.getSource();
            Window win = SwingUtilities.getWindowAncestor(comp);
            win.dispose();
        });
    }

    private void addPvcButton() {
        ImageIcon icon = new ImageIcon("images/pvc.png");
        icon.setImage(icon.getImage().getScaledInstance(400,140,Image.SCALE_DEFAULT));
        JButton button = new JButton(icon);
        button.setLocation(HEIGHT / 7 + 600, HEIGHT / 10 + 400);
        button.setSize(400, 140);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        add(button);

        button.addActionListener(e -> {
            Game.get().getGameSetup().promptUser();
            Game.get().setupUpdate(Game.get().getGameSetup());
            /*以下3行：
             关闭原来的界面
             */
            JComponent comp = (JComponent) e.getSource();
            Window win = SwingUtilities.getWindowAncestor(comp);
            win.dispose();
        });
    }

}
