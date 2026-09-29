package techguns.entities.npcs;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

/**
 * Prisoner of the Legion camp (act III of the campaign). Behaves like the captured scientist:
 * right click frees him, then he follows his rescuer to the extraction point.
 */
public class LegionPrisoner extends CapturedScientist {

	public LegionPrisoner(World world) {
		super(world);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32D);
	}

	@Override
	protected String getMessagePrefix() {
		return "prisoner";
	}
}
