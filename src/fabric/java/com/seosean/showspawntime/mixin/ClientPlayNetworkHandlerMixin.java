package com.seosean.showspawntime.mixin;

import com.seosean.showspawntime.ShowSpawnTimeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void showspawntime$playSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        showspawntime$handleSound(packet.getSound().value().id().toString(), packet.getPitch());
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"))
    private void showspawntime$playSoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        showspawntime$handleSound(packet.getSound().value().id().toString(), packet.getPitch());
    }

    private static void showspawntime$handleSound(String sound, float pitch) {
        MinecraftClient.getInstance().execute(() -> ShowSpawnTimeClient.STATE.onSound(sound, pitch));
    }
}
