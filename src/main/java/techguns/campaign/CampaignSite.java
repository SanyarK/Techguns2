package techguns.campaign;

/**
 * Places of the campaign missions. The structures are built by {@link CampaignSiteBuilder}
 * when the player gets close to the objective point.
 */
public enum CampaignSite {

	/** no structure, e.g. a radio talk or a kill objective anywhere */
	NONE(0, false),
	/** the start bunker or the home of the player in normal worlds */
	HOME(24, false),
	/** a ruined house with a survivor stash */
	STASH_HOUSE(10, false),
	/** tents and a campfire of bandit scouts */
	BANDIT_CAMP(16, false),
	/** lattice radio mast with a broken antenna on top */
	RADIO_MAST(3, false),
	/** the command post of the Resistance with colonel Gromov */
	COMMAND_POST(12, true),
	/** two storey ruin of a hospital */
	HOSPITAL(10, false),
	/** Legion airfield, reused by the last mission of act III */
	AIRFIELD(40, true, "airfield"),
	/** Legion bunker where the scientist is held */
	SCIENTIST_BUNKER(16, false),
	/** Legion military base with a fuel depot */
	MILITARY_BASE(28, false),
	/** a Legion convoy stopped on an old road */
	CONVOY(24, false),
	/** underground mine with the Legion command center on its second level */
	UNDERGROUND_MINE(40, false),
	/** bunker of the General */
	GENERAL_BUNKER(16, false),
	/** fenced Legion prison camp */
	PRISON_CAMP(20, false),
	/** extraction point of the Resistance, prisoners are brought here */
	EVAC(8, false),
	/* ---------- act IV and V, see CAMPAIGN.md ---------- */
	/** wasteland area full of mutant warriors */
	MUTANT_ZONE(48, false),
	/** crater lair of the Mutant Warlord */
	MUTANT_LAIR(24, false),
	/** the secret underground mutagen laboratory of project Chimera */
	MUTAGEN_LAB(32, true, "lab"),
	/** ruins of the nuclear power plant, the reactor core lies in the reactor hall */
	REACTOR_RUINS(32, false),
	/** Legion headquarters: a fortified compound above a large bunker */
	LEGION_HQ(32, false),
	/** launch pad of the Purifier */
	LAUNCH_SITE(24, true, "launch"),
	/** the Hive in the center of the crater, the lair of Chimera deep below */
	HIVE(32, true, "hive");

	/** how close the player has to get for "reach" objectives */
	public final double reachRadius;
	/** true when later missions may come back to this place */
	public final boolean persistent;
	/** name under which missions with several places find this place again, null = never reused */
	public final String memory;

	CampaignSite(double reachRadius, boolean persistent) {
		this(reachRadius, persistent, null);
	}

	CampaignSite(double reachRadius, boolean persistent, String memory) {
		this.reachRadius = reachRadius;
		this.persistent = persistent;
		this.memory = memory;
	}

	public static CampaignSite byName(String name) {
		for (CampaignSite s : values()) {
			if (s.name().equals(name)) {
				return s;
			}
		}
		return NONE;
	}
}
