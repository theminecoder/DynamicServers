package me.theminecoder.dynamicservers.data;

/**
 * @author theminecoder
 * @version 1.0
 */
public class ServerData {

    private String id;
    private String ip;

    private int players;
    private int maxPlayers;

    public ServerData(String id, String ip, int players, int maxPlayers) {
        this.id = id;
        this.ip = ip;
        this.players = players;
        this.maxPlayers = maxPlayers;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPlayers() {
        return players;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
