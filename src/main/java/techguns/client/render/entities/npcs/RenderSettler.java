package techguns.client.render.entities.npcs;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import techguns.Techguns;
import techguns.entities.npcs.SettlerNPC;

/**
 * Settlers of the command post, one of three clothes per settler
 */
public class RenderSettler extends RenderTexturedSoldier<SettlerNPC> {

	protected static final ResourceLocation[] TEXTURES = new ResourceLocation[SettlerNPC.VARIANTS];

	static {
		for (int i = 0; i < TEXTURES.length; i++) {
			TEXTURES[i] = new ResourceLocation(Techguns.MODID, "textures/entity/settler_" + (i + 1) + ".png");
		}
	}

	public RenderSettler(RenderManager renderManagerIn) {
		super(renderManagerIn, TEXTURES[0]);
	}

	@Override
	protected ResourceLocation getEntityTexture(SettlerNPC entity) {
		return TEXTURES[entity.getVariant()];
	}
}
