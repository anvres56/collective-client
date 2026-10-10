package me.collective.module.combat;

import me.collective.category.Category;
import me.collective.event.EventHandler;
import me.collective.event.events.TickEvent;
import me.collective.module.Module;
import me.collective.setting.BooleanSetting;
import me.collective.setting.NumberSetting;
import me.collective.util.InventoryUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    public final NumberSetting health = add(new NumberSetting("Health", 8.0, 1.0, 36.0, 1.0));
    public final BooleanSetting noTotems = add(new BooleanSetting("NoTotems", true));

    public AutoLog() {
        super("AutoLog", "Automatically disconnects from the server to save your life on 2b2t.", Category.COMBAT);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (hp <= health.getValue().floatValue()) {
            disconnect(String.format("Health fell below threshold: %.1f HP", hp));
            return;
        }

        if (noTotems.getValue()) {
            int totems = InventoryUtil.find(Items.TOTEM_OF_UNDYING).count();
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) totems++;
            if (totems <= 0) {
                disconnect("No totems of undying remaining in inventory!");
            }
        }
    }

    private void disconnect(String reason) {
        setToggled(false);
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("§c[AutoLog] §f" + reason));
        }
    }
}
