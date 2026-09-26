package techguns.client.render.entities.npcs;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import techguns.Techguns;
import techguns.client.models.npcs.ModelMilitaryJet;
import techguns.entities.npcs.MilitaryJet;

public class RenderMilitaryJet extends RenderLiving<MilitaryJet> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Techguns.MODID,"textures/entity/military_jet.png");

	protected static final float SCALE = 2.0f;

	public RenderMilitaryJet(RenderManager rendermanagerIn) {
		super(rendermanagerIn, new ModelMilitaryJet(), 3.0f);
	}

	@Override
	protected void preRenderCallback(MilitaryJet entitylivingbaseIn, float partialTickTime) {
		GlStateManager.scale(SCALE, SCALE, SCALE);
	}

	@Override
	protected ResourceLocation getEntityTexture(MilitaryJet entity) {
		return TEXTURE;
	}

}
