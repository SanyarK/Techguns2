package techguns.entities.npcs;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

/**
 * Shared helper code for boss NPCs
 */
public class BossHelper {

	/**
	 * Tries to place a minion on free ground near the boss and spawns it
	 * @return true when the minion was spawned
	 */
	public static boolean spawnMinionNear(EntityLiving boss, EntityLiving minion, EntityLivingBase target, int radius) {
		World w = boss.world;
		for (int i = 0; i < 10; i++) {
			int x = (int) Math.floor(boss.posX + (w.rand.nextDouble() * 2.0D - 1.0D) * radius);
			int z = (int) Math.floor(boss.posZ + (w.rand.nextDouble() * 2.0D - 1.0D) * radius);
			BlockPos base = new BlockPos(x, (int) Math.floor(boss.posY), z);

			for (int dy = 2; dy >= -3; dy--) {
				BlockPos p = base.up(dy);
				if (w.isAirBlock(p) && w.isAirBlock(p.up()) && w.getBlockState(p.down()).getMaterial().blocksMovement()) {
					minion.setLocationAndAngles(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, w.rand.nextFloat() * 360.0F, 0.0F);
					if (w.getCollisionBoxes(minion, minion.getEntityBoundingBox()).isEmpty()) {
						minion.onInitialSpawn(w.getDifficultyForLocation(p), (IEntityLivingData) null);
						w.spawnEntity(minion);
						minion.spawnExplosionParticle();
						if (target != null) {
							minion.setAttackTarget(target);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Show a message in the action bar of all players near the boss
	 */
	public static void notifyNearbyPlayers(EntityLivingBase boss, ITextComponent message, double range) {
		List<EntityPlayer> players = boss.world.getEntitiesWithinAABB(EntityPlayer.class, boss.getEntityBoundingBox().grow(range));
		for (EntityPlayer ply : players) {
			ply.sendStatusMessage(message, true);
		}
	}
}
