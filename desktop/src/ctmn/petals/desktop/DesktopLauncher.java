package ctmn.petals.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import ctmn.petals.Const;
import ctmn.petals.GamePref;
import ctmn.petals.PetalsGame;

public class DesktopLauncher {
    public static void main(String[] arg) {

        boolean packAndExit = arg.length == 1 && arg[0].equals("packTextures");

        boolean windowed = arg.length == 1 && arg[0].equals("windowed");

        if (packAndExit) {
            RunTexturePacker.pack();
            return;
        }

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.preferencesDirectory = "Documents/Fae's Feisty Fray";
        config.foregroundFPS = 60;
        config.backgroundFPS = 30;
        config.vSyncEnabled = true;
        config.title = Const.APP_NAME + " (" + Const.APP_VER_NAME + ")";
        config.width = 854;
        config.height = 480;
        config.pauseWhenBackground = false;
        config.pauseWhenMinimized = false;
        config.addIcon("libgdx128.png", Files.FileType.Internal);
        config.addIcon("libgdx64.png", Files.FileType.Internal);
        config.addIcon("libgdx32.png", Files.FileType.Internal);
        //config.addIcon("libgdx16.png", Files.FileType.Internal);

        new LwjglApplication(new PetalsGame(RunTexturePacker::pack, () -> {
            if (windowed) {
                GamePref.INSTANCE.setFullscreen(false);
            }
        }), config);
    }
}
