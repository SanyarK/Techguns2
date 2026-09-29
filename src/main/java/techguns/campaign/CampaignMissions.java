package techguns.campaign;

import static techguns.campaign.CampaignMission.mission;
import static techguns.campaign.CampaignMission.Anchor.HOME;
import static techguns.campaign.CampaignMission.Anchor.POST;
import static techguns.campaign.CampaignMission.ObjectiveType.BOSS;
import static techguns.campaign.CampaignMission.ObjectiveType.COLLECT;
import static techguns.campaign.CampaignMission.ObjectiveType.DEFEND;
import static techguns.campaign.CampaignMission.ObjectiveType.DELIVER;
import static techguns.campaign.CampaignMission.ObjectiveType.DESTROY;
import static techguns.campaign.CampaignMission.ObjectiveType.ESCORT;
import static techguns.campaign.CampaignMission.ObjectiveType.ITEM;
import static techguns.campaign.CampaignMission.ObjectiveType.KILL;
import static techguns.campaign.CampaignMission.ObjectiveType.REACH;
import static techguns.campaign.CampaignMission.ObjectiveType.TALK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import techguns.TGArmors;
import techguns.TGItems;
import techguns.TGuns;
import techguns.campaign.CampaignMission.Destination;
import techguns.campaign.CampaignMission.Group;
import techguns.campaign.CampaignMission.Reward;
import techguns.campaign.CampaignMission.Wave;

/**
 * The mission table of the story campaign "Dawn": 30 missions in 5 acts.
 * The story and the same table in readable form are in CAMPAIGN.md in the repository root.
 * Missions marked planned() would be listed in the journal only, all 30 missions are playable.
 */
public class CampaignMissions {

	protected static final List<CampaignMission> MISSIONS = new ArrayList<>();

	public static final int ACTS = 5;

	static {
		/*
		 * ACT I "Awakening" - zombies and bandits around the start bunker, 100-350 blocks
		 */
		add(mission(1, 1, TALK)
				.reward(Reward.of("campaign/reward_act1", 1, 30, stack(() -> TGItems.RADIO, 1), stack(() -> TGItems.BANDAGE, 4),
						stack(() -> Items.BREAD, 6), shared(() -> TGItems.PISTOL_MAGAZINE, 4), stack(() -> Item.getItemFromBlock(net.minecraft.init.Blocks.TORCH), 16))));
		add(mission(2, 1, COLLECT).count(3)
				.at(CampaignSite.STASH_HOUSE, HOME, 60, 130).sites(3)
				.reward(Reward.of("campaign/reward_act1", 1, 60)));
		add(mission(3, 1, DEFEND).around(HOME).atNight()
				.waves(Wave.of(Group.of(CampaignTarget.ZOMBIE, 4)),
						Wave.of(Group.of(CampaignTarget.ZOMBIE, 4), Group.of(CampaignTarget.ZOMBIE_SOLDIER, 2)),
						Wave.of(Group.of(CampaignTarget.ZOMBIE, 3), Group.of(CampaignTarget.ZOMBIE_SOLDIER, 5)))
				.reward(Reward.of("campaign/reward_act1", 1, 120, stack(() -> TGItems.COMBAT_KNIFE, 1),
						stack(() -> TGArmors.t1_combat_Helmet, 1), stack(() -> TGArmors.t1_combat_Chestplate, 1))));
		add(mission(4, 1, KILL).target(CampaignTarget.BANDIT, 5)
				.at(CampaignSite.BANDIT_CAMP, HOME, 120, 200)
				.reward(Reward.of("campaign/reward_act1", 1, 150, stack(() -> TGuns.revolver, 1), shared(() -> TGItems.PISTOL_ROUNDS, 24))));
		add(mission(5, 1, DELIVER).item(() -> TGItems.ANTENNA_PARTS).giveOnAccept()
				.at(CampaignSite.RADIO_MAST, HOME, 180, 260)
				.reward(Reward.of("campaign/reward_act1", 1, 180, stack(() -> TGItems.GPS_NAVIGATOR, 1))));
		add(mission(6, 1, REACH).at(CampaignSite.COMMAND_POST, HOME, 250, 350).inPerson()
				.reward(Reward.of("campaign/reward_act1", 2, 250, stack(() -> TGArmors.t1_combat_Leggings, 1), stack(() -> TGArmors.t1_combat_Boots, 1),
						stack(() -> TGuns.ak47, 1), shared(() -> TGItems.ASSAULTRIFLE_MAGAZINE, 4))));

		/*
		 * ACT II "Resistance" - raiders and the first Legion soldiers, jets, 150-550 blocks from the post
		 */
		add(mission(7, 2, KILL).target(CampaignTarget.HOSTILE, 10).around(POST).area(160)
				.raids(CampaignTarget.RAIDER, 40, 3)
				.reward(Reward.of("campaign/reward_act2", 1, 250)));
		add(mission(8, 2, COLLECT).item(() -> TGItems.MEDICINE).count(3)
				.at(CampaignSite.HOSPITAL, POST, 300, 400).inPerson()
				.reward(Reward.of("campaign/reward_act2", 1, 300, stack(() -> TGItems.BANDAGE, 6))));
		add(mission(9, 2, REACH).at(CampaignSite.AIRFIELD, POST, 400, 500).remember("airfield")
				.reward(Reward.of("campaign/reward_act2", 1, 300)));
		add(mission(10, 2, KILL).target(CampaignTarget.JET, 2)
				.raids(CampaignTarget.JET, 30, 1)
				.reward(Reward.of("campaign/reward_act2", 2, 400, shared(() -> TGItems.ROCKET, 4))));
		add(mission(11, 2, ESCORT).count(1).at(CampaignSite.SCIENTIST_BUNKER, POST, 450, 550)
				.escortTo(Destination.POST).inPerson()
				.reward(Reward.of("campaign/reward_act2", 2, 450, stack(() -> TGItems.MILITARY_CONTRACT, 1))));
		add(mission(12, 2, DEFEND).around(POST)
				.waves(Wave.of(Group.of(CampaignTarget.LEGION, 5)),
						Wave.of(Group.air(CampaignTarget.PARATROOPER, 4), Group.of(CampaignTarget.LEGION, 2)),
						Wave.of(Group.of(CampaignTarget.LEGION_ELITE, 4), Group.of(CampaignTarget.LEGION, 2)))
				.inPerson()
				.reward(Reward.of("campaign/reward_act2", 2, 600, stack(() -> TGArmors.t2_combat_Helmet, 1), stack(() -> TGArmors.t2_combat_Chestplate, 1),
						stack(() -> TGArmors.t2_combat_Leggings, 1), stack(() -> TGArmors.t2_combat_Boots, 1),
						stack(() -> TGuns.m4, 1), shared(() -> TGItems.ASSAULTRIFLE_MAGAZINE, 6))));

		/*
		 * ACT III "Legion" - elite, heavy soldiers and the General, 550-1000 blocks
		 */
		add(mission(13, 3, DESTROY).count(4).at(CampaignSite.MILITARY_BASE, POST, 550, 700)
				.reward(Reward.of("campaign/reward_act3", 1, 600)));
		add(mission(14, 3, KILL).target(CampaignTarget.LEGION_ELITE, 6).at(CampaignSite.CONVOY, POST, 600, 750)
				.reward(Reward.of("campaign/reward_act3", 1, 650)));
		add(mission(15, 3, REACH).at(CampaignSite.UNDERGROUND_MINE, POST, 650, 800)
				.reward(Reward.of("campaign/reward_act3", 1, 700)));
		add(mission(16, 3, BOSS).target(CampaignTarget.GENERAL, 1).item(() -> TGItems.INTEL_DOCUMENTS)
				.at(CampaignSite.GENERAL_BUNKER, POST, 750, 900).inPerson()
				.reward(Reward.of("campaign/reward_act3", 2, 900, stack(() -> TGItems.MILITARY_CONTRACT, 2))));
		add(mission(17, 3, ESCORT).count(3).at(CampaignSite.PRISON_CAMP, POST, 800, 950)
				.escortTo(Destination.EVAC)
				.reward(Reward.of("campaign/reward_act3", 2, 900)));
		add(mission(18, 3, DESTROY).count(3).alsoKill(CampaignTarget.LEGION, 8)
				.at(CampaignSite.AIRFIELD, POST, 900, 1000).remember("airfield").inPerson()
				.reward(Reward.of("campaign/reward_act3", 3, 1200, stack(() -> TGArmors.t3_combat_Helmet, 1), stack(() -> TGArmors.t3_combat_Chestplate, 1),
						stack(() -> TGArmors.t3_combat_Leggings, 1), stack(() -> TGArmors.t3_combat_Boots, 1),
						stack(() -> TGuns.scar, 1), shared(() -> TGItems.ASSAULTRIFLE_MAGAZINE, 8))));

		/*
		 * ACT IV "Plague" - mutants, the Warlord and the Prototype, 900-1250 blocks
		 */
		add(mission(19, 4, COLLECT).item(() -> TGItems.TISSUE_SAMPLE).count(5).dropFrom(CampaignTarget.MUTANT_WARRIOR, 0.5f)
				.at(CampaignSite.MUTANT_ZONE, POST, 900, 1050)
				.reward(Reward.of("campaign/reward_act4", 1, 1000, stack(() -> TGuns.lasergun, 1), shared(() -> TGItems.ENERGY_CELL, 6))));
		add(mission(20, 4, BOSS).target(CampaignTarget.WARLORD, 1).at(CampaignSite.MUTANT_LAIR, POST, 1000, 1150)
				.reward(Reward.of("campaign/reward_act4", 2, 1200, stack(() -> TGuns.grenadelauncher, 1), shared(() -> TGItems.GRENADE_40MM, 16))));
		add(mission(21, 4, REACH).at(CampaignSite.MUTAGEN_LAB, POST, 1100, 1250).remember("lab")
				.reward(Reward.of("campaign/reward_act4", 1, 1000, stack(() -> TGItems.BANDAGE, 6))));
		add(mission(22, 4, ITEM).item(() -> TGItems.MUTAGEN_SAMPLE).at(CampaignSite.MUTAGEN_LAB, POST, 1100, 1250).remember("lab")
				.reward(Reward.of("campaign/reward_act4", 1, 1100, stack(() -> TGItems.RAD_AWAY, 2))));
		add(mission(23, 4, BOSS).target(CampaignTarget.PROTOTYPE, 1).at(CampaignSite.MUTAGEN_LAB, POST, 1100, 1250).remember("lab")
				.reward(Reward.of("campaign/reward_act4", 2, 1500, stack(() -> TGuns.gaussrifle, 1), shared(() -> TGItems.GAUSSRIFLE_SLUGS, 16))));
		add(mission(24, 4, DEFEND).around(POST)
				.waves(Wave.of(Group.of(CampaignTarget.MUTANT, 6)), Wave.of(Group.of(CampaignTarget.MUTANT, 8)),
						Wave.of(Group.of(CampaignTarget.MUTANT_WARRIOR, 6)), Wave.of(Group.of(CampaignTarget.MUTANT_ELITE, 6)),
						Wave.of(Group.of(CampaignTarget.MUTANT_ELITE, 4), Group.of(CampaignTarget.MUTANT_WARRIOR, 6)))
				.inPerson()
				.reward(Reward.of("campaign/reward_act4", 3, 2000, stack(() -> TGArmors.t3_power_Helmet, 1), stack(() -> TGArmors.t3_power_Chestplate, 1),
						stack(() -> TGArmors.t3_power_Leggings, 1), stack(() -> TGArmors.t3_power_Boots, 1), shared(() -> TGItems.ENERGY_CELL, 8),
						stack(() -> TGuns.minigun, 1), shared(() -> TGItems.MINIGUN_DRUM, 4))));

		/*
		 * ACT V "Dawn" - the Purifier, the Hive and Chimera, 1200-1500 blocks
		 */
		add(mission(25, 5, COLLECT)
				.places(POST, 1200, 1500, CampaignSite.REACTOR_RUINS, CampaignSite.LEGION_HQ, CampaignSite.MUTAGEN_LAB)
				.components(() -> TGItems.REACTOR_CORE, () -> TGItems.CONTROL_MODULE, () -> TGItems.PURIFIER_FILTER).inPerson()
				.reward(Reward.of("campaign/reward_act5", 2, 2000, stack(() -> TGuns.pulserifle, 1), shared(() -> TGItems.ADVANCED_MAGAZINE, 6))));
		add(mission(26, 5, ESCORT).count(1).at(CampaignSite.LAUNCH_SITE, POST, 1300, 1500).remember("launch")
				.escortTo(Destination.SITE).raids(CampaignTarget.MUTANT, 45, 4)
				.reward(Reward.of("campaign/reward_act5", 2, 2000, stack(() -> TGuns.guidedmissilelauncher, 1), shared(() -> TGItems.ROCKET, 8))));
		add(mission(27, 5, DEFEND).at(CampaignSite.LAUNCH_SITE, POST, 1300, 1500).remember("launch").seconds(180)
				.waves(Wave.of(Group.of(CampaignTarget.MUTANT, 8)), Wave.of(Group.of(CampaignTarget.MUTANT_ELITE, 6), Group.of(CampaignTarget.JET, 1)),
						Wave.of(Group.of(CampaignTarget.MUTANT_ELITE, 6), Group.of(CampaignTarget.MUTANT_WARRIOR, 6), Group.of(CampaignTarget.JET, 2)))
				.reward(Reward.of("campaign/reward_act5", 2, 2500, shared(() -> TGItems.ENERGY_CELL, 8), stack(() -> TGItems.BANDAGE, 8))));
		add(mission(28, 5, REACH).at(CampaignSite.HIVE, POST, 1400, 1500).remember("hive")
				.reward(Reward.of("campaign/reward_act5", 1, 2000)));
		add(mission(29, 5, BOSS).target(CampaignTarget.CHIMERA, 1).at(CampaignSite.HIVE, POST, 1400, 1500).remember("hive")
				.reward(Reward.of("campaign/reward_act5", 3, 5000)));
		add(mission(30, 5, TALK).inPerson()
				.reward(Reward.of("campaign/reward_act5", 3, 5000, HeroItems.hero(() -> TGArmors.t4_power_Helmet, "helmet"),
						HeroItems.hero(() -> TGArmors.t4_power_Chestplate, "chestplate"), HeroItems.hero(() -> TGArmors.t4_power_Leggings, "leggings"),
						HeroItems.hero(() -> TGArmors.t4_power_Boots, "boots"), HeroItems.hero(() -> TGuns.goldenrevolver, "revolver"),
						HeroItems.hero(() -> TGuns.gaussrifle, "rifle"), shared(() -> TGItems.ENERGY_CELL, 12), shared(() -> TGItems.GAUSSRIFLE_SLUGS, 24),
						shared(() -> TGItems.PISTOL_ROUNDS, 32))));
	}

	protected static void add(CampaignMission.Builder builder) {
		MISSIONS.add(builder.build());
	}

	protected static Supplier<ItemStack> stack(Supplier<Item> item, int count) {
		return () -> {
			Item i = item.get();
			return i == null ? ItemStack.EMPTY : new ItemStack(i, count);
		};
	}

	protected static Supplier<ItemStack> shared(Supplier<ItemStack> stack, int count) {
		return () -> {
			ItemStack s = stack.get();
			return s == null || s.isEmpty() ? ItemStack.EMPTY : TGItems.newStack(s, count);
		};
	}

	public static List<CampaignMission> all() {
		return Collections.unmodifiableList(MISSIONS);
	}

	@Nullable
	public static CampaignMission byId(int id) {
		return id >= 1 && id <= MISSIONS.size() ? MISSIONS.get(id - 1) : null;
	}

	/** number of the last mission of the story */
	public static int last() {
		return MISSIONS.size();
	}

	/** number of the last mission that can be played in this version */
	public static int lastPlayable() {
		int last = 0;
		for (CampaignMission m : MISSIONS) {
			if (!m.playable) {
				break;
			}
			last = m.id;
		}
		return last;
	}

	public static List<CampaignMission> ofAct(int act) {
		List<CampaignMission> list = new ArrayList<>();
		for (CampaignMission m : MISSIONS) {
			if (m.act == act) {
				list.add(m);
			}
		}
		return list;
	}
}
