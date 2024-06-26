package de.jeff_media.angelchest.listeners;

import de.jeff_media.angelchest.AngelChestMain;
import de.jeff_media.angelchest.EmergencyMode;
import de.jeff_media.angelchest.config.Messages;
import com.jeff_media.jefflib.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class EmergencyListener implements Listener {

    @SuppressWarnings("unused")
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent playerJoinEvent) {

        if (AngelChestMain.getInstance().invalidConfigFiles == null) return;

        if (playerJoinEvent.getPlayer().isOp()) {
            int i = 0;
            for (final String file : AngelChestMain.getInstance().invalidConfigFiles) {
                final String[] text = EmergencyMode.BROKEN_CONFIG_FILE.clone();
                i++;
                Bukkit.getScheduler().scheduleSyncDelayedTask(AngelChestMain.getInstance(), () -> {
                    for (int j = 0; j < text.length; j++) {
                        text[j] = text[j].replace("{filename}", file);
                    }
                    Messages.send(playerJoinEvent.getPlayer(), text);
                }, Ticks.fromSeconds(0.5) + i);
            }
        }
    }

}
