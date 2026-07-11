package com.seosean.showspawntime.mixin;

import com.seosean.showspawntime.PowerupManager;
import com.seosean.showspawntime.ShowSpawnTimeClient;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntityRenderer.class)
public abstract class ArmorStandEntityRendererMixin {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/decoration/ArmorStandEntity;Lnet/minecraft/client/render/entity/state/ArmorStandEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void showspawntime$updatePowerupLabel(ArmorStandEntity entity, ArmorStandEntityRenderState state,
                                                   float tickProgress, CallbackInfo ci) {
        PowerupManager.ActivePowerup powerup = ShowSpawnTimeClient.POWERUPS.find(entity);
        if (powerup != null) {
            state.displayName = powerup.countdownName();
        }
    }
}
