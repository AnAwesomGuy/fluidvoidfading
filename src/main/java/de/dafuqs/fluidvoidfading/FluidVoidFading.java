package de.dafuqs.fluidvoidfading;

import net.minecraft.ResourceLocationException;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod(value = FluidVoidFading.MODID, dist = Dist.CLIENT)
public class FluidVoidFading {
    public static final String MODID = "fluidvoidfading";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TRANSPARENT_FLUIDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        TRANSPARENT_FLUIDS = builder.comment("If you notice a fluid not rendering transparent, try adding its identifier here")
                .defineListAllowEmpty("additionalTransparentFluids", List.of("minecraft:flowing_lava"), () -> "", o -> {
                    if (o instanceof String)
                        try {
                            ResourceLocation.parse((String) o);
                            return true;
                        } catch (ResourceLocationException ignored) {
                        }
                    return false;
                });
        SPEC = builder.build();
    }

    public FluidVoidFading(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        bus.addListener(FluidVoidFading::clientStartup);
    }

    public static void clientStartup(FMLClientSetupEvent event) {
        for (String s : TRANSPARENT_FLUIDS.get()) {
            ResourceLocation rl = ResourceLocation.parse(s);
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            if (fluid == Fluids.EMPTY)
                LOGGER.error("Fluid '{}' not found!", s);
            ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent());
        }
    }
}
