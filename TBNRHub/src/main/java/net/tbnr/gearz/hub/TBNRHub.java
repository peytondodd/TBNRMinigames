package net.tbnr.gearz.hub;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Protocol;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.mongodb.DBObject;
import lombok.Getter;
import net.lingala.zip4j.exception.ZipException;
import net.tbnr.gearz.Gearz;
import net.tbnr.gearz.GearzException;
import net.tbnr.gearz.arena.ArenaManager;
import net.tbnr.gearz.hub.items.warpstar.WarpStarCommands;
import net.tbnr.gearz.server.Server;
import net.tbnr.gearz.server.ServerManager;
import net.tbnr.util.TPlugin;
import net.tbnr.util.command.TCommand;
import net.tbnr.util.command.TCommandHandler;
import net.tbnr.util.command.TCommandSender;
import net.tbnr.util.command.TCommandStatus;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Joey
 * Date: 8/30/13
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class TBNRHub extends TPlugin implements TCommandHandler {
    private MultiserverCannons cannon;
    private Spawn spawnHandler;
    private static TBNRHub instance;
    @Getter
    private HubItems hubItems;
    @Getter private HubArena arena;

    public TBNRHub() {
        ConfigurationSerialization.registerClass(MultiserverCannon.class);
    }

    public static TBNRHub getInstance() {
        return instance;
    }

    @Override
    public void enable() {
	    TBNRHub.instance = this;
	    Gearz.getInstance().setLobbyServer(true);
        DBObject hub_arena = getMongoDB().getCollection("hub_arena").findOne();
        if (hub_arena != null) {
            try {
                arena = (HubArena)ArenaManager.arenaFromDBObject(HubArena.class, hub_arena);
                arena.loadWorld();
            } catch (GearzException | ClassCastException | ZipException | IOException e) {
                e.printStackTrace();
            }
        }

	    cannon = new MultiserverCannons();
	    spawnHandler = new Spawn();
	    hubItems = new HubItems("net.tbnr.gearz.hub.items");

	    SignEdit signedit = new SignEdit();

	    TCommandHandler[] commandHandlers2Register = {
			    this,
			    cannon,
			    spawnHandler,
			    signedit,
			    new WarpStarCommands()
	    };

	    Listener[] listeners2Register = {
			    spawnHandler,
			    cannon,
			    new ColoredSigns(),
			    new BouncyPads(),
			    new LoginMessages(),
			    new SnowballEXP(),
			    new Restrictions(),
			    new PlayerThings(),
			    new BlastOffSigns(),
			    hubItems,
			    signedit
	    };

	    //Register all commands
	    for(TCommandHandler commandHandler : commandHandlers2Register) registerCommands(commandHandler);

	    //Register all events
	    for(Listener listener : listeners2Register) registerEvents(listener);

	    new SaveAllTask().runTaskTimer(this, 0, 12000);

        ServerManager.setGame("lobby");
        ServerManager.setStatusString("HUB_DEFAULT");
        ServerManager.setOpenForJoining(true);
        Server thisServer = ServerManager.getThisServer();
        try {
            thisServer.setAddress(Gearz.getExternalIP());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        thisServer.setPort(Bukkit.getPort());
        thisServer.save();

	    addHoverPingText();
    }

    public MultiserverCannons getCannon() {
        return cannon;
    }

    public Spawn getSpawn() {
        return this.spawnHandler;
    }

    @Override
    public void disable() {
    }

    @Override
    public String getStorablePrefix() {
        return "ghub";
    }

    public String getChatPrefix() {
        return getFormat("prefix");
    }

    /*
    @TCommand(
            name = "test",
            usage = "Test command!",
            permission = "gearz.test",
            senders = {TCommandSender.Player, TCommandSender.Console}
    )
    public TCommandStatus test(CommandSender sender, TCommandSender type, TCommand meta, Command command, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Gearz Engine test!");
        return TCommandStatus.SUCCESSFUL;
    }
     */
    @SuppressWarnings("unused")
    public static void handleCommandStatus(TCommandStatus status, CommandSender sender) {
        if (status == TCommandStatus.SUCCESSFUL) return;
        sender.sendMessage(getInstance().getFormat("formats.command-status", true, new String[]{"<status>", status.toString()}));
    }

    @Override
    public void handleCommandStatus(TCommandStatus status, CommandSender sender, TCommandSender senderType) {
        handleCommandStatus(status, sender);
    }

    public static class SaveAllTask extends BukkitRunnable {
        @Override
        public void run() {
            for (World world : Bukkit.getServer().getWorlds()) world.save();

            Bukkit.broadcast(ChatColor.GREEN + "World saved!", "gearz.notifysave");
            TBNRHub.getInstance().getLogger().info("Saved the world.");
        }
    }

    @SuppressWarnings("unused")
    @TCommand(name = "head", permission = "gearz.head", senders = {TCommandSender.Player}, usage = "/head <name>")
    public TCommandStatus head(CommandSender sender, TCommandSender type, TCommand meta, Command command, String[] args) {
	    ItemStack stack;
	    ItemMeta itemMeta;
        for (String s : args) {
            stack = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
            itemMeta = stack.getItemMeta();
            assert itemMeta instanceof SkullMeta;
            SkullMeta m = (SkullMeta) itemMeta;

            m.setOwner(s);
            stack.setItemMeta(m);
            ((Player) sender).getInventory().addItem(stack);
        }
        return TCommandStatus.SUCCESSFUL;
    }

    public String compile(String[] args, int min, int max) {
        StringBuilder builder = new StringBuilder();

        for (int i = min; i < args.length; i++) {
            builder.append(args[i]);
            if (i == max) return builder.toString();
            builder.append(" ");
        }
        return builder.toString();
    }

	public void addHoverPingText() {
		getLogger().info("added hover ping text");
		ProtocolLibrary.getProtocolManager().addPacketListener(
				new PacketAdapter(this, ListenerPriority.NORMAL,
						Arrays.asList(PacketType.Status.Server.OUT_SERVER_INFO), ListenerOptions.ASYNC) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						getLogger().info("packet recievied");
						handlePing(event.getPacket().getServerPings().read(0));
					}
				}
		);
	}

	public void handlePing(WrappedServerPing ping) {
		ArrayList<WrappedGameProfile> wrappedGameProfileArrayList = new ArrayList<>();

		int i = 0;
		for(String string : getConfig().getStringList("hover-ping-text")) {
			getLogger().info(string);
			wrappedGameProfileArrayList.add(new WrappedGameProfile("id"+i, ChatColor.translateAlternateColorCodes('&', string)));
			i++;
		}

		ping.setPlayers(wrappedGameProfileArrayList);
	}
}
