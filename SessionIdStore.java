package com.sessionid.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores recently used Session IDs locally so the player can re-use them.
 *
 * A Session ID is a string in the format:  address:port@token
 * e.g.  "play.myserver.net:25565@abc123token"
 * or just  "play.myserver.net@abc123token"  (default port 25565)
 *
 * The token portion is passed as the player's username in a direct-connect
 * so that server-side auth plugins can validate it.
 */
public class SessionIdStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path savePath;
    private static List<SessionEntry> recent = new ArrayList<>();
    private static final int MAX_RECENT = 10;

    public static void load() {
        savePath = FabricLoader.getInstance().getConfigDir().resolve("sessionid_recent.json");
        if (savePath.toFile().exists()) {
            try (Reader r = new FileReader(savePath.toFile(), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<List<SessionEntry>>() {}.getType();
                List<SessionEntry> loaded = GSON.fromJson(r, type);
                if (loaded != null) recent = loaded;
            } catch (IOException ignored) {}
        }
    }

    public static void save() {
        if (savePath == null) load();
        try (Writer w = new FileWriter(savePath.toFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(recent, w);
        } catch (IOException ignored) {}
    }

    public static void addRecent(SessionEntry entry) {
        recent.removeIf(e -> e.rawId.equals(entry.rawId));
        recent.add(0, entry);
        if (recent.size() > MAX_RECENT) recent = recent.subList(0, MAX_RECENT);
        save();
    }

    public static List<SessionEntry> getRecent() {
        return recent;
    }

    /**
     * Parse a raw session ID string into host, port, and token.
     * Accepted formats:
     *   host:port@token
     *   host@token
     *   host:port          (no token — plain direct connect)
     *   host
     */
    public static ParsedSession parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();

        String address;
        int port = 25565;
        String token = null;

        int atIdx = raw.indexOf('@');
        if (atIdx >= 0) {
            address = raw.substring(0, atIdx);
            token = raw.substring(atIdx + 1).trim();
        } else {
            address = raw;
        }

        // Extract port from address
        int colonIdx = address.lastIndexOf(':');
        if (colonIdx >= 0) {
            try {
                port = Integer.parseInt(address.substring(colonIdx + 1));
                address = address.substring(0, colonIdx);
            } catch (NumberFormatException ignored) {}
        }

        return new ParsedSession(address.trim(), port, token, raw);
    }

    // ── Data classes ────────────────────────────────────────────────────────

    public static class SessionEntry {
        public String rawId;
        public String label; // friendly display name

        public SessionEntry(String rawId, String label) {
            this.rawId = rawId;
            this.label = label;
        }
    }

    public static class ParsedSession {
        public final String host;
        public final int port;
        public final String token; // may be null
        public final String raw;

        public ParsedSession(String host, int port, String token, String raw) {
            this.host  = host;
            this.port  = port;
            this.token = token;
            this.raw   = raw;
        }

        public String displayAddress() {
            return port == 25565 ? host : host + ":" + port;
        }
    }
}
