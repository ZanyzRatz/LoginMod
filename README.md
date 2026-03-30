# Session ID Connect — Fabric 1.20.4

A **client-only** Fabric mod that adds a **"⚡ Session ID"** button to the
top-right corner of the Multiplayer / Server List screen.

Click it, paste a Session ID, and connect instantly — no need to manually
add the server to your list.

---

## How It Works

### Session ID Format

```
host:port@token
host@token
host:port
```

**Examples:**
```
play.myserver.net@abc123secrettoken
play.myserver.net:25565@abc123secrettoken
mc.example.com@eyJhbGciOiJIUzI1NiJ9...
192.168.1.10:25565
```

The `token` after `@` is your session/auth token.  
The mod connects you to `host:port` — your server-side auth plugin reads
the token after you join (e.g. via `/token abc123` or a custom packet).

### Recent IDs
Up to 5 recently used Session IDs are saved in  
`config/sessionid_recent.json` and shown as quick-connect buttons.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.15+ for MC 1.20.4
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop `session-id-connect-1.0.0.jar` into your `mods/` folder
4. **Client-only** — does NOT need to be installed on the server

---

## Building

```bash
./gradlew build
# Output: build/libs/session-id-connect-1.0.0.jar
```

Requires Java 17+.

---

## Project Structure

```
src/main/java/com/sessionid/
├── SessionIdMod.java                  # Client entrypoint
├── client/
│   ├── SessionIdScreen.java           # Popup connect screen
│   └── SessionIdStore.java            # Recent IDs + ID parser
└── mixin/
    └── MultiplayerScreenMixin.java    # Injects button into server list
```

---

## Customization

- **Button position**: edit `btnX` / `btnY` in `MultiplayerScreenMixin.java`
- **Max recent saved**: edit `MAX_RECENT` in `SessionIdStore.java`
- **Token handling**: the token is part of the raw ID string saved to recent;
  how it's consumed is up to your server-side auth plugin
