package me.theminecoder.dynamicservers;

import me.theminecoder.dynamicservers.data.ServerData;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;

public final class DynamicServersSpigot extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        DynamicServersCore.boot(
                this.getConfig().getString("redis.hostname"),
                this.getConfig().getInt("redis.port"),
                this.getConfig().getString("redis.password"),
                this.getLogger()
        );

        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try (Jedis jedis = DynamicServersCore.getJedisPool().getResource()) {
                jedis.publish(DynamicServersCore.REDIS_CHANNEL, DynamicServersCore.getGSON().toJson(new ServerData(
                        this.getConfig().getString("server-id"),
                        this.getServer().getIp() + ":" + this.getServer().getPort(),
                        this.getServer().getOnlinePlayers().size(),
                        this.getServer().getMaxPlayers()
                )));
            }
        }, 0, 60);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
