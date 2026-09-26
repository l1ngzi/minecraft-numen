package com.dwinovo.numen.platform;

import com.dwinovo.numen.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge 1.20.1 implementation of {@link IPlatformHelper}. Mirrors the behaviour
 * of {@code FabricPlatformHelper} / the NeoForge reference, just against Forge's
 * {@code ModList} / {@code FMLPaths} / {@code FMLEnvironment}.
 */
public class ForgePlatformHelper implements IPlatformHelper {

    public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.commands.synchronization.ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.COMMAND_ARGUMENT_TYPE,
                    com.dwinovo.numen.Constants.MOD_ID);

    @Override
    public <A extends com.mojang.brigadier.arguments.ArgumentType<?>, T extends net.minecraft.commands.synchronization.ArgumentTypeInfo.Template<A>> void registerArgumentType(
            String name, Class<A> type, net.minecraft.commands.synchronization.ArgumentTypeInfo<A, T> info) {
        ARGUMENT_TYPES.register(name, () -> net.minecraft.commands.synchronization.ArgumentTypeInfos.registerByClass(type, info));
    }


    @Override
    public double blockInteractionRange(net.minecraft.world.entity.player.Player player) { return player.getBlockReach(); }

    @Override
    public double entityInteractionRange(net.minecraft.world.entity.player.Player player) { return player.getEntityReach(); }

    @Override
    public boolean canInteractWithBlock(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, double padding) {
        return player.canReach(pos, padding);
    }

    @Override
    public boolean canInteractWithEntity(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity entity, double padding) {
        return player.canReach(entity, padding);
    }

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }
}
