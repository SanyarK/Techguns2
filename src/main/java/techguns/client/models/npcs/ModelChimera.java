package techguns.client.models.npcs;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.entities.npcs.ChimeraBoss;

/**
 * Chimera, the final boss: a hunched mutant with a flesh claw on the right, a cannon arm on the
 * left, a reactor pack with exhausts and tentacles on the back, one flesh and one machine leg.
 * Box layout and texture are generated together (textures/entity/chimera.png and chimera_glow.png).
 */
@SideOnly(Side.CLIENT)
public class ModelChimera extends ModelBase {

	/** forward lean of the torso, the arms and the head compensate it */
	protected static final float LEAN = 0.45F;

	public ModelRenderer torso;
	public ModelRenderer belly;
	public ModelRenderer heart;
	public ModelRenderer chestPlate;
	public ModelRenderer pack;
	public ModelRenderer pipeL;
	public ModelRenderer pipeR;
	public ModelRenderer spike1;
	public ModelRenderer spike2;
	public ModelRenderer spike3;
	public ModelRenderer head;
	public ModelRenderer jaw;
	public ModelRenderer visor;
	public ModelRenderer cable;
	public ModelRenderer antenna;
	public ModelRenderer armR;
	public ModelRenderer clawR;
	public ModelRenderer talon1;
	public ModelRenderer talon2;
	public ModelRenderer talon3;
	public ModelRenderer armL;
	public ModelRenderer cannon;
	public ModelRenderer cannonRing;
	public ModelRenderer muzzle;
	public ModelRenderer tentacle1;
	public ModelRenderer tentacle1b;
	public ModelRenderer tentacle2;
	public ModelRenderer tentacle2b;
	public ModelRenderer legR;
	public ModelRenderer shinR;
	public ModelRenderer legL;
	public ModelRenderer shinL;
	public ModelRenderer piston;

	public ModelChimera() {
		this.textureWidth = 256;
		this.textureHeight = 64;
		this.torso = new ModelRenderer(this, 0, 0);
		this.torso.setRotationPoint(0.0F, 7.0F, 2.0F);
		this.torso.addBox(-8.0F, -16.0F, -5.0F, 16, 16, 10, 0.0F);
		this.belly = new ModelRenderer(this, 180, 26);
		this.belly.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.belly.addBox(-6.0F, -8.0F, -8.0F, 12, 8, 3, 0.0F);
		this.heart = new ModelRenderer(this, 132, 43);
		this.heart.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.heart.addBox(-2.5F, -13.0F, -6.5F, 5, 5, 2, 0.0F);
		this.chestPlate = new ModelRenderer(this, 242, 26);
		this.chestPlate.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.chestPlate.addBox(3.0F, -15.0F, -6.0F, 5, 8, 2, 0.0F);
		this.pack = new ModelRenderer(this, 134, 0);
		this.pack.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.pack.addBox(-6.0F, -15.0F, 5.0F, 12, 11, 6, 0.0F);
		this.pipeL = new ModelRenderer(this, 0, 43);
		this.pipeL.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.pipeL.addBox(-5.0F, -21.0F, 7.0F, 3, 7, 3, 0.0F);
		this.pipeR = new ModelRenderer(this, 12, 43);
		this.pipeR.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.pipeR.addBox(2.0F, -21.0F, 7.0F, 3, 7, 3, 0.0F);
		this.spike1 = new ModelRenderer(this, 146, 43);
		this.spike1.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.spike1.addBox(-1.0F, -20.0F, -2.0F, 2, 4, 2, 0.0F);
		this.spike2 = new ModelRenderer(this, 170, 43);
		this.spike2.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.spike2.addBox(-5.0F, -19.0F, 0.0F, 2, 3, 2, 0.0F);
		this.spike3 = new ModelRenderer(this, 178, 43);
		this.spike3.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.spike3.addBox(3.0F, -19.0F, 0.0F, 2, 3, 2, 0.0F);
		this.head = new ModelRenderer(this, 98, 0);
		this.head.setRotationPoint(0.0F, -15.0F, -4.0F);
		this.head.addBox(-4.5F, -8.0F, -9.0F, 9, 9, 9, 0.0F);
		this.jaw = new ModelRenderer(this, 210, 26);
		this.jaw.setRotationPoint(0.0F, 0.0F, -1.0F);
		this.jaw.addBox(-4.0F, 0.0F, -8.0F, 8, 3, 8, 0.0F);
		this.visor = new ModelRenderer(this, 190, 43);
		this.visor.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.visor.addBox(-3.5F, -6.0F, -9.5F, 7, 2, 1, 0.0F);
		this.cable = new ModelRenderer(this, 24, 43);
		this.cable.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.cable.addBox(-1.0F, -10.0F, -6.0F, 2, 2, 8, 0.0F);
		this.antenna = new ModelRenderer(this, 186, 43);
		this.antenna.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.antenna.addBox(-4.0F, -12.0F, -5.0F, 1, 4, 1, 0.0F);
		this.armR = new ModelRenderer(this, 52, 0);
		this.armR.setRotationPoint(-9.5F, -13.0F, -1.0F);
		this.armR.addBox(-5.0F, -2.0F, -3.5F, 6, 12, 7, 0.0F);
		this.clawR = new ModelRenderer(this, 170, 0);
		this.clawR.setRotationPoint(-2.0F, 10.0F, 0.0F);
		this.clawR.addBox(-3.5F, 0.0F, -4.0F, 7, 9, 8, 0.0F);
		this.talon1 = new ModelRenderer(this, 92, 43);
		this.talon1.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.talon1.addBox(-3.5F, 9.0F, -4.0F, 2, 6, 2, 0.0F);
		this.talon2 = new ModelRenderer(this, 84, 43);
		this.talon2.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.talon2.addBox(-1.0F, 9.0F, -4.0F, 2, 7, 2, 0.0F);
		this.talon3 = new ModelRenderer(this, 100, 43);
		this.talon3.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.talon3.addBox(1.5F, 9.0F, -4.0F, 2, 6, 2, 0.0F);
		this.armL = new ModelRenderer(this, 200, 0);
		this.armL.setRotationPoint(9.5F, -13.0F, -1.0F);
		this.armL.addBox(-1.0F, -3.0F, -4.5F, 8, 8, 9, 0.0F);
		this.cannon = new ModelRenderer(this, 78, 0);
		this.cannon.setRotationPoint(3.0F, 4.0F, 0.0F);
		this.cannon.addBox(-2.5F, 0.0F, -2.5F, 5, 14, 5, 0.0F);
		this.cannonRing = new ModelRenderer(this, 108, 43);
		this.cannonRing.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.cannonRing.addBox(-3.0F, 5.0F, -3.0F, 6, 2, 6, 0.0F);
		this.muzzle = new ModelRenderer(this, 154, 43);
		this.muzzle.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.muzzle.addBox(-2.0F, 14.0F, -2.0F, 4, 2, 4, 0.0F);
		this.tentacle1 = new ModelRenderer(this, 124, 26);
		this.tentacle1.setRotationPoint(-4.0F, -7.0F, 10.0F);
		this.tentacle1.addBox(-1.5F, -1.5F, 0.0F, 3, 3, 9, 0.0F);
		this.tentacle1b = new ModelRenderer(this, 44, 43);
		this.tentacle1b.setRotationPoint(0.0F, 0.0F, 9.0F);
		this.tentacle1b.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8, 0.0F);
		this.tentacle2 = new ModelRenderer(this, 148, 26);
		this.tentacle2.setRotationPoint(4.0F, -7.0F, 10.0F);
		this.tentacle2.addBox(-1.5F, -1.5F, 0.0F, 3, 3, 9, 0.0F);
		this.tentacle2b = new ModelRenderer(this, 64, 43);
		this.tentacle2b.setRotationPoint(0.0F, 0.0F, 9.0F);
		this.tentacle2b.addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8, 0.0F);
		this.legR = new ModelRenderer(this, 0, 26);
		this.legR.setRotationPoint(-5.0F, 7.0F, 2.0F);
		this.legR.addBox(-3.5F, 0.0F, -3.5F, 7, 10, 7, 0.0F);
		this.shinR = new ModelRenderer(this, 56, 26);
		this.shinR.setRotationPoint(0.0F, 10.0F, 0.0F);
		this.shinR.addBox(-4.0F, 0.0F, -5.0F, 8, 7, 9, 0.0F);
		this.legL = new ModelRenderer(this, 28, 26);
		this.legL.setRotationPoint(5.0F, 7.0F, 2.0F);
		this.legL.addBox(-3.5F, 0.0F, -3.5F, 7, 10, 7, 0.0F);
		this.shinL = new ModelRenderer(this, 90, 26);
		this.shinL.setRotationPoint(0.0F, 10.0F, 0.0F);
		this.shinL.addBox(-4.0F, 0.0F, -5.0F, 8, 7, 9, 0.0F);
		this.piston = new ModelRenderer(this, 172, 26);
		this.piston.setRotationPoint(0.0F, 0.0F, 0.0F);
		this.piston.addBox(-1.0F, 2.0F, 3.5F, 2, 10, 2, 0.0F);
		this.torso.addChild(this.belly);
		this.torso.addChild(this.heart);
		this.torso.addChild(this.chestPlate);
		this.torso.addChild(this.pack);
		this.torso.addChild(this.pipeL);
		this.torso.addChild(this.pipeR);
		this.torso.addChild(this.spike1);
		this.torso.addChild(this.spike2);
		this.torso.addChild(this.spike3);
		this.torso.addChild(this.head);
		this.head.addChild(this.jaw);
		this.head.addChild(this.visor);
		this.head.addChild(this.cable);
		this.head.addChild(this.antenna);
		this.torso.addChild(this.armR);
		this.armR.addChild(this.clawR);
		this.clawR.addChild(this.talon1);
		this.clawR.addChild(this.talon2);
		this.clawR.addChild(this.talon3);
		this.torso.addChild(this.armL);
		this.armL.addChild(this.cannon);
		this.cannon.addChild(this.cannonRing);
		this.cannon.addChild(this.muzzle);
		this.torso.addChild(this.tentacle1);
		this.tentacle1.addChild(this.tentacle1b);
		this.torso.addChild(this.tentacle2);
		this.tentacle2.addChild(this.tentacle2b);
		this.legR.addChild(this.shinR);
		this.legL.addChild(this.shinL);
		this.legL.addChild(this.piston);
	}

	@Override
	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
		this.torso.render(scale);
		this.legR.render(scale);
		this.legL.render(scale);
	}

	@Override
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
		int phase = 1;
		float smash = 0.0F;
		if (entity instanceof ChimeraBoss) {
			ChimeraBoss chimera = (ChimeraBoss) entity;
			phase = chimera.getPhase();
			int ticks = chimera.getSmashTicks();
			if (ticks > 0) {
				float t = Math.max(0.0F, ticks - (ageInTicks - entity.ticksExisted));
				//raise the claw in the first half, smash it down in the second
				smash = t > 6.0F ? (12.0F - t) / 6.0F : t / 6.0F;
			}
		}
		float walk = MathHelper.cos(limbSwing * 0.45F) * 0.9F * limbSwingAmount;
		float breath = MathHelper.sin(ageInTicks * 0.08F);

		this.legR.rotateAngleX = walk;
		this.legL.rotateAngleX = -walk;
		this.shinR.rotateAngleX = Math.max(0.0F, -walk) * 0.7F;
		this.shinL.rotateAngleX = Math.max(0.0F, walk) * 0.7F;

		this.torso.rotateAngleX = LEAN + breath * 0.03F + smash * 0.2F;
		this.torso.rotateAngleY = MathHelper.cos(limbSwing * 0.45F) * 0.08F * limbSwingAmount;

		this.head.rotateAngleY = netHeadYaw * 0.017453292F * 0.7F;
		this.head.rotateAngleX = headPitch * 0.017453292F * 0.6F - LEAN;
		this.jaw.rotateAngleX = 0.12F + (MathHelper.sin(ageInTicks * 0.2F) + 1.0F) * 0.08F + (phase == 3 ? 0.25F : 0.0F) + smash * 0.3F;

		//flesh claw: swings while walking, raised and slammed down when attacking
		this.armR.rotateAngleX = -LEAN - walk * 0.4F - smash * 2.2F;
		this.armR.rotateAngleZ = 0.15F + breath * 0.03F;
		this.clawR.rotateAngleX = -0.2F - smash * 0.4F;

		//cannon arm: hangs in phase 1, points forward in the machine phases
		if (phase >= 2) {
			this.armL.rotateAngleX = -LEAN - 1.57F + MathHelper.sin(ageInTicks * 0.3F) * 0.03F;
			this.armL.rotateAngleZ = -0.1F;
		} else {
			this.armL.rotateAngleX = -LEAN + walk * 0.4F;
			this.armL.rotateAngleZ = -0.12F;
		}

		float speed = phase == 3 ? 0.22F : 0.1F;
		this.tentacle1.rotateAngleY = MathHelper.sin(ageInTicks * speed) * 0.5F - 0.3F;
		this.tentacle1.rotateAngleX = -0.4F + MathHelper.cos(ageInTicks * speed * 1.3F) * 0.3F;
		this.tentacle1b.rotateAngleX = MathHelper.sin(ageInTicks * speed * 1.5F + 1.0F) * 0.5F;
		this.tentacle2.rotateAngleY = -MathHelper.sin(ageInTicks * speed + 1.7F) * 0.5F + 0.3F;
		this.tentacle2.rotateAngleX = -0.4F + MathHelper.cos(ageInTicks * speed * 1.3F + 1.7F) * 0.3F;
		this.tentacle2b.rotateAngleX = MathHelper.sin(ageInTicks * speed * 1.5F + 2.7F) * 0.5F;

		//the heart beats faster in the last phase
		float beat = MathHelper.sin(ageInTicks * (phase == 3 ? 0.9F : 0.4F));
		this.heart.offsetZ = -Math.max(0.0F, beat) * 0.03F;
	}
}
