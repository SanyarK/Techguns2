package techguns.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Epilogue of the campaign "Dawn": the fates of the heroes scroll up like the end poem of
 * Minecraft while the sky at the bottom of the screen slowly lights up, "Thank you for playing"
 * stops in the middle. Space or a mouse button scroll faster, Esc closes the screen.
 * The text lines are techguns.campaign.epilogue.1, .2 ... in the lang files.
 */
@SideOnly(Side.CLIENT)
public class GuiCampaignEpilogue extends GuiScreen {

	protected static final String KEY = "techguns.campaign.epilogue.";
	protected static final int WRAP_WIDTH = 260;
	protected static final int LINE_HEIGHT = 12;
	protected static final int TITLE_SPACE = 80;
	protected static final float SPEED = 0.55f;

	protected final List<String> lines = new ArrayList<>();
	protected float scroll = 0.0f;
	protected float lastScroll = 0.0f;
	protected int endTicks = 0;

	@Override
	public void initGui() {
		if (this.lines.isEmpty()) {
			for (int i = 1; i < 300; i++) {
				String key = KEY + i;
				String text = I18n.format(key);
				if (text.equals(key)) {
					break;
				}
				if (text.trim().isEmpty()) {
					this.lines.add("");
				} else {
					this.lines.addAll(this.fontRenderer.listFormattedStringToWidth(text, WRAP_WIDTH));
				}
			}
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f));
		}
	}

	/** scroll at which the last line stands in the middle of the screen */
	protected float maxScroll() {
		return TITLE_SPACE + Math.max(this.lines.size() - 1, 0) * LINE_HEIGHT + this.height / 2.0f;
	}

	@Override
	public void updateScreen() {
		this.lastScroll = this.scroll;
		float max = this.maxScroll();
		if (this.scroll < max) {
			boolean fast = Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Mouse.isButtonDown(0);
			this.scroll = Math.min(max, this.scroll + SPEED * (fast ? 6.0f : 1.0f));
		} else {
			this.endTicks++;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		float s = this.lastScroll + (this.scroll - this.lastScroll) * partialTicks;
		float progress = Math.min(1.0f, s / Math.max(this.maxScroll(), 1.0f));

		//night sky that turns into dawn at the bottom of the screen
		drawRect(0, 0, this.width, this.height, 0xFF04050A);
		int glow = (int) (progress * 200.0f);
		this.drawGradientRect(0, this.height / 3, this.width, this.height, 0x00FF9A3C, (glow << 24) | 0xE07A2E);

		int baseY = (int) (this.height - s);
		String title = I18n.format(KEY + "title");
		GlStateManager.pushMatrix();
		GlStateManager.translate(this.width / 2.0f, baseY + 12.0f, 0.0f);
		GlStateManager.scale(3.0f, 3.0f, 1.0f);
		this.drawCenteredString(this.fontRenderer, title, 0, 0, 0xFFD27F);
		GlStateManager.popMatrix();
		this.drawCenteredString(this.fontRenderer, I18n.format(KEY + "subtitle"), this.width / 2, baseY + 48, 0xA0A0A0);

		for (int i = 0; i < this.lines.size(); i++) {
			int y = baseY + TITLE_SPACE + i * LINE_HEIGHT;
			if (y < -LINE_HEIGHT || y > this.height) {
				continue;
			}
			this.drawCenteredString(this.fontRenderer, this.lines.get(i), this.width / 2, y, 0xE8E8E8);
		}

		//soft edges
		this.drawGradientRect(0, 0, this.width, 40, 0xFF04050A, 0x0004050A);
		if (this.endTicks > 40) {
			this.drawCenteredString(this.fontRenderer, I18n.format(KEY + "close"), this.width / 2, this.height - 16, 0x909090);
		} else if (s > 20.0f && s < this.maxScroll() - 20.0f) {
			this.drawCenteredString(this.fontRenderer, I18n.format(KEY + "skip"), this.width / 2, this.height - 16, 0x606060);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return true;
	}
}
