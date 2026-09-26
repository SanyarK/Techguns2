package techguns.client.models.npcs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Simple parachute canopy with 4 lines, rendered above biped models
 */
@SideOnly(Side.CLIENT)
public class ModelParachute extends ModelBase {

	protected static final float CANOPY_Y = -34.0F;

	ModelRenderer canopy;
	ModelRenderer canopyTop;
	ModelRenderer[] lines = new ModelRenderer[4];

	public ModelParachute() {
		this.textureWidth = 128;
		this.textureHeight = 64;

		this.canopy = new ModelRenderer(this, 0, 0);
		this.canopy.addBox(-12.0F, CANOPY_Y, -12.0F, 24, 3, 24);

		this.canopyTop = new ModelRenderer(this, 0, 32);
		this.canopyTop.addBox(-8.0F, CANOPY_Y - 3.0F, -8.0F, 16, 3, 16);

		float[][] corners = {{10.0F, 10.0F}, {-10.0F, 10.0F}, {10.0F, -10.0F}, {-10.0F, -10.0F}};
		for (int i = 0; i < 4; i++) {
			float cx = corners[i][0];
			float cz = corners[i][1];
			//line goes from the canopy corner down to the shoulders
			float sx = Math.signum(cx) * 4.0F;
			float sz = Math.signum(cz) * 2.0F;
			float dx = sx - cx;
			float dy = 0.0F - (CANOPY_Y + 3.0F);
			float dz = sz - cz;
			float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

			ModelRenderer line = new ModelRenderer(this, 64, 32);
			line.addBox(-0.5F, 0.0F, -0.5F, 1, (int) len, 1);
			line.setRotationPoint(cx, CANOPY_Y + 3.0F, cz);
			line.rotateAngleZ = (float) Math.asin(-dx / len);
			line.rotateAngleX = (float) Math.asin(dz / len);
			this.lines[i] = line;
		}
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		this.canopy.render(scale);
		this.canopyTop.render(scale);
		for (ModelRenderer line : this.lines) {
			line.render(scale);
		}
	}
}
