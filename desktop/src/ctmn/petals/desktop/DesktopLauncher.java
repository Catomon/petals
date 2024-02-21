package ctmn.petals.desktop;

import ctmn.petals.Const;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import ctmn.petals.PetalsGame;

public class DesktopLauncher {
	public static void main (String[] arg) {

		if (!Const.IS_RELEASE && false)
			RunTexturePacker.pack();
		else System.out.println(DesktopLauncher.class.getSimpleName() + " RunTexturePacker.pack() canceled");

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.preferencesDirectory = ".prefs/";
		config.foregroundFPS = 120;
		//config.backgroundFPS = 30;
		//config.vSyncEnabled = false;
		config.title = Const.APP_NAME + " (" + Const.APP_VER_NAME + ")";
		if (Const.IS_RELEASE)
			config.width = 854;
		else
			config.width = 480;
		config.height = 600;
		config.addIcon("libgdx128.png", Files.FileType.Internal);
		config.addIcon("libgdx64.png", Files.FileType.Internal);
		config.addIcon("libgdx32.png", Files.FileType.Internal);
		//config.addIcon("libgdx16.png", Files.FileType.Internal);

		new LwjglApplication(new PetalsGame(), config);
	}
}
