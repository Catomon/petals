package ctmn.petals.desktop;

import ctmn.petals.Config;
import ctmn.petals.DiscordPresence;
import ctmn.petals.Rich;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

/** Discord rich presence for the desktop build. */
public class DesktopDiscordPresence implements DiscordPresence {

    private final boolean disabled = Config.DISABLE_RICH;

    @Override
    public void start() {
        if (disabled) return;

        DiscordRPC.discordInitialize("1209175690793066516", new DiscordEventHandlers(), true);
    }

    @Override
    public void stop() {
        if (disabled) return;

        DiscordRPC.discordShutdown();
    }

    @Override
    public void update(Rich rich) {
        if (disabled) return;

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.largeImageKey = "big_icon";
        presence.largeImageText = Config.APP_NAME;

        if (rich == Rich.EDITOR) {
            presence.smallImageKey = "small_icon";
            presence.smallImageText = "Petals";
            presence.details = "In Map Editor";
        } else if (rich == Rich.PLAYING) {
            presence.smallImageKey = "small_icon";
            presence.smallImageText = "Petals";
            presence.details = "In Game";
        }

        DiscordRPC.discordUpdatePresence(presence);
    }
}
