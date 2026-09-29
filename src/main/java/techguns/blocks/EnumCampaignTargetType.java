package techguns.blocks;

import net.minecraft.util.IStringSerializable;

/**
 * Objects the player has to destroy in campaign sabotage missions
 */
public enum EnumCampaignTargetType implements IStringSerializable {
	/** explodes a few seconds after it was damaged */
	FUEL_TANK,
	/** flight control console of the Legion airfield */
	FLIGHT_CONTROL;

	@Override
	public String getName() {
		return this.name().toLowerCase();
	}
}
