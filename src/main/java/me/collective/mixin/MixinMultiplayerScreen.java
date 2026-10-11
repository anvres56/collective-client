package me.collective.mixin;

import me.collective.gui.alt.AltScreen;
import me.collective.gui.proxy.ProxyScreen;
import me.collective.proxy.ProxyManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Кнопки Alt Manager и выбора прокси в меню серверов. */
@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen {

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T element);

    @Inject(method = "init", at = @At("TAIL"))
    private void addAltProxyButtons(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Alts"), button ->
            MinecraftClient.getInstance().setScreen(new AltScreen(self))
        ).dimensions(self.width - 108, 6, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Proxy: " + ProxyManager.INSTANCE.activeName()), button ->
            MinecraftClient.getInstance().setScreen(new ProxyScreen(self))
        ).dimensions(self.width - 108, 30, 100, 20).build());
    }
}
