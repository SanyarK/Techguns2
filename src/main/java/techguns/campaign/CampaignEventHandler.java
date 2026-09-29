package techguns.campaign;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import techguns.TGConfig;
import techguns.TGItems;
import techguns.Techguns;
import techguns.capabilities.TGCampaignData;
import techguns.capabilities.TGCampaignDataCapProvider;

/**
 * Events of the story campaign: capability attach, the start (radio in the bunker or the radio item),
 * converted old progress, kill tracking, stash searching and the per second mission update.
 */
@Mod.EventBusSubscriber(modid = Techguns.MODID)
public class CampaignEventHandler {

	@SubscribeEvent
	public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(TGCampaignDataCapProvider.ID, new TGCampaignDataCapProvider());
		}
	}

	@SubscribeEvent
	public static void playerClone(final PlayerEvent.Clone event) {
		TGCampaignData oldData = TGCampaignData.get(event.getOriginal());
		TGCampaignData newData = TGCampaignData.get(event.getEntityPlayer());
		if (oldData != null && newData != null) {
			newData.copyFrom(oldData);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		if (!(event.player instanceof EntityPlayerMP)) {
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		TGCampaignData data = TGCampaignData.get(player);
		if (data == null) {
			return;
		}
		if (TGConfig.campaignEnabled && !data.isRadioGiven()) {
			data.setRadioGiven(true);
			//in the start bunker the radio on the wall calls, elsewhere the commander sends a radio
			if (TGConfig.campaignGiveRadio && !TGCampaign.isAtStartBunker(player)) {
				TGCampaign.giveOrDrop(player, new ItemStack(TGItems.RADIO));
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.radio_given").setStyle(new Style().setColor(TextFormatting.GOLD)));
			}
		}
		if (data.getMigratedFrom() > 0) {
			//progress of the old 10 mission campaign was converted, its objective is gone
			TGCampaign.overworldData(player).clearPoints(player);
			CampaignMission m = CampaignMissions.byId(data.getMission());
			if (m != null) {
				player.sendMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.msg.migrated", m.id, new TextComponentTranslation(m.getTitleKey()))
						.setStyle(new Style().setColor(TextFormatting.GOLD)));
			}
			data.clearMigrated();
		}
		TGCampaign.sync(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			TGCampaign.onPlayerLoggedOut((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			TGCampaign.sync((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			TGCampaign.stopBattle((EntityPlayerMP) event.player, true);
			TGCampaign.sync((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		EntityLivingBase victim = event.getEntityLiving();
		if (victim.world.isRemote || !TGConfig.campaignEnabled) {
			return;
		}
		if (victim instanceof EntityPlayerMP && !(victim instanceof FakePlayer)) {
			TGCampaign.onPlayerDied((EntityPlayerMP) victim);
			return;
		}
		Entity killer = event.getSource().getTrueSource();
		if (killer instanceof EntityPlayerMP && !(killer instanceof FakePlayer)) {
			TGCampaign.onEntityKilled((EntityPlayerMP) killer, victim);
		}
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getWorld().isRemote || !TGConfig.campaignEnabled || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
			return;
		}
		TGCampaign.onBlockInteract((EntityPlayerMP) event.getEntityPlayer(), event.getPos());
	}

	/**
	 * a stash that is broken instead of opened counts as searched too
	 */
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getWorld().isRemote || !TGConfig.campaignEnabled || !(event.getPlayer() instanceof EntityPlayerMP)) {
			return;
		}
		TGCampaign.onBlockInteract((EntityPlayerMP) event.getPlayer(), event.getPos());
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !TGConfig.campaignEnabled) {
			return;
		}
		if (!(event.player instanceof EntityPlayerMP) || event.player instanceof FakePlayer) {
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) event.player;
		if (player.world.isRemote || player.world.provider.getDimension() != 0) {
			return;
		}
		if (player.ticksExisted <= 0 || player.ticksExisted % 20 != 0) {
			return;
		}
		TGCampaign.tickPlayer(player);
	}
}
