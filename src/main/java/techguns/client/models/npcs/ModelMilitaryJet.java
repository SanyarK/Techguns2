package techguns.client.models.npcs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Ground attack jet, nose points to -Z like all entity models
 */
@SideOnly(Side.CLIENT)
public class ModelMilitaryJet extends ModelBase {

	protected static final float PITCH_PIVOT_Y = 17.0F / 16.0F;

	ModelRenderer fuselage;
	ModelRenderer nose;
	ModelRenderer cockpit;
	ModelRenderer wings;
	ModelRenderer tailWings;
	ModelRenderer fin;
	ModelRenderer exhaust;
	ModelRenderer missileLeft;
	ModelRenderer missileRight;

	public ModelMilitaryJet() {
		this.textureWidth = 256;
		this.textureHeight = 128;

		this.fuselage = new ModelRenderer(this, 0, 0);
		this.fuselage.addBox(-3.0F, 14.0F, -20.0F, 6, 6, 40);

		this.nose = new ModelRenderer(this, 96, 0);
		this.nose.addBox(-2.0F, 15.0F, -26.0F, 4, 4, 6);

		this.cockpit = new ModelRenderer(this, 120, 0);
		this.cockpit.addBox(-2.0F, 11.0F, -16.0F, 4, 3, 9);

		this.fin = new ModelRenderer(this, 150, 0);
		this.fin.addBox(-0.5F, 6.0F, 12.0F, 1, 8, 8);

		this.exhaust = new ModelRenderer(this, 172, 0);
		this.exhaust.addBox(-2.5F, 14.5F, 20.0F, 5, 5, 2);

		this.missileLeft = new ModelRenderer(this, 192, 0);
		this.missileLeft.addBox(-13.0F, 18.0F, -5.0F, 2, 2, 10);

		this.missileRight = new ModelRenderer(this, 192, 0);
		this.missileRight.addBox(11.0F, 18.0F, -5.0F, 2, 2, 10);

		this.wings = new ModelRenderer(this, 0, 48);
		this.wings.addBox(-22.0F, 17.0F, -6.0F, 44, 1, 14);

		this.tailWings = new ModelRenderer(this, 120, 48);
		this.tailWings.addBox(-10.0F, 16.0F, 14.0F, 20, 1, 6);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		GlStateManager.pushMatrix();
		//tilt the nose with the pitch
		GlStateManager.translate(0.0F, PITCH_PIVOT_Y, 0.0F);
		GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.translate(0.0F, -PITCH_PIVOT_Y, 0.0F);

		this.fuselage.render(scale);
		this.nose.render(scale);
		this.cockpit.render(scale);
		this.fin.render(scale);
		this.exhaust.render(scale);
		this.missileLeft.render(scale);
		this.missileRight.render(scale);
		this.wings.render(scale);
		this.tailWings.render(scale);

		GlStateManager.popMatrix();
	}
}
