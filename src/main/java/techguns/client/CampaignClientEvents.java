package techguns.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import techguns.Techguns;
import techguns.campaign.HeroItems;
import techguns.world.wasteland.BiomeWasteland;

/**
 * Client side of the campaign ending: the white flash of the Purifier launch, the restored world
 * (the chunks are rendered again for the green grass) and the tooltips of the hero items.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Techguns.MODID)
public class CampaignClientEvents {

	protected static int flashTicks = 0;
	protected static int flashLength = 1;
	/** the flag of the biome is shared with the integrated server, this is what the client rendered last */
	protected static boolean knownRestored = false;

	public static void flash(int ticks) {
		flashLength = Math.max(ticks, 1);
		flashTicks = flashLength;
	}

	public static void setRestored(boolean restored) {
		BiomeWasteland.setWorldRestored(restored);
		if (restored != knownRestored) {
			knownRestored = restored;
			Minecraft mc = Minecraft.getMinecraft();
			if (mc.world != null && mc.renderGlobal != null) {
				mc.renderGlobal.loadRenderers();
			}
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END && flashTicks > 0) {
			flashTicks--;
		}
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || flashTicks <= 0) {
			return;
		}
		float f = Math.max(0.0f, Math.min(1.0f, (flashTicks - event.getPartialTicks()) / flashLength * 1.5f));
		int alpha = (int) (f * 255.0f);
		if (alpha <= 3) {
			return;
		}
		ScaledResolution res = event.getResolution();
		Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), (alpha << 24) | 0xFFFFF4);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		String id = HeroItems.getHeroId(event.getItemStack());
		List<String> tooltip = event.getToolTip();
		if (id == null || tooltip.isEmpty()) {
			return;
		}
		tooltip.set(0, TextFormatting.GOLD + I18n.format(HeroItems.nameKey(id)));
		List<String> lore = new ArrayList<>();
		lore.add(TextFormatting.YELLOW + I18n.format(Techguns.MODID + ".hero.tooltip"));
		for (int i = 1; i <= HeroItems.LORE_LINES; i++) {
			lore.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + I18n.format(HeroItems.loreKey(id, i)));
		}
		tooltip.addAll(1, lore);
	}

	@SubscribeEvent
	public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
		knownRestored = false;
		flashTicks = 0;
		BiomeWasteland.setWorldRestored(false);
	}
}
