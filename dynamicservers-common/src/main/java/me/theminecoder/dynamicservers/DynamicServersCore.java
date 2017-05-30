package me.theminecoder.dynamicservers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.theminecoder.dynamicservers.data.ServerData;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author theminecoder
 * @version 1.0
 */
public class DynamicServersCore {

    public static final String REDIS_CHANNEL = "dynamic-server";

    private static JedisPool JEDIS_POOL;
    private static Gson GSON;

    private static Cache<String, ServerData> SERVER_CACHE = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    static void boot(String hostname, int port, String password, Logger log) {
        if (JEDIS_POOL != null) {
            return;
        }
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(DynamicServersCore.class.getClassLoader());
        if(password!=null && password.trim().length()>0) {
            JEDIS_POOL = new JedisPool(new GenericObjectPoolConfig(), hostname, port, 5000, password);
        } else {
            JEDIS_POOL = new JedisPool(new GenericObjectPoolConfig(), hostname, port, 5000);
        }
        Thread.currentThread().setContextClassLoader(previous);
        GSON = new GsonBuilder().create();
        Thread listenerThread = new Thread(() -> {
            while (true) {
                try {
                    try (Jedis jedis = JEDIS_POOL.getResource()) {
                        jedis.subscribe(new JedisPubSub() {
                            @Override
                            public void onMessage(String channel, String message) {
                                ServerData data = GSON.fromJson(message, ServerData.class);
                                SERVER_CACHE.put(data.getId(), data);
                            }

                            @Override
                            public void onSubscribe(String channel, int subscribedChannels) {
                                log.info("Dynamic Server System Started!");
                            }

                            @Override
                            public void onUnsubscribe(String channel, int subscribedChannels) {
                                log.info("Dynamic Server System Stopped!");
                            }
                        }, REDIS_CHANNEL);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); //pls no crash thread :(
                }
            }
        });
        listenerThread.setName("Dynamic Server Config Receiver");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static JedisPool getJedisPool() {
        return JEDIS_POOL;
    }

    public static Gson getGSON() {
        return GSON;
    }

    public static Map<String, ServerData> getServerCache() {
        return SERVER_CACHE.asMap();
    }
}
