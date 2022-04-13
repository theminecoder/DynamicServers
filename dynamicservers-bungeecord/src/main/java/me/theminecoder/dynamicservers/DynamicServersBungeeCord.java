package me.theminecoder.dynamicservers;

import me.theminecoder.dynamicservers.data.ServerData;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class DynamicServersBungeeCord extends Plugin implements Listener {

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");


        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        DynamicServersCore.boot(config.getString("redis.hostname"), config.getInt("redis.port"),
                config.getString("redis.password"), this.getLogger());
        this.getProxy().getScheduler().schedule(this, () -> {
            Map<String, ServerData> backendServers = DynamicServersCore.getServerCache();
            Map<String, ServerInfo> currentServers = this.getProxy().getServers();

            for (Iterator<Map.Entry<String, ServerInfo>> currentServerIterator = currentServers.entrySet().iterator(); currentServerIterator.hasNext(); ) {
                Map.Entry<String, ServerInfo> currentServerEntry = currentServerIterator.next();
                if (backendServers.containsKey(currentServerEntry.getKey())) {
                    if (!backendServers.get(currentServerEntry.getKey()).getIp().equalsIgnoreCase(
                            currentServerEntry.getValue().getAddress().getAddress().getHostAddress() + ":" + currentServerEntry.getValue().getAddress().getPort()
                    )) {
                        //noinspection deprecation
                        currentServerEntry.getValue().getPlayers().forEach(player ->
                                player.disconnect("That server has changed IP while online, duplicate server?"));
                        String[] newIp = backendServers.get(currentServerEntry.getKey()).getIp().split(":");
                        ServerInfo newServer = this.getProxy().constructServerInfo(currentServerEntry.getKey(),
                                new InetSocketAddress(newIp[0], Integer.parseInt(newIp[1])), "", false);
                        this.getLogger().warning("Server IP for \"" + currentServerEntry.getKey() + "\" updated!");
                        currentServerEntry.setValue(newServer);
                    }
                } else {
                    //noinspection deprecation
                    currentServerEntry.getValue().getPlayers().forEach(player ->
                            player.disconnect("That server has timed out, broken connection?"));
                    currentServerIterator.remove();
                    this.getLogger().info("Removed "+currentServerEntry.getValue().getName()+" due to timeout...");
                }
            }

            backendServers.entrySet().stream().filter(serverEntry -> !currentServers.containsKey(serverEntry.getKey())).forEach(serverEntry -> {
                String[] newIp = backendServers.get(serverEntry.getKey()).getIp().split(":");
                ServerInfo newServer = this.getProxy().constructServerInfo(serverEntry.getKey(),
                        new InetSocketAddress(newIp[0], Integer.parseInt(newIp[1])), "", false);
                currentServers.put(serverEntry.getKey(), newServer);
                this.getLogger().info("Added "+serverEntry.getKey()+" ("+newIp[0]+":"+newIp[1]+")");
            });
        }, 0, 5, TimeUnit.SECONDS);
        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        event.getResponse().getPlayers().setMax(DynamicServersCore.getServerCache().values().stream()
                .mapToInt(ServerData::getMaxPlayers).sum());
    }
}
