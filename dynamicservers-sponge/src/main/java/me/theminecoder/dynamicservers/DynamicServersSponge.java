package me.theminecoder.dynamicservers;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import me.theminecoder.dynamicservers.data.ServerData;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.net.InetSocketAddress;

@Plugin(
        id = "dynamicservers",
        name = "DynamicServers"
)
public class DynamicServersSponge {

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        File configFile = new File("config/dynamicservers.conf");
        CommentedConfigurationNode config;
        try {
            if (!configFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                configFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                configFile.createNewFile();
                try (InputStream in = DynamicServersSponge.class.getResourceAsStream("/config.conf");
                     OutputStream out = new FileOutputStream(configFile)) {
                    ByteStreams.copy(in, out);
                }
            }
            config = configManager.load();
        } catch (IOException e) {
            logger.error("Error loading config", e);
            return;
        }

        DynamicServersCore.boot(config.getNode("redis", "hostname").getString(), config.getNode("redis", "port").getInt(),
                config.getNode("redis", "password").getString(), java.util.logging.Logger.getLogger("DynamicServers"));
        Sponge.getScheduler().createTaskBuilder().async().intervalTicks(60).execute(() -> {
            InetSocketAddress address = Sponge.getServer().getBoundAddress().orElseThrow(() ->
                    new IllegalStateException("Bound Address not available, server not yet bound?"));
            try (Jedis jedis = DynamicServersCore.getJedisPool().getResource()) {
                jedis.publish(DynamicServersCore.REDIS_CHANNEL, DynamicServersCore.getGSON().toJson(new ServerData(
                        config.getNode("server-id").getString("server"),
                        address.getAddress().getHostAddress() + ":" + address.getPort(),
                        Sponge.getServer().getOnlinePlayers().size(),
                        Sponge.getServer().getMaxPlayers()
                )));
            }
        }).name("Dynamic Server Config Sender").submit(this);
    }

}
