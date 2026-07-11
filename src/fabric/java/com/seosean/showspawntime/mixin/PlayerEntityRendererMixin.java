package com.seosean.showspawntime.mixin;

import com.seosean.showspawntime.PlayerRenderStateAlpha;
import com.seosean.showspawntime.ShowSpawnTimeClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void showspawntime$updateRenderState(AbstractClientPlayerEntity player, PlayerEntityRenderState state,
                                                 float tickProgress, CallbackInfo ci) {
        float alpha = ShowSpawnTimeClient.playerAlpha(player);
        ((PlayerRenderStateAlpha) state).showspawntime$setAlpha(alpha);
        if (alpha <= 0.0F) {
            state.invisible = true;
            state.invisibleToPlayer = true;
            state.onFire = false;
        }
    }
}
