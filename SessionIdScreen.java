package com.sessionid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Modal popup shown when the player clicks "Session ID" on the multiplayer screen.
 *
 * The player pastes a Session ID string and clicks Connect (or presses Enter).
 * Recent IDs are listed below the input for quick reuse.
 */
public class SessionIdScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget idField;
    private ButtonWidget connectButton;

    private String errorMessage = "";
    private int shakeTicks = 0;
    private int shakeOffset = 0;
    private float alpha = 0f;

    // Layout constants
    private static final int PANEL_W = 320;
    private static final int PANEL_H_BASE = 130;
    private static final int ROW_H = 18;

    public SessionIdScreen(Screen parent) {
        super(Text.literal("Connect via Session ID"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        SessionIdStore.load();

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Input field
        idField = new TextFieldWidget(
                textRenderer,
                cx - 140, cy - 38,
                280, 20,
                Text.literal("Session ID")
        );
        idField.setMaxLength(256);
        idField.setPlaceholder(Text.literal("host:port@token  or  host@token"));
        this.addDrawableChild(idField);

        // Connect button
        connectButton = ButtonWidget.builder(Text.literal("Connect"), btn -> doConnect())
                .dimensions(cx - 70, cy - 10, 140, 20)
                .build();
        this.addDrawableChild(connectButton);

        // Cancel button
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
                        .dimensions(cx - 70, cy + 14, 140, 20)
                        .build()
        );

        // Recent session buttons (up to 5 shown)
        List<SessionIdStore.SessionEntry> recent = SessionIdStore.getRecent();
        int shown = Math.min(recent.size(), 5);
        for (int i = 0; i < shown; i++) {
            final SessionIdStore.SessionEntry entry = recent.get(i);
            String label = entry.label != null && !entry.label.isBlank() ? entry.label : entry.rawId;
            if (label.length() > 36) label = label.substring(0, 33) + "...";
            final String displayLabel = label;

            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("⟳  " + displayLabel), btn -> {
                        idField.setText(entry.rawId);
                        doConnect();
                    }).dimensions(cx - 140, cy + 42 + i * (ROW_H + 2), 280, ROW_H).build()
            );
        }

        this.setInitialFocus(idField);
    }

    private void doConnect() {
        String raw = idField.getText().trim();
        if (raw.isEmpty()) {
            showError("Please enter a Session ID.");
            return;
        }

        SessionIdStore.ParsedSession parsed = SessionIdStore.parse(raw);
        if (parsed == null || parsed.host.isEmpty()) {
            showError("Invalid Session ID format.");
            return;
        }

        // Save to recent
        String label = parsed.token != null
                ? parsed.displayAddress() + " (token)"
                : parsed.displayAddress();
        SessionIdStore.addRecent(new SessionIdStore.SessionEntry(raw, label));

        // Build ServerInfo and connect
        ServerInfo info = new ServerInfo(
                parsed.token != null ? "Session: " + parsed.displayAddress() : parsed.displayAddress(),
                parsed.displayAddress(),
                ServerInfo.ServerType.OTHER
        );

        // If a token is present, store it so the server-side can read it.
        // We pass it as the server's resource pack URL field (safe, unused by vanilla)
        // OR simply join — the token is in the raw ID for server-side mods to use.
        // Most session-auth plugins read it from a login command or packet after join.
        MinecraftClient client = MinecraftClient.getInstance();

        // Validate address parses correctly
        try {
            ServerAddress.parse(parsed.displayAddress());
        } catch (Exception e) {
            showError("Could not resolve address: " + parsed.host);
            return;
        }

        client.execute(() -> {
            // Connect directly — opens the downloading terrain screen
            client.setScreen(new net.minecraft.client.gui.screen.ConnectScreen(
                    parent,
                    client,
                    ServerAddress.parse(parsed.displayAddress()),
                    info,
                    false,
                    null
            ));
        });
    }

    private void showError(String msg) {
        errorMessage = msg;
        shakeTicks = 14;
    }

    @Override
    public void tick() {
        if (alpha < 1f) alpha = Math.min(1f, alpha + 0.08f);
        idField.tick();

        if (shakeTicks > 0) {
            shakeTicks--;
            shakeOffset = (shakeTicks % 4 < 2) ? 5 : -5;
        } else {
            shakeOffset = 0;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dim background
        this.renderBackground(ctx);

        int cx = this.width / 2 + shakeOffset;
        int cy = this.height / 2;

        List<SessionIdStore.SessionEntry> recent = SessionIdStore.getRecent();
        int shownRecent = Math.min(recent.size(), 5);
        int panelH = PANEL_H_BASE + (shownRecent > 0 ? 24 + shownRecent * (ROW_H + 2) : 0);

        int px = cx - PANEL_W / 2;
        int py = cy - panelH / 2 - 10;

        // Shadow
        ctx.fill(px + 5, py + 5, px + PANEL_W + 5, py + panelH + 5, 0x55000000);
        // Panel
        ctx.fill(px, py, px + PANEL_W, py + panelH, 0xF0101018);
        // Border
        ctx.drawBorder(px, py, PANEL_W, panelH, 0xFF3a7bd5);

        // Title bar
        ctx.fill(px, py, px + PANEL_W, py + 22, 0xFF1a2a4a);
        ctx.drawCenteredTextWithShadow(textRenderer, "⚡  Session ID Connect", cx, py + 7, 0xFF7ab3ff);

        // Divider
        ctx.fill(px + 8, py + 23, px + PANEL_W - 8, py + 24, 0x883a7bd5);

        // Label
        ctx.drawTextWithShadow(textRenderer, "Session ID:", cx - 140, cy - 50, 0xFFaaccff);

        // Hint text
        ctx.drawTextWithShadow(textRenderer, "Format:  host:port@token", cx - 140, cy + 40, 0xFF556688);

        // Error
        if (!errorMessage.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "§c✗ " + errorMessage, cx, py + panelH - 12, 0xFFff5555);
        }

        // Recent label
        if (shownRecent > 0) {
            ctx.fill(px + 8, cy + 36, px + PANEL_W - 8, cy + 37, 0x553a7bd5);
            ctx.drawTextWithShadow(textRenderer, "Recent:", cx - 140, cy + 40, 0xFF7799cc);
        }

        // Re-align widgets to shake offset
        idField.setX(cx - 140);
        connectButton.setX(cx - 70);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / numpad enter
            if (connectButton.active) doConnect();
            return true;
        }
        if (keyCode == 256) { // ESC
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
