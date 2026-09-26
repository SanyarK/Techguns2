package techguns.client.render.entities.npcs;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import techguns.entities.npcs.SuperMutantBasic;

/**
 * Super mutant renderer with a fixed skin texture (used by the Prototype boss)
 */
public class RenderTexturedMutant extends RenderSuperMutant {

	protected final ResourceLocation texture;

	public RenderTexturedMutant(RenderManager renderManagerIn, ResourceLocation texture) {
		super(renderManagerIn);
		this.texture = texture;
	}

	@Override
	protected ResourceLocation getEntityTexture(SuperMutantBasic entity) {
		return this.texture;
	}
}
