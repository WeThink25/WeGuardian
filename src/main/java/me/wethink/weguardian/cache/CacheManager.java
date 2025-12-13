package me.wethink.weguardian.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class CacheManager {

    private final Cache<UUID, Optional<Punishment>> banCache;

    private final Cache<UUID, Optional<Punishment>> muteCache;

    private final Cache<String, Optional<Punishment>> ipBanCache;

    private final Cache<String, Optional<Punishment>> ipMuteCache;

    public CacheManager(WeGuardian plugin) {
        int cacheExpiry = plugin.getConfig().getInt("cache.expiry-minutes", 10);
        int maxSize = plugin.getConfig().getInt("cache.max-size", 1000);

        this.banCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(cacheExpiry, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.muteCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(cacheExpiry, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.ipBanCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(cacheExpiry, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.ipMuteCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(cacheExpiry, TimeUnit.MINUTES)
                .recordStats()
                .build();

        plugin.getLogger().info("Cache manager initialized (max: " + maxSize + ", expiry: " + cacheExpiry + "m)");
    }


    public Optional<Punishment> getActiveBan(UUID uuid) {
        Optional<Punishment> cached = banCache.getIfPresent(uuid);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                banCache.invalidate(uuid);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }


    public Optional<Punishment> getActiveMute(UUID uuid) {
        Optional<Punishment> cached = muteCache.getIfPresent(uuid);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                muteCache.invalidate(uuid);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }


    public void cacheBan(UUID uuid, Optional<Punishment> punishment) {
        banCache.put(uuid, punishment);
    }


    public void cacheMute(UUID uuid, Optional<Punishment> punishment) {
        muteCache.put(uuid, punishment);
    }


    public Optional<Punishment> getActiveIpBan(String ipAddress) {
        Optional<Punishment> cached = ipBanCache.getIfPresent(ipAddress);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                ipBanCache.invalidate(ipAddress);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }

    public Optional<Punishment> getActiveIpMute(String ipAddress) {
        Optional<Punishment> cached = ipMuteCache.getIfPresent(ipAddress);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                ipMuteCache.invalidate(ipAddress);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }


    public void cacheIpBan(String ipAddress, Optional<Punishment> punishment) {
        ipBanCache.put(ipAddress, punishment);
    }


    public void cacheIpMute(String ipAddress, Optional<Punishment> punishment) {
        ipMuteCache.put(ipAddress, punishment);
    }


    public void invalidate(UUID uuid) {
        banCache.invalidate(uuid);
        muteCache.invalidate(uuid);
    }


    public void invalidateBan(UUID uuid) {
        banCache.invalidate(uuid);
    }


    public void invalidateMute(UUID uuid) {
        muteCache.invalidate(uuid);
    }


    public void invalidateIpBan(String ipAddress) {
        ipBanCache.invalidate(ipAddress);
    }


    public void invalidateIpMute(String ipAddress) {
        ipMuteCache.invalidate(ipAddress);
    }


    public void clear() {
        banCache.invalidateAll();
        muteCache.invalidateAll();
        ipBanCache.invalidateAll();
        ipMuteCache.invalidateAll();
    }

    public String getStats() {
        return String.format(
                "Bans: %d cached | Mutes: %d cached | IP Bans: %d cached | IP Mutes: %d cached",
                banCache.estimatedSize(),
                muteCache.estimatedSize(),
                ipBanCache.estimatedSize(),
                ipMuteCache.estimatedSize());
    }


    public void cleanup() {
        banCache.cleanUp();
        muteCache.cleanUp();
        ipBanCache.cleanUp();
        ipMuteCache.cleanUp();
    }
}
