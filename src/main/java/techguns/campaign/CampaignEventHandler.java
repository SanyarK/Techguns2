package techguns.campaign;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.monster.IMob;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
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
import techguns.TGPackets;
import techguns.Techguns;
import techguns.capabilities.TGCampaignData;
import techguns.capabilities.TGCampaignDataCapProvider;
import techguns.packets.PacketCampaignEvent;
import techguns.world.wasteland.BiomeWasteland;

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
		//remind the player where the current objective is
		if (TGConfig.campaignEnabled && data.getState() == TGCampaignData.STATE_ACTIVE && data.hasObjective() && !data.isFinished()) {
			TGCampaign.announcePoint(player, data.getObjective());
		}
		//rain, sky and fog of a world the Purifier restored
		boolean restored = WorldRestoration.get(TGCampaign.overworld(player)).isRestored();
		TGPackets.network.sendTo(new PacketCampaignEvent(PacketCampaignEvent.RESTORED, restored ? 1 : 0), player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			CampaignFinale.onLogout((EntityPlayerMP) event.player);
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
		if (player.world.isRemote) {
			return;
		}
		CampaignFinale.tick(player);
		if (player.world.provider.getDimension() != 0) {
			return;
		}
		if (player.ticksExisted <= 0 || player.ticksExisted % 20 != 0) {
			return;
		}
		TGCampaign.tickPlayer(player);
	}

	/**
	 * the wastes come back to life around the posts, a little every tick
	 */
	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != 0
				|| !(event.world instanceof WorldServer) || !BiomeWasteland.isWorldRestored()) {
			return;
		}
		WorldRestoration.get(event.world).tick((WorldServer) event.world);
	}

	/**
	 * the server side flag of the restored world follows the loaded overworld
	 */
	@SubscribeEvent
	public static void onWorldLoad(WorldEvent.Load event) {
		World world = event.getWorld();
		if (!world.isRemote && world.provider.getDimension() == 0) {
			BiomeWasteland.setWorldRestored(WorldRestoration.get(world).isRestored());
		}
	}

	/**
	 * on restored land the monsters are rare
	 */
	@SubscribeEvent
	public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
		World world = event.getWorld();
		if (world.isRemote || !BiomeWasteland.isWorldRestored() || world.provider.getDimension() != 0 || !(event.getEntityLiving() instanceof IMob)) {
			return;
		}
		if (world.rand.nextFloat() < WorldRestoration.SPAWN_DENY_CHANCE && WorldRestoration.get(world).isInRestoredArea(event.getX(), event.getZ())) {
			event.setResult(Event.Result.DENY);
		}
	}

	/**
	 * the Wasteland Hero carries the title in the chat
	 */
	@SubscribeEvent
	public static void onServerChat(ServerChatEvent event) {
		EntityPlayerMP player = event.getPlayer();
		TGCampaignData data = player != null ? TGCampaignData.get(player) : null;
		if (data == null || !data.isFinished() || !TGConfig.campaignEnabled) {
			return;
		}
		ITextComponent title = new TextComponentTranslation(Techguns.MODID + ".campaign.hero.chat_tag", new TextComponentTranslation(Techguns.MODID + ".campaign.hero.title"));
		title.setStyle(new Style().setColor(TextFormatting.GOLD));
		ITextComponent name = new TextComponentString("").appendSibling(title).appendSibling(player.getDisplayName());
		event.setComponent(new TextComponentTranslation("chat.type.text", name, ForgeHooks.newChatWithLinks(event.getMessage())));
	}
}
