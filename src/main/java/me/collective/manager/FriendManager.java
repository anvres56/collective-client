package me.collective.manager;

import java.util.HashSet;
import java.util.Set;

public class FriendManager {
    public static final FriendManager INSTANCE = new FriendManager();
    private final Set<String> friends = new HashSet<>();

    private FriendManager() {
    }

    public static FriendManager getInstance() {
        return INSTANCE;
    }

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public boolean isFriend(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) return false;
        return isFriend(player.getName().getString());
    }

    public void addFriend(String name) {
        friends.add(name.toLowerCase());
    }

    public void removeFriend(String name) {
        friends.remove(name.toLowerCase());
    }

    public Set<String> getFriends() {
        return friends;
    }
}
