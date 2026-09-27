package ctmn.petals.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import ctmn.petals.Config;
import ctmn.petals.GamePref;
import ctmn.petals.PetalsGame;

public class DesktopLauncher {
    public static void main(String[] arg) {

        boolean packAndExit = arg.length == 1 && arg[0].equals("packTextures");

        boolean windowed = arg.length == 1 && arg[0].equals("windowed");

        boolean botTest = false;
        boolean botLog = false;
        boolean verboseLog = false;
        boolean dedicatedServer = false;
        boolean onlineTest = false;
        String onlineTestHost = "localhost";
        int onlineTestPort = Config.SERVER_PORT;
        String botDifficulty = "normal";
        for (String a : arg) {
            String lower = a.toLowerCase();
            if (lower.equals("botvsbot") || lower.equals("bottest") || lower.equals("--bot-test")) {
                botTest = true;
            } else if (lower.equals("onlinetest")) {
                onlineTest = true;
            } else if (lower.equals("dedicatedserver") || lower.equals("--dedicated-server")) {
                dedicatedServer = true;
            } else if (lower.equals("easy") || lower.equals("normal") || lower.equals("hard") || lower.equals("insane")) {
                botDifficulty = lower;
            } else if (lower.equals("botlog") || lower.equals("--bot-log")) {
                botLog = true;
            } else if (lower.equals("verboselog") || lower.equals("--verbose-log")) {
                verboseLog = true;
            }
        }

        // "onlinetest" may be followed by a host and a port, so read those with an index.
        if (onlineTest) {
            for (int i = 0; i < arg.length; i++) {
                if (!arg[i].toLowerCase().equals("onlinetest")) continue;

                if (i + 1 < arg.length && !arg[i + 1].startsWith("--")) {
                    onlineTestHost = arg[i + 1];
                }
                if (i + 2 < arg.length) {
                    try {
                        onlineTestPort = Integer.parseInt(arg[i + 2]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        Config.INSTANCE.setBotLog(botLog);
        Config.INSTANCE.setVerboseLog(verboseLog);
        Config.INSTANCE.setOnlineTest(onlineTest);
        Config.INSTANCE.setOnlineTestHost(onlineTestHost);
        Config.INSTANCE.setOnlineTestPort(onlineTestPort);

        if (botTest) {
            Config.INSTANCE.setBotTest(true);
            Config.INSTANCE.setBotTestDifficulty(botDifficulty);
        }

        if (packAndExit) {
            RunTexturePacker.pack();
            return;
        }

        if (dedicatedServer) {
            ctmn.petals.server.ServerConfig serverConfig = ctmn.petals.server.ServerConfig.Companion.fromArgs(arg);
            com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration headless =
                new com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration();
            headless.updatesPerSecond = 30;
            new com.badlogic.gdx.backends.headless.HeadlessApplication(
                new ctmn.petals.server.ServerApp(serverConfig), headless);
            return;
        }

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.preferencesDirectory = "Documents/Fae's Feisty";
        config.foregroundFPS = 60;
        config.backgroundFPS = 30;
        config.vSyncEnabled = false;
        config.title = Config.APP_NAME + " (" + Config.APP_VER_NAME + ")";
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
