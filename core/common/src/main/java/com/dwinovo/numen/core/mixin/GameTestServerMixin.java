package com.dwinovo.numen.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 仓库跑批场地悬空:1.20.1 固定在 -60 的场地会被原版清场逻辑补上天然泥土,把独立平台连在一起。 */
@Mixin(GameTestServer.class)
abstract class GameTestServerMixin {
    @ModifyArg(method = "startTests", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/gametest/framework/GameTestRunner;runTestBatches(Ljava/util/Collection;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/gametest/framework/GameTestTicker;I)Ljava/util/Collection;"), index = 1)
    private BlockPos numen$testPlatformOrigin(BlockPos original) {
        return System.getProperty("numen.gametest.structures") == null
                ? original : new BlockPos(original.getX(), 60, original.getZ());
    }
}
