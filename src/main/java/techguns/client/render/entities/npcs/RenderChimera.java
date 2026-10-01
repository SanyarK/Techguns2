package techguns.client.render.entities.npcs;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import techguns.Techguns;
import techguns.client.models.npcs.ModelChimera;
import techguns.client.render.TGRenderHelper;
import techguns.entities.npcs.ChimeraBoss;

/**
 * Renderer of the final boss: the model scaled up and a second pass with the glowing parts
 * (heart, eye, reactor vents, cannon muzzle) at full brightness.
 */
public class RenderChimera extends RenderLiving<ChimeraBoss> {

	public static final ResourceLocation TEXTURE = new ResourceLocation(Techguns.MODID, "textures/entity/chimera.png");
	public static final ResourceLocation GLOW = new ResourceLocation(Techguns.MODID, "textures/entity/chimera_glow.png");

	protected static final float SCALE = 1.4F;

	public RenderChimera(RenderManager renderManagerIn) {
		super(renderManagerIn, new ModelChimera(), 1.6F);
		this.addLayer(new LayerChimeraGlow(this));
	}

	@Override
	protected void preRenderCallback(ChimeraBoss entity, float partialTickTime) {
		GlStateManager.scale(SCALE, SCALE, SCALE);
	}

	@Override
	protected ResourceLocation getEntityTexture(ChimeraBoss entity) {
		return TEXTURE;
	}

	public static class LayerChimeraGlow implements LayerRenderer<ChimeraBoss> {

		protected final RenderChimera renderer;

		public LayerChimeraGlow(RenderChimera renderer) {
			this.renderer = renderer;
		}

		@Override
		public void doRenderLayer(ChimeraBoss entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			this.renderer.bindTexture(GLOW);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
			GlStateManager.depthMask(!entity.isInvisible());
			TGRenderHelper.enableFXLighting();
			float pulse = 0.7F + 0.3F * MathHelper.sin(ageInTicks * (entity.getPhase() == 3 ? 0.45F : 0.15F));
			GlStateManager.color(pulse, pulse, pulse, 1.0F);
			this.renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			TGRenderHelper.disableFXLighting();
			GlStateManager.depthMask(true);
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.disableBlend();
		}

		@Override
		public boolean shouldCombineTextures() {
			return false;
		}
	}
}
