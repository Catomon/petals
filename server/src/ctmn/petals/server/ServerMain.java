package ctmn.petals.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Standalone dedicated server entry point.
 *
 * Run with the Gradle module:
 *   .\gradlew.bat :server:runServer --args="--port 7396"
 * or a bot vs bot smoke test:
 *   .\gradlew.bat :server:runServer --args="botmatch normal"
 * or the packaged jar:
 *   java -jar server/build/libs/server-&lt;version&gt;.jar --port 7396
 */
public class ServerMain {
    public static void main(String[] args) {
        ServerConfig config = ServerConfig.Companion.fromArgs(args);

        HeadlessApplicationConfiguration headless = new HeadlessApplicationConfiguration();
        headless.updatesPerSecond = 30;

        new HeadlessApplication(new ServerApp(config), headless);
    }
}
