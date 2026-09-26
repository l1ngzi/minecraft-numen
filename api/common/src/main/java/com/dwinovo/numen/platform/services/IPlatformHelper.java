package com.dwinovo.numen.platform.services;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;

public interface IPlatformHelper {

    String getPlatformName();

    /** The game's config directory ({@code <gameDir>/config}); used for the user-editable models file. */
    java.nio.file.Path getConfigDir();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /**
     * 在 MC 的指令参数类型注册表({@code COMMAND_ARGUMENT_TYPE})里登记一种参数类型,按类认。两侧的公共初始化各调一次;
     * 各加载器的登记时机与写法不同(NeoForge 在注册事件里、Fabric 当场),由实现各管各的。
     *
     * @param name 注册名的路径部分,命名空间是引擎的 mod id
     */
    <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void registerArgumentType(
            String name, Class<A> type, ArgumentTypeInfo<A, T> info);

    /** 1.20.1 原版触及范围;加载器提供属性时由它读实际值。 */
    default double blockInteractionRange(net.minecraft.world.entity.player.Player player) {
        return player.isCreative() ? 5.0 : 4.5;
    }

    default double entityInteractionRange(net.minecraft.world.entity.player.Player player) {
        return player.isCreative() ? 6.0 : 3.0;
    }

    default boolean canInteractWithBlock(net.minecraft.world.entity.player.Player player,
                                        net.minecraft.core.BlockPos pos, double padding) {
        double range = blockInteractionRange(player) + padding;
        return player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= range * range;
    }

    default boolean canInteractWithEntity(net.minecraft.world.entity.player.Player player,
                                         net.minecraft.world.entity.Entity entity, double padding) {
        double range = entityInteractionRange(player) + padding;
        return entity.getBoundingBox().distanceToSqr(player.getEyePosition()) < range * range;
    }

    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
