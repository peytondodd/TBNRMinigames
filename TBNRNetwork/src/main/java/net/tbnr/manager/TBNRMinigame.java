package net.tbnr.manager;

import net.tbnr.gearz.GearzPlugin;
import net.tbnr.gearz.arena.Arena;
import net.tbnr.gearz.game.GameMeta;
import net.tbnr.gearz.game.GearzGame;
import net.tbnr.gearz.game.GearzPlayerProvider;

import java.util.HashMap;
import java.util.List;

public abstract class TBNRMinigame extends GearzGame<TBNRPlayer> {

    private HashMap<TBNRPlayer, Integer> pendingPoints;

    /**
     * New game in this arena
     *
     * @param players        The players in this game
     * @param arena          The Arena that the game is in.
     * @param plugin         The plugin that handles this Game.
     * @param meta           The meta of the game.
     * @param id
     * @param playerProvider
     */
    public TBNRMinigame(List<TBNRPlayer> players, Arena arena, GearzPlugin<TBNRPlayer> plugin, GameMeta meta, Integer id, GearzPlayerProvider<TBNRPlayer> playerProvider) {
        super(players, arena, plugin, meta, id, playerProvider);
    }

    protected void addGPoints(TBNRPlayer player, Integer points) {
        this.pendingPoints.put(player, (this.pendingPoints.containsKey(player) ? this.pendingPoints.get(player) : 0) + points);
        player.getTPlayer().sendMessage(getFormat("points-added", new String[]{"<points>", String.valueOf(points)}));
    }

    protected abstract int xpForPlaying();
}
