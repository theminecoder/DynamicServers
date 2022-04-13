package me.theminecoder.dynamicservers;

import me.theminecoder.dynamicservers.data.ServerData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

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

        String detectedServerId = new File(".").getAbsoluteFile().getName();
        try {
            Properties serverProperties = new Properties();
            serverProperties.load(new FileReader("server.properties"));
            if(serverProperties.containsKey("server-name")) detectedServerId = serverProperties.getProperty("server-name");
            if(serverProperties.containsKey("server-id")) detectedServerId = serverProperties.getProperty("server-id");
        } catch (Exception ignored) {
        }

        String finalDetectedServerId = detectedServerId;
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try (Jedis jedis = DynamicServersCore.getJedisPool().getResource()) {
                jedis.publish(DynamicServersCore.REDIS_CHANNEL, DynamicServersCore.getGSON().toJson(new ServerData(
                        this.getConfig().getString("server-id", finalDetectedServerId),
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
