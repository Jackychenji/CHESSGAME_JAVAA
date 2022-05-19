package InternationalChess;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.FileInputStream;
import java.io.IOException;

public  class Play implements Runnable {

    private final String file;
    private AdvancedPlayer player = null;
    private Thread thread = null;

    public Play(String file) {
        this.file = file;
    }

    public void run() {
        // 每次开始需要重新创建AdvancedPlayer，否则会报错
        createPlayer();
        play();
    }

    public void start() {
        thread = new Thread(this, "Player thread");
        thread.start();
    }

    public void stop() {
        player.stop();
        thread = null;
    }

    protected void play() {
        try {
            player.play();
        } catch (JavaLayerException ex) {
            System.err.println("Problem playing audio: " + ex);
        }
    }

    protected void createPlayer() {
        try {
            player = new AdvancedPlayer(new FileInputStream(file));
            // 这里设置一个监听器，来监听停止事件
            player.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent event) {
                    // 当播放完毕后,会触发该事件,再次调用start()即可!
                    start();
                }
            });
        } catch (JavaLayerException | IOException e) {
            e.printStackTrace();
        }
    }
}
