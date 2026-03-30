package com.sessionid.mixin;

import com.sessionid.client.SessionIdScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects a "Session ID" button into the top-right corner of the
 * Multiplayer / Server List screen.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sessionid$addButton(CallbackInfo ci) {
        // Position: top-right corner — 8px from right edge, 8px from top
        int btnW = 90;
        int btnH = 16;
        int btnX = this.width - btnW - 8;
        int btnY = 8;

        MultiplayerScreen self = (MultiplayerScreen) (Object) this;

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("⚡ Session ID"),
                        btn -> this.client.setScreen(new SessionIdScreen(self))
                )
                .dimensions(btnX, btnY, btnW, btnH)
                .build()
        );
    }
}
