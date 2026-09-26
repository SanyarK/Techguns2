package techguns.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.entities.npcs.MilitaryJet;
import techguns.items.ItemMilitaryContract;

/**
 * Events for military contracts (quest progress) and random air raids
 */
@Mod.EventBusSubscriber(modid = Techguns.MODID)
public class MilitaryExpansionEventHandler {

	protected static final double AIR_RAID_SPAWN_DISTANCE = 72.0D;

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		EntityLivingBase victim = event.getEntityLiving();
		if (victim.world.isRemote) {
			return;
		}
		Entity killer = event.getSource().getTrueSource();
		if (killer instanceof EntityPlayer && !(killer instanceof FakePlayer)) {
			ItemMilitaryContract.onEntityKilled((EntityPlayer) killer, victim);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !TGConfig.airRaids) {
			return;
		}
		EntityPlayer player = event.player;
		World world = player.world;
		if (world.isRemote || world.provider.getDimension() != 0) {
			return;
		}
		if (player.ticksExisted <= 0 || player.ticksExisted % TGConfig.airRaidInterval != 0) {
			return;
		}
		if (player.isCreative() || player.isSpectator() || world.getDifficulty() == EnumDifficulty.PEACEFUL) {
			return;
		}
		if (world.rand.nextFloat() >= TGConfig.airRaidChance) {
			return;
		}

		BlockPos spawn = world.getSpawnPoint();
		double dx = player.posX - spawn.getX();
		double dz = player.posZ - spawn.getZ();
		double minDist = TGConfig.airRaidMinDistance;
		if (dx * dx + dz * dz < minDist * minDist) {
			return;
		}

		//jets can't reach players in caves and buildings
		if (!world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ))) {
			return;
		}

		//only one raid at a time
		if (!world.getEntitiesWithinAABB(MilitaryJet.class, player.getEntityBoundingBox().grow(160.0D, 128.0D, 160.0D)).isEmpty()) {
			return;
		}

		startAirRaid(world, player);
	}

	/**
	 * Spawns a military jet some distance away from the target that attacks it
	 */
	public static boolean startAirRaid(World world, EntityPlayer target) {
		double angle = world.rand.nextDouble() * Math.PI * 2.0D;
		double x = target.posX + Math.cos(angle) * AIR_RAID_SPAWN_DISTANCE;
		double z = target.posZ + Math.sin(angle) * AIR_RAID_SPAWN_DISTANCE;
		int ground = world.getHeight(MathHelper.floor(x), MathHelper.floor(z));
		double y = Math.min(Math.max(ground, target.posY) + 30.0D, world.getActualHeight() - 5);

		MilitaryJet jet = new MilitaryJet(world);
		float yaw = (float) (-MathHelper.atan2(target.posX - x, target.posZ - z) * (180D / Math.PI));
		jet.setLocationAndAngles(x, y, z, yaw, 0.0F);
		jet.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(jet)), (IEntityLivingData) null);

		if (world.spawnEntity(jet)) {
			jet.setAttackTarget(target);
			target.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".message.airraid").setStyle(new Style().setColor(TextFormatting.RED)), true);
			return true;
		}
		return false;
	}
}
