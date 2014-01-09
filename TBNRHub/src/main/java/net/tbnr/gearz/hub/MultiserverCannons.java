package net.tbnr.gearz.hub;

import net.tbnr.gearz.Gearz;
import net.tbnr.gearz.netcommand.BouncyUtils;
import net.tbnr.gearz.packets.packetwrapper.WrapperPlayServerWorldParticles;
import net.tbnr.gearz.server.Server;
import net.tbnr.gearz.server.ServerManager;
import net.tbnr.util.TPlugin;
import net.tbnr.util.command.TCommand;
import net.tbnr.util.command.TCommandHandler;
import net.tbnr.util.command.TCommandSender;
import net.tbnr.util.command.TCommandStatus;
import net.tbnr.util.player.TPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Joey
 * Date: 8/30/13
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("ALL")
public class MultiserverCannons implements Listener, TCommandHandler {
    private HashSet<TPlayer> actives = new HashSet<>();
    private HashMap<Location, MultiserverCannon> cannons = new HashMap<>();

    public MultiserverCannons() {
        List pads = TBNRHub.getInstance().getConfig().getList("pads");
        if (pads == null) return;
        for (Object cannon : pads) {
            if (!(cannon instanceof MultiserverCannon)) continue;
            this.cannons.put(((MultiserverCannon) cannon).getRefrenceBlock(), (MultiserverCannon) cannon);
            TBNRHub.getInstance().registerEvents((MultiserverCannon) cannon);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        MultiserverCannon cannon = getCannon(event.getPlayer().getLocation().getBlock().getLocation());
        if (cannon == null) return;
        TPlayer player = TBNRHub.getInstance().getPlayerManager().getPlayer(event.getPlayer());
        if (actives.contains(player)) return;
        event.setCancelled(true);
        cannon.connecting(player);
        player.playSound(Sound.FUSE);
        try {
            player.playParticleEffect(new TPlayer.TParticleEffect(player.getPlayer().getLocation(), Gearz.getRandom().nextFloat(), 1, 35, 35, WrapperPlayServerWorldParticles.ParticleEffect.LARGE_SMOKE));
        } catch (Exception e) {
            e.printStackTrace();
        }
        MultiserverCannonProcess multiserverCannonProcess = new MultiserverCannonProcess(player, cannon);
        TBNRHub.getInstance().registerEvents(multiserverCannonProcess);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TBNRHub.getInstance(), multiserverCannonProcess, 35);
        this.actives.add(player);
        event.setCancelled(true);
    }

    @TCommand(
            name = "setcannon",
            usage = "/setcannon <name>",
            permission = "gearz.setcannon",
            senders = {TCommandSender.Player})
    public TCommandStatus setCannon(org.bukkit.command.CommandSender sender, TCommandSender type, TCommand meta, Command command, String[] args) {
        if (args.length < 1) return TCommandStatus.FEW_ARGS;
        if (args.length > 1) return TCommandStatus.MANY_ARGS;
        Player player = (Player) sender;
        Block pressure = player.getLocation().getBlock();
        Block coal = pressure.getRelative(BlockFace.DOWN);
        Block wool = coal.getRelative(BlockFace.DOWN);
        if (!(pressure.getType() == Material.STONE_PLATE && coal.getType() == Material.COAL_BLOCK && wool.getType() == Material.WOOL))
            return TCommandStatus.INVALID_ARGS;
        MultiserverCannon existing = getCannon(pressure.getLocation());
        MultiserverCannon cannon = new MultiserverCannon(args[0], TPlugin.encodeLocationString(pressure.getLocation()), TPlugin.encodeLocationString(player.getLocation()));
        List pads = TBNRHub.getInstance().getConfig().getList("pads");
        if (pads == null) {
            pads = new ArrayList();
        }
        if (existing != null) {
            player.sendMessage(ChatColor.RED + "This pad already exists! Updating it :D");
            pads.remove(existing);
        }
        pads.add(cannon);
        cannons.put(cannon.getRefrenceBlock(), cannon);
        TBNRHub.getInstance().getConfig().set("pads", pads);
        TBNRHub.getInstance().saveConfig();
        player.sendMessage(ChatColor.GREEN + "Setup a pad for " + args[0]);
        return TCommandStatus.SUCCESSFUL;
    }

    @TCommand(
            name = "delcannon",
            usage = "/delcannon",
            permission = "gearz.delcannon",
            senders = {TCommandSender.Player})
    public TCommandStatus delCannon(org.bukkit.command.CommandSender sender, TCommandSender type, TCommand meta, Command command, String[] args) {
        Player player = (Player) sender;
        MultiserverCannon existing = getCannon(player.getLocation().getBlock().getLocation());
        if (existing == null) {
            return TCommandStatus.INVALID_ARGS;
        }
        List pads = TBNRHub.getInstance().getConfig().getList("pads");
        if (!pads.contains(existing)) {
            return TCommandStatus.INVALID_ARGS;
        }
        pads.remove(existing);
        TBNRHub.getInstance().getConfig().set("pads", pads);
        TBNRHub.getInstance().saveConfig();
        existing.removeLabelAll();
        player.sendMessage(ChatColor.GREEN + "Removed a pad for " + existing.getServer());
        this.cannons.remove(existing.getRefrenceBlock());
        return TCommandStatus.SUCCESSFUL;
    }

    private MultiserverCannon getCannon(Location location) {
        /*List pads = TBNRHub.getInstance().getConfig().getList("pads");
        if (pads == null) {
            return null;
        }
        for (Object cannon : pads) {
            if (!(cannon instanceof MultiserverCannon)) continue;
            if (((MultiserverCannon)cannon).getRefrenceBlock().equals(location)) return (MultiserverCannon)cannon;
        }
        return null;*/
        return this.cannons.containsKey(location) ? this.cannons.get(location) : null;
    }

    @Override
    public void handleCommandStatus(TCommandStatus status, org.bukkit.command.CommandSender sender, TCommandSender senderType) {
        TBNRHub.handleCommandStatus(status, sender);
    }

    public static enum ProcessState {
        PRE_IGNITE,
        IGNITE,
        PROPEL,
        SEND,
        SENT
    }

    public static class MultiserverCannonProcess extends BukkitRunnable implements Listener {
        private TPlayer player;
        private MultiserverCannon cannon;
        private ProcessState state;
        private int propell_ticks = 0;
        private float pitch = 0;
        private float yaw = 0;

        public MultiserverCannonProcess(TPlayer player, MultiserverCannon cannon) {
            this.player = player;
            this.cannon = cannon;
            this.state = ProcessState.PRE_IGNITE;
        }

        @Override
        public void run() {
            if (this.state == ProcessState.PRE_IGNITE) this.state = ProcessState.IGNITE;
            switch (this.state) {
                case IGNITE:
                    this.player.playSound(Sound.EXPLODE);
                    try {
                        player.playParticleEffect(new TPlayer.TParticleEffect(player.getPlayer().getLocation(), Gearz.getRandom().nextFloat(), 1, 15, 10, WrapperPlayServerWorldParticles.ParticleEffect.EXPLODE));
                    } catch (Exception ignored) {
                    }
                    this.player.playSound(Sound.PORTAL_TRAVEL);
                    this.player.getPlayer().teleport(this.cannon.getRefrenceLook());
                    this.player.getPlayer().setVelocity(this.player.getPlayer().getLocation().getDirection().multiply(1.9).add(new Vector(0, 0.5, 0)));
                    this.state = ProcessState.PROPEL;
                    this.pitch = this.player.getPlayer().getLocation().getPitch();
                    this.yaw = this.player.getPlayer().getLocation().getYaw();
                    reregister(1);
                    break;
                case PROPEL:
                    if (this.propell_ticks > 65) this.state = ProcessState.SEND;
                    this.propell_ticks++;
                    reregister(1);
                    /*try {
                        player.playParticleEffect(new TPlayer.TParticleEffect(player.getPlayer().getLocation(), new Location(player.getPlayer().getWorld(), 1, 1, 1), 25, 35, TPlayer.TParticleEffectType.LARGE_SMOKE));
                        for (TPlayer tPlayer : TPlayerManager.getInstance().getPlayers()) {
                            if (!tPlayer.getPlayer().canSee(player.getPlayer())) continue;
                            if (tPlayer.getPlayer().getLocation().distance(player.getPlayer().getLocation()) > 35) continue;
                            tPlayer.playParticleEffect(new TPlayer.TParticleEffect(player.getPlayer().getLocation(), new Location(player.getPlayer().getWorld(), 1, 1, 1), 15, 35, TPlayer.TParticleEffectType.SMOKE));
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }*/
                    this.player.getPlayer().setVelocity(this.player.getPlayer().getLocation().getDirection().multiply(1.9).add(new Vector(0, 0.5, 0)));
                    break;
                case SEND:
                    this.state = ProcessState.SENT;
                    String serverFor = getServerFor(cannon.getServer(), false);
                    if (serverFor == null) serverFor = getServerFor(cannon.getServer(), true);
                    if (serverFor == null) serverFor = cannon.getServer();
                    BouncyUtils.sendPlayerToServer(this.player.getPlayer(), serverFor);
                    this.player.getPlayer().teleport(TBNRHub.getInstance().getSpawn().getSpawn());
                    try {
                        this.player.playParticleEffect(new TPlayer.TParticleEffect(player.getPlayer().getLocation(), Gearz.getRandom().nextFloat(), 1, 10, 2, WrapperPlayServerWorldParticles.ParticleEffect.HEART));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    TBNRHub.getInstance().getCannon().actives.remove(player);
                    break;
            }
        }

        private String getServerFor(String game, boolean allowFulls) {
            List<net.tbnr.gearz.server.Server> server = ServerManager.getServersWithGame(game);
            if (server == null) return game;
            for (Server s : server) {
                if (!s.getStatusString().equals("lobby")) continue;
                if (!s.isCanJoin()) continue;
                if (s.getMaximumPlayers() == s.getPlayerCount() && allowFulls) continue;
                return s.getBungee_name();
            }
            return null;
        }

        private void reregister(int time) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(TBNRHub.getInstance(), this, time);
        }

        @EventHandler
        public void onMove(PlayerMoveEvent event) {
            if (this.state == ProcessState.SENT || this.state == ProcessState.PRE_IGNITE) return;
            if (!event.getPlayer().equals(this.player.getPlayer())) return;
            if (event.getTo().getPitch() == this.pitch && event.getTo().getYaw() == this.yaw) return;
            Location newTo = event.getTo();
            newTo.setPitch(this.pitch);
            newTo.setYaw(this.yaw);
            event.getPlayer().teleport(newTo);
        }
    }
}