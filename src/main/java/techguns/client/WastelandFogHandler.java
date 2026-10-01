package techguns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.util.MathUtil;
import techguns.world.wasteland.BiomeWasteland;

/**
 * Dust fog of the wasteland biomes (green in the radioactive zone). Strength and color change
 * slowly when walking between biomes, under a roof the fog fades out. When the world was restored
 * by the Purifier of the campaign the fog slowly clears.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Techguns.MODID)
public class WastelandFogHandler {

	protected static float strength = 0.0f;
	protected static float red = 0.66f;
	protected static float green = 0.58f;
	protected static float blue = 0.44f;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		float target = 0.0f;
		float tr = red;
		float tg = green;
		float tb = blue;
		//after the launch of the Purifier the dust is gone and the fog is normal again
		if (TGConfig.cl_wastelandFog && !BiomeWasteland.isWorldRestored() && mc.world != null && mc.player != null) {
			BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ);
			Biome biome = mc.world.getBiome(pos);
			if (biome instanceof BiomeWasteland) {
				BiomeWasteland wasteland = (BiomeWasteland) biome;
				target = wasteland.getFogStrength();
				int color = wasteland.getFogColor();
				tr = ((color >> 16) & 255) / 255.0f;
				tg = ((color >> 8) & 255) / 255.0f;
				tb = (color & 255) / 255.0f;
				if (!mc.world.canSeeSky(pos)) {
					target *= 0.35f;
				}
			}
		}
		strength += (target - strength) * 0.04f;
		red += (tr - red) * 0.04f;
		green += (tg - green) * 0.04f;
		blue += (tb - blue) * 0.04f;
		if (strength < 0.002f) {
			strength = 0.0f;
		}
	}

	@SubscribeEvent
	public static void onFogColors(EntityViewRenderEvent.FogColors event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (strength <= 0.0f || mc.world == null || event.getState().getMaterial().isLiquid()) {
			return;
		}
		//keep day and night, the dust is only lit by the sun
		float angle = mc.world.getCelestialAngle((float) event.getRenderPartialTicks());
		float light = MathUtil.clamp(MathHelper.cos(angle * (float) Math.PI * 2.0f) * 2.0f + 0.5f, 0.06f, 1.0f);
		float s = strength * 0.85f;
		event.setRed(event.getRed() * (1.0f - s) + red * light * s);
		event.setGreen(event.getGreen() * (1.0f - s) + green * light * s);
		event.setBlue(event.getBlue() * (1.0f - s) + blue * light * s);
	}

	@SubscribeEvent
	public static void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
		if (strength <= 0.0f) {
			return;
		}
		float far = event.getFarPlaneDistance();
		float end = Math.min(far, far - (far - 48.0f) * strength * 0.8f);
		float start = event.getFogMode() == -1 ? 0.0f : end * (0.05f + 0.35f * (1.0f - strength));
		GlStateManager.setFogStart(start);
		GlStateManager.setFogEnd(end);
	}
}
