package techguns.campaign;

import techguns.Techguns;

/**
 * The missions of the story campaign, in order. The mission logic itself is in {@link TGCampaign}.
 */
public enum CampaignMission {

	/** 1: reach the command post and report to the commander */
	CONTACT(1, ObjectiveType.REACH, 1, true),
	/** 2: eliminate hostiles near the post */
	BAPTISM(2, ObjectiveType.KILL, 6, false),
	/** 3: scout the airfield */
	RECON(3, ObjectiveType.REACH, 1, false),
	/** 4: shoot down 2 attack jets */
	CLEAR_SKIES(4, ObjectiveType.KILL, 2, false),
	/** 5: free the captured scientist from a bunker and bring him to the commander */
	RESCUE(5, ObjectiveType.ESCORT, 1, true),
	/** 6: kill the General and take his intel documents */
	SNAKE_HEAD(6, ObjectiveType.KILL, 1, false),
	/** 7: find the secret mutagen lab */
	FIND_LAB(7, ObjectiveType.REACH, 1, false),
	/** 8: take the mutagen sample from the lab safe */
	SAMPLE(8, ObjectiveType.ITEM, 1, false),
	/** 9: kill the Prototype boss */
	PROTOTYPE(9, ObjectiveType.KILL, 1, false),
	/** 10: return to the commander for the final ceremony */
	TRIUMPH(10, ObjectiveType.REACH, 1, true);

	public enum ObjectiveType {
		REACH, KILL, ESCORT, ITEM;
	}

	public final int id;
	public final ObjectiveType type;
	/** kill count or similar to reach */
	public final int required;
	/** true when the mission must be turned in at the commander NPC in person, false = radio is enough */
	public final boolean requiresCommander;

	CampaignMission(int id, ObjectiveType type, int required, boolean requiresCommander) {
		this.id = id;
		this.type = type;
		this.required = required;
		this.requiresCommander = requiresCommander;
	}

	public static CampaignMission byId(int id) {
		for (CampaignMission m : values()) {
			if (m.id == id) {
				return m;
			}
		}
		return null;
	}

	public String getTitleKey() {
		return Techguns.MODID + ".campaign.mission" + this.id + ".title";
	}

	public String getBriefKey() {
		return Techguns.MODID + ".campaign.mission" + this.id + ".brief";
	}

	public String getActiveKey() {
		return Techguns.MODID + ".campaign.mission" + this.id + ".active";
	}

	public String getDoneKey() {
		return Techguns.MODID + ".campaign.mission" + this.id + ".done";
	}
}
