package me.collective.mixin;

import me.collective.gui.alt.AltScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Кнопка Alt Manager в главном меню. */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T element);

    @Inject(method = "init", at = @At("TAIL"))
    private void addAltsButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Alts"), button ->
            MinecraftClient.getInstance().setScreen(new AltScreen(self))
        ).dimensions(self.width - 108, self.height - 28, 100, 20).build());
    }
}
