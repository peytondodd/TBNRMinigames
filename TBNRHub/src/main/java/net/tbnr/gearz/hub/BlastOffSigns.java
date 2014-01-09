package net.tbnr.gearz.hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.tbnr.gearz.netcommand.BouncyUtils;
import net.tbnr.gearz.server.Server;
import net.tbnr.gearz.server.ServerManager;
import net.tbnr.util.InventoryGUI;
import net.tbnr.util.ServerSelector;
import net.tbnr.util.player.TPlayer;
import net.tbnr.util.player.TPlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jake on 12/27/13.
 */
public class BlastOffSigns implements Listener {
    final private Map<TPlayer, SignData> inUse = new HashMap<>();

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN && block.getType() != Material.WALL_SIGN) return;
        Sign sign = (Sign) block.getState();
        final String[] lines = sign.getLines();
        if (lines[0] == null || lines[1] == null || ServerManager.getServersWithGame(lines[1]).size() == 0 || !ChatColor.stripColor(lines[0]).equals(ChatColor.stripColor(TBNRHub.getInstance().getFormat("formats.blastoff-topline", true))))
            return;
        final ServerSelector serverSelector = new ServerSelector(lines[1], new ServerSelector.SelectorCallback() {
            @Override
            public void onItemSelect(ServerSelector selector, InventoryGUI.InventoryGUIItem item, Player player) {
                Server server = selector.getServers().get(item.getSlot());
                if (server.isCanJoin()) {
                    selector.close(player);
                    SignData signData = new SignData(server, player.getLocation().getBlockY());
                    inUse.put(TPlayerManager.getInstance().getPlayer(player), signData);
                    player.setVelocity(new Vector(0, 4, 0));

                }
            }

            @Override
            public void onSelectorOpen(ServerSelector selector, Player player) {
            }

            @Override
            public void onSelectorClose(ServerSelector selector, Player player) {
            }
        });
        serverSelector.open(event.getPlayer());
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TPlayer tPlayer = TPlayerManager.getInstance().getPlayer(player);
        if (!inUse.containsKey(tPlayer)) return;
        Integer y = event.getTo().getBlockY();
        SignData signData = inUse.get(tPlayer);
        /**
         * This handles if a player hit a block before reaching the distance
         */
        if (event.getTo().getBlock().getRelative(BlockFace.UP).getType() == Material.AIR) {
            BouncyUtils.sendPlayerToServer(player, signData.getServer().getBungee_name());
            inUse.remove(tPlayer);
            return;
        }
        if (y - signData.getStart() >= TBNRHub.getInstance().getConfig().getInt("blastoff.distance")) {
            BouncyUtils.sendPlayerToServer(player, signData.getServer().getBungee_name());
            inUse.remove(tPlayer);
        }

    }

    @AllArgsConstructor
    @Data
    @EqualsAndHashCode
    public class SignData {
        private Server server;
        private Integer start;
    }
}