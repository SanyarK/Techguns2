package techguns.campaign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import techguns.Techguns;

/**
 * One row of the mission table ({@link CampaignMissions}): number, act, objective type and its
 * parameters, place (site and distance), reward, dialog keys and where it is turned in.
 * Built with {@link #mission(int, int, ObjectiveType)} and the builder methods.
 */
public class CampaignMission {

	public enum ObjectiveType {
		/** talk to the commander (radio or in person) */
		TALK,
		/** get to a place */
		REACH,
		/** kill a number of enemies of a category */
		KILL,
		/** search places (stashes) or gather quest items */
		COLLECT,
		/** bring a quest item to a place */
		DELIVER,
		/** free captives and bring them to the post or an extraction point */
		ESCORT,
		/** fight off waves of enemies around a point */
		DEFEND,
		/** destroy target blocks (and optionally a garrison) */
		DESTROY,
		/** stay alive near a point until the timer runs out */
		SURVIVE,
		/** kill a boss, optionally take an item from him */
		BOSS,
		/** take a quest item from a place */
		ITEM;
	}

	/** point the objective distance is measured from */
	public enum Anchor {
		/** where the player accepts the mission */
		PLAYER,
		/** the start bunker (or the home of the player in normal worlds) */
		HOME,
		/** the command post */
		POST;
	}

	/** where captives have to be brought */
	public enum Destination {
		/** to the commander at the command post */
		POST,
		/** to an extraction point placed near the site */
		EVAC,
		/** the captive waits at the command post and has to be brought to the objective site */
		SITE;
	}

	/**
	 * A group of enemies of one wave
	 */
	public static class Group {
		public final CampaignTarget target;
		public final int count;
		/** dropped from the sky on parachutes */
		public final boolean airdrop;

		protected Group(CampaignTarget target, int count, boolean airdrop) {
			this.target = target;
			this.count = count;
			this.airdrop = airdrop;
		}

		public static Group of(CampaignTarget target, int count) {
			return new Group(target, count, false);
		}

		public static Group air(CampaignTarget target, int count) {
			return new Group(target, count, true);
		}
	}

	public static class Wave {
		public final Group[] groups;

		protected Wave(Group[] groups) {
			this.groups = groups;
		}

		public static Wave of(Group... groups) {
			return new Wave(groups);
		}

		public int size() {
			int n = 0;
			for (Group g : this.groups) {
				n += g.count;
			}
			return n;
		}
	}

	public static class Reward {
		/** loot table rolled once per roll, null = none */
		@Nullable
		public final ResourceLocation loot;
		public final int lootRolls;
		public final int xp;
		public final List<Supplier<ItemStack>> items;

		protected Reward(@Nullable ResourceLocation loot, int lootRolls, int xp, List<Supplier<ItemStack>> items) {
			this.loot = loot;
			this.lootRolls = lootRolls;
			this.xp = xp;
			this.items = items;
		}

		@SafeVarargs
		public static Reward of(@Nullable String loot, int lootRolls, int xp, Supplier<ItemStack>... items) {
			return new Reward(loot == null ? null : new ResourceLocation(Techguns.MODID, loot), lootRolls, xp, Arrays.asList(items));
		}
	}

	public final int id;
	public final int act;
	/** difficulty 1-5 */
	public final int stars;
	public final ObjectiveType type;

	/** KILL / BOSS: what counts, DEFEND / SURVIVE: who attacks */
	@Nullable
	public final CampaignTarget target;
	/** required amount: kills, stashes, items, captives, targets */
	public final int count;
	/** second kill objective, e.g. the garrison of a DESTROY mission */
	@Nullable
	public final CampaignTarget target2;
	public final int count2;

	public final CampaignSite site;
	/** different site types for the objective points, null = all points use {@link #site} */
	@Nullable
	public final CampaignSite[] siteList;
	public final Anchor anchor;
	public final int minDistance;
	public final int maxDistance;
	/** number of objective points (e.g. three houses) */
	public final int sites;
	/** name under which the site is remembered, later missions with the same key come back to it */
	@Nullable
	public final String siteKey;
	/** KILL: only kills within this distance of the anchor count, 0 = anywhere */
	public final int area;

	@Nullable
	public final Supplier<Item> questItem;
	public final boolean giveOnAccept;
	/** COLLECT: the quest item drops from these enemies */
	@Nullable
	public final CampaignTarget dropFrom;
	public final float dropChance;
	/** COLLECT: one different quest item per objective point, found in the chest of the point, null = none */
	@Nullable
	public final List<Supplier<Item>> components;

	public final List<Wave> waves;
	public final boolean atNight;
	public final int seconds;

	/** enemies sent after the player while the mission runs */
	@Nullable
	public final CampaignTarget raids;
	public final int raidInterval;
	public final int raidSize;

	public final Destination destination;
	public final Reward reward;
	/** true = has to be turned in at the commander in person, false = by radio */
	public final boolean inPerson;
	/** false = only in the table, implemented in a later update */
	public final boolean playable;
	/** prefix of the dialog keys: title, brief, goal, hint, done, reward */
	public final String key;

	protected CampaignMission(Builder b) {
		this.id = b.id;
		this.act = b.act;
		this.stars = b.act;
		this.type = b.type;
		this.target = b.target;
		this.count = b.count;
		this.target2 = b.target2;
		this.count2 = b.count2;
		this.site = b.site;
		this.siteList = b.siteList;
		this.anchor = b.anchor;
		this.minDistance = b.minDistance;
		this.maxDistance = b.maxDistance;
		this.sites = b.sites;
		this.siteKey = b.siteKey;
		this.area = b.area;
		this.questItem = b.questItem;
		this.giveOnAccept = b.giveOnAccept;
		this.dropFrom = b.dropFrom;
		this.dropChance = b.dropChance;
		this.components = b.components;
		this.waves = Collections.unmodifiableList(b.waves);
		this.atNight = b.atNight;
		this.seconds = b.seconds;
		this.raids = b.raids;
		this.raidInterval = b.raidInterval;
		this.raidSize = b.raidSize;
		this.destination = b.destination;
		this.reward = b.reward;
		this.inPerson = b.inPerson;
		this.playable = b.playable;
		this.key = Techguns.MODID + ".campaign." + (b.key != null ? b.key : String.format("m%02d", b.id));
	}

	public static Builder mission(int id, int act, ObjectiveType type) {
		return new Builder(id, act, type);
	}

	public String getTitleKey() {
		return this.key + ".title";
	}

	public String getBriefKey() {
		return this.key + ".brief";
	}

	public String getGoalKey() {
		return this.key + ".goal";
	}

	public String getHintKey() {
		return this.key + ".hint";
	}

	public String getDoneKey() {
		return this.key + ".done";
	}

	public String getRewardKey() {
		return this.key + ".reward";
	}

	public String getActKey() {
		return Techguns.MODID + ".campaign.act" + this.act;
	}

	/**
	 * the item of COLLECT / DELIVER / ITEM / BOSS objectives, null if there is none
	 */
	@Nullable
	public Item getQuestItem() {
		return this.questItem == null ? null : this.questItem.get();
	}

	/** site type of the objective point with the given index */
	public CampaignSite getSite(int index) {
		if (this.siteList != null && index >= 0 && index < this.siteList.length) {
			return this.siteList[index];
		}
		return this.site;
	}

	/**
	 * COLLECT with components: the quest item that lies at the objective point with the given index
	 */
	@Nullable
	public Item getComponent(int index) {
		if (this.components == null || index < 0 || index >= this.components.size()) {
			return null;
		}
		return this.components.get(index).get();
	}

	public boolean hasComponents() {
		return this.components != null && !this.components.isEmpty();
	}

	/** number of objective points */
	public int getSiteCount() {
		return this.siteList != null ? this.siteList.length : this.sites;
	}

	public boolean isDefense() {
		return this.type == ObjectiveType.DEFEND || this.type == ObjectiveType.SURVIVE;
	}

	/** the total amount the progress bar in the dialog counts to */
	public int getRequired() {
		if (this.type == ObjectiveType.DEFEND) {
			//charging missions count the charge in percent
			return this.seconds > 0 ? 100 : this.waves.size();
		}
		if (this.type == ObjectiveType.SURVIVE) {
			return this.seconds;
		}
		return this.count;
	}

	public static class Builder {
		protected final int id;
		protected final int act;
		protected final ObjectiveType type;
		protected CampaignTarget target;
		protected int count = 1;
		protected CampaignTarget target2;
		protected int count2 = 0;
		protected CampaignSite site = CampaignSite.NONE;
		protected CampaignSite[] siteList;
		protected Anchor anchor = Anchor.PLAYER;
		protected int minDistance = 0;
		protected int maxDistance = 0;
		protected int sites = 1;
		protected String siteKey;
		protected int area = 0;
		protected Supplier<Item> questItem;
		protected boolean giveOnAccept = false;
		protected CampaignTarget dropFrom;
		protected float dropChance = 0f;
		protected List<Supplier<Item>> components;
		protected final List<Wave> waves = new ArrayList<>();
		protected boolean atNight = false;
		protected int seconds = 0;
		protected CampaignTarget raids;
		protected int raidInterval = 30;
		protected int raidSize = 2;
		protected Destination destination = Destination.POST;
		protected Reward reward = Reward.of(null, 0, 0);
		protected boolean inPerson = false;
		protected boolean playable = true;
		protected String key;

		protected Builder(int id, int act, ObjectiveType type) {
			this.id = id;
			this.act = act;
			this.type = type;
		}

		/** kill objective / attackers: category and amount */
		public Builder target(CampaignTarget target, int count) {
			this.target = target;
			this.count = count;
			return this;
		}

		/** required amount of stashes, items, captives or targets */
		public Builder count(int count) {
			this.count = count;
			return this;
		}

		/** additional kills required, e.g. the garrison of a base that has to be destroyed */
		public Builder alsoKill(CampaignTarget target, int count) {
			this.target2 = target;
			this.count2 = count;
			return this;
		}

		/** place of the objective: site type and distance range from the anchor */
		public Builder at(CampaignSite site, Anchor anchor, int minDistance, int maxDistance) {
			this.site = site;
			this.anchor = anchor;
			this.minDistance = minDistance;
			this.maxDistance = maxDistance;
			return this;
		}

		/** several different places, one objective point each */
		public Builder places(Anchor anchor, int minDistance, int maxDistance, CampaignSite... sites) {
			this.at(sites[0], anchor, minDistance, maxDistance);
			this.siteList = sites;
			return this;
		}

		/** anchor only (DEFEND at home / at the post, KILL around the post) */
		public Builder around(Anchor anchor) {
			this.anchor = anchor;
			return this;
		}

		/** number of objective points of the site type */
		public Builder sites(int sites) {
			this.sites = sites;
			return this;
		}

		/** remember the site under a name, later missions with the same name come back to it */
		public Builder remember(String siteKey) {
			this.siteKey = siteKey;
			return this;
		}

		/** kills only count within this distance of the anchor */
		public Builder area(int area) {
			this.area = area;
			return this;
		}

		public Builder item(Supplier<Item> item) {
			this.questItem = item;
			return this;
		}

		/** the quest item is handed out when the mission is accepted */
		public Builder giveOnAccept() {
			this.giveOnAccept = true;
			return this;
		}

		/** the quest item drops from these enemies */
		public Builder dropFrom(CampaignTarget target, float chance) {
			this.dropFrom = target;
			this.dropChance = chance;
			return this;
		}

		/** COLLECT: one quest item per place (in the order of {@link #places}), each lies in the chest of its place */
		@SafeVarargs
		public final Builder components(Supplier<Item>... items) {
			this.components = Arrays.asList(items);
			this.count = items.length;
			return this;
		}

		public Builder waves(Wave... waves) {
			this.waves.addAll(Arrays.asList(waves));
			return this;
		}

		/** the attack only starts at night */
		public Builder atNight() {
			this.atNight = true;
			return this;
		}

		/** SURVIVE: length of the fight, DEFEND: charging time, the waves come one after another while it runs */
		public Builder seconds(int seconds) {
			this.seconds = seconds;
			return this;
		}

		/** sends small groups of enemies after the player every 'interval' seconds */
		public Builder raids(CampaignTarget target, int interval, int size) {
			this.raids = target;
			this.raidInterval = interval;
			this.raidSize = size;
			return this;
		}

		public Builder escortTo(Destination destination) {
			this.destination = destination;
			return this;
		}

		public Builder reward(Reward reward) {
			this.reward = reward;
			return this;
		}

		/** has to be turned in at the commander in person */
		public Builder inPerson() {
			this.inPerson = true;
			return this;
		}

		/** only listed, implemented in a later update */
		public Builder planned() {
			this.playable = false;
			return this;
		}

		public Builder dialog(String key) {
			this.key = key;
			return this;
		}

		public CampaignMission build() {
			return new CampaignMission(this);
		}
	}
}
