package techguns.campaign;

import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.world.World;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.Bandit;
import techguns.entities.npcs.Commando;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.General;
import techguns.entities.npcs.GenericNPCUndead;
import techguns.entities.npcs.HeavySoldier;
import techguns.entities.npcs.MilitaryJet;
import techguns.entities.npcs.MutantWarlord;
import techguns.entities.npcs.MutantWarrior;
import techguns.entities.npcs.Paratrooper;
import techguns.entities.npcs.PrototypeBoss;
import techguns.entities.npcs.SuperMutantBasic;
import techguns.entities.npcs.SuperMutantElite;
import techguns.entities.npcs.SuperMutantHeavy;
import techguns.entities.npcs.ZombieFarmer;
import techguns.entities.npcs.ZombieMiner;
import techguns.entities.npcs.ZombiePoliceman;
import techguns.entities.npcs.ZombieSoldier;
import techguns.util.TGLogger;

/**
 * Enemy categories of the campaign: which kills count for an objective and which mobs
 * the campaign spawns for garrisons, waves and raids. The order of the categories follows
 * the difficulty curve of the story: zombies, bandits, Legion soldiers, elite, jets, bosses.
 */
public enum CampaignTarget {

	/** any hostile creature */
	HOSTILE(e -> e instanceof IMob,
			spawns(ZombieSoldier.class, 2, ZombieFarmer.class, 2, Bandit.class, 2, ZombieMiner.class, 1)),
	/** vanilla and Techguns zombies */
	ZOMBIE(e -> e instanceof EntityZombie || e instanceof GenericNPCUndead,
			spawns(EntityZombie.class, 3, ZombieFarmer.class, 3, ZombieMiner.class, 2)),
	/** the tougher undead for later night waves */
	ZOMBIE_SOLDIER(e -> e instanceof EntityZombie || e instanceof GenericNPCUndead,
			spawns(ZombieSoldier.class, 3, ZombiePoliceman.class, 2, ZombieMiner.class, 1)),
	BANDIT(e -> e instanceof Bandit,
			spawns(Bandit.class, 1)),
	/** bandits and zombies roaming the wastes, for raids around the command post */
	RAIDER(e -> e instanceof IMob,
			spawns(Bandit.class, 3, ZombieSoldier.class, 2, ZombiePoliceman.class, 1)),
	/** soldiers of the Legion junta */
	LEGION(e -> e instanceof ArmySoldier || e instanceof Commando || e instanceof EliteSoldier || e instanceof HeavySoldier || e instanceof General,
			spawns(ArmySoldier.class, 4, Commando.class, 1)),
	PARATROOPER(e -> e instanceof Paratrooper,
			spawns(Paratrooper.class, 1)),
	/** elite and heavy soldiers of the Legion */
	LEGION_ELITE(e -> e instanceof EliteSoldier || e instanceof HeavySoldier,
			spawns(EliteSoldier.class, 1, HeavySoldier.class, 1)),
	HEAVY(e -> e instanceof HeavySoldier,
			spawns(HeavySoldier.class, 1)),
	JET(e -> e instanceof MilitaryJet,
			spawns(MilitaryJet.class, 1)),
	GENERAL(e -> e instanceof General,
			spawns(General.class, 1)),
	MUTANT(e -> e instanceof SuperMutantBasic,
			spawns(SuperMutantBasic.class, 3, SuperMutantHeavy.class, 1, MutantWarrior.class, 1)),
	MUTANT_WARRIOR(e -> e instanceof MutantWarrior,
			spawns(MutantWarrior.class, 1)),
	MUTANT_ELITE(e -> e instanceof SuperMutantElite && !(e instanceof MutantWarlord) && !(e instanceof PrototypeBoss),
			spawns(SuperMutantElite.class, 2, SuperMutantHeavy.class, 1)),
	WARLORD(e -> e instanceof MutantWarlord,
			spawns(MutantWarlord.class, 1)),
	PROTOTYPE(e -> e instanceof PrototypeBoss,
			spawns(PrototypeBoss.class, 1));

	protected final Predicate<EntityLivingBase> matcher;
	protected final Object[] spawnTable;

	CampaignTarget(Predicate<EntityLivingBase> matcher, Object[] spawnTable) {
		this.matcher = matcher;
		this.spawnTable = spawnTable;
	}

	/**
	 * class, weight, class, weight...
	 */
	protected static Object[] spawns(Object... classAndWeight) {
		return classAndWeight;
	}

	public boolean matches(EntityLivingBase entity) {
		return entity != null && this.matcher.test(entity);
	}

	public String getTranslationKey() {
		return "techguns.campaign.target." + this.name().toLowerCase();
	}

	/**
	 * Creates (but does not spawn) a random mob of this category
	 */
	public EntityLiving create(World world, Random rnd) {
		int total = 0;
		for (int i = 1; i < this.spawnTable.length; i += 2) {
			total += (Integer) this.spawnTable[i];
		}
		int roll = rnd.nextInt(Math.max(total, 1));
		Class<?> clazz = (Class<?>) this.spawnTable[0];
		for (int i = 0; i + 1 < this.spawnTable.length; i += 2) {
			roll -= (Integer) this.spawnTable[i + 1];
			if (roll < 0) {
				clazz = (Class<?>) this.spawnTable[i];
				break;
			}
		}
		try {
			return (EntityLiving) clazz.getConstructor(World.class).newInstance(world);
		} catch (Exception e) {
			TGLogger.logger_server.warning("Campaign could not create " + clazz.getName() + ": " + e);
			return null;
		}
	}
}
