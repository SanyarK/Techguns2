package techguns.client.render.entities.npcs;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import techguns.Techguns;
import techguns.client.models.npcs.ModelParachute;
import techguns.entities.npcs.Paratrooper;

public class RenderParatrooper extends RenderTexturedSoldier<Paratrooper> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Techguns.MODID,"textures/entity/army_soldier.png");
	private static final ResourceLocation PARACHUTE_TEXTURE = new ResourceLocation(Techguns.MODID,"textures/entity/parachute.png");

	public RenderParatrooper(RenderManager renderManagerIn) {
		super(renderManagerIn, TEXTURE);
		this.addLayer(new LayerParachute(this));
	}

	public static class LayerParachute implements LayerRenderer<Paratrooper> {

		protected final RenderParatrooper renderer;
		protected final ModelParachute model = new ModelParachute();

		public LayerParachute(RenderParatrooper renderer) {
			this.renderer = renderer;
		}

		@Override
		public void doRenderLayer(Paratrooper entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
			if (entity.hasParachute()) {
				this.renderer.bindTexture(PARACHUTE_TEXTURE);
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
				GlStateManager.popMatrix();
			}
		}

		@Override
		public boolean shouldCombineTextures() {
			return false;
		}

	}
}
