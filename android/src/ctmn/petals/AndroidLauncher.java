package ctmn.petals;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctmn.petals.utils.SaveCrypto.INSTANCE.setCipher(new AesSaveCipher());
		ctmn.petals.menu.MultiplayerMenuKt.enableNetworking();
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(new PetalsGame(), config);
	}
}
