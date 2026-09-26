package com.dwinovo.numen.core;

import com.dwinovo.numen.agent.skill.SkillRegistry;
import com.dwinovo.numen.core.debug.DebugCommands;
import com.dwinovo.numen.core.debug.PathDebugRenderer;
import com.dwinovo.numen.core.pathing.cache.PathCaches;
import com.dwinovo.numen.core.scan.BlockSearch;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.nio.file.Path;

/**
 * Forge entry point for the numen-core tool pack. Registers the tools and task
 * runners into the numen-api engine, then wires the server-tick work its tools
 * need (budget-sliced block scans, the off-thread pathfinder's chunk snapshots).
 * The engine itself is brought up by the separate numen-api mod, which core
 * depends on.
 *
 * <p>Forge keeps separate mod and game event buses — per-tick / world lifecycle
 * events go on {@link MinecraftForge#EVENT_BUS}.
 */
@Mod(Constants.MOD_ID)
public class NumenCoreForge {

    public NumenCoreForge() {
        // 内嵌的联动模组:装了目标模组才接上,没装当不存在。见 plugins.Builtin。
        com.dwinovo.numen.plugins.Builtin.registerAll(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        NumenCore.init();
        // Forge 收集 GameTest 方法时不会初始化持有类;测试命令必须在服务器建指令树前登记。
        // 结构目录只在仓库的开发运行配置里声明,发行物不挂测试命令。
        if (System.getProperty("numen.gametest.structures") != null) {
            com.dwinovo.numen.core.gametest.CommandGameTests.registerFixture();
            com.dwinovo.numen.core.gametest.TaskControlGameTests.registerFixture();
        }

        // 游戏内用例的注册走<b>模组总线</b>,不是游戏总线:Forge 1.20.1 不像高版本
        // 那样扫描 @GameTestHolder 自动收集,得在 RegisterGameTestsEvent 里把持有
        // 类交出去。跑批时由 -Dforge.enabledGameTestNamespaces 决定跑不跑。
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((net.minecraftforge.event.RegisterGameTestsEvent e) -> {
                    e.register(com.dwinovo.numen.core.gametest.BuildGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.CollectGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.CombatGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.CommandGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.ContainerGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.FishGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.GameTestKit.class);
                    e.register(com.dwinovo.numen.core.gametest.GearGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.InteractGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.InventoryGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.LocateGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.MineGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.ModeGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.MovementGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.PerceptionGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.PermissionGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.PluginGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.SleepGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.SurvivalGameTests.class);
                    e.register(com.dwinovo.numen.core.gametest.TaskControlGameTests.class);
                });

        MinecraftForge.EVENT_BUS.addListener(NumenCoreForge::onServerTickPost);
        // Debug verbs merged into the /numen root registered by the engine mod.
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent e) ->
                DebugCommands.register(e.getDispatcher()));

        // Client-only: declare core's built-in skills, read in place from the
        // skills/ dir bundled in this jar. Skills feed the client-side LLM, so
        // this never runs on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            declareBundledSkills();
        }

        Constants.LOG.info("numen-core initialised on Forge.");
    }

    private static void declareBundledSkills() {
        Path root = ModJar.find("skills");
        if (root != null) {
            SkillRegistry.instance().declareBundled(root);
        } else {
            Constants.LOG.warn("[numen-core] no bundled skills/ dir found in jar");
        }
    }

    private static void onServerTickPost(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        // 排程机器的心跳随机器归了 numen-api;core 只 tick 自己的工具配套。
        BlockSearch.tick(server);
        com.dwinovo.numen.core.pathing.plan.RoutePlanner.serverTick(server);
        PathCaches.serverTick(server);
        // Debug particles for pathing state, sent only to players with debug on.
        PathDebugRenderer.serverTick(server);
    }
}
