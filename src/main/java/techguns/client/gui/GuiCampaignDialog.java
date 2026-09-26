package techguns.client.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.TGPackets;
import techguns.campaign.CampaignMission;
import techguns.campaign.TGCampaign;
import techguns.capabilities.TGCampaignData;
import techguns.packets.PacketCampaignAction;

/**
 * Dialog screen of the campaign commander (via radio or talking to the NPC),
 * with mission briefing, accept / turn in buttons and the mission journal.
 */
@SideOnly(Side.CLIENT)
public class GuiCampaignDialog extends GuiScreen {

	protected static final int PANEL_WIDTH = 280;
	protected static final int PANEL_HEIGHT = 190;

	protected static final int BTN_ACCEPT = 0;
	protected static final int BTN_TURN_IN = 1;
	protected static final int BTN_JOURNAL = 2;
	protected static final int BTN_CLOSE = 3;
	protected static final int BTN_BACK = 4;

	protected final int mission;
	protected final byte state;
	protected final int progress;
	protected final boolean atCommander;

	protected boolean journal = false;

	public GuiCampaignDialog(int mission, byte state, int progress, boolean atCommander) {
		this.mission = mission;
		this.state = state;
		this.progress = progress;
		this.atCommander = atCommander;
	}

	protected int panelLeft() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	protected int panelTop() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		int bottom = this.panelTop() + PANEL_HEIGHT - 26;
		int left = this.panelLeft();

		if (this.journal) {
			this.buttonList.add(new GuiButton(BTN_BACK, left + PANEL_WIDTH / 2 - 50, bottom, 100, 20, I18n.format("techguns.campaign.gui.back")));
			return;
		}

		CampaignMission m = CampaignMission.byId(this.mission);
		boolean finished = this.mission > TGCampaignData.LAST_MISSION;
		int x = left + 8;

		if (!finished && m != null && this.state == TGCampaignData.STATE_OFFERED) {
			this.buttonList.add(new GuiButton(BTN_ACCEPT, x, bottom, 100, 20, I18n.format("techguns.campaign.gui.accept")));
			x += 104;
		}
		if (!finished && m != null && this.state == TGCampaignData.STATE_READY) {
			GuiButton turnIn = new GuiButton(BTN_TURN_IN, x, bottom, 120, 20, I18n.format("techguns.campaign.gui.turn_in"));
			turnIn.enabled = !m.requiresCommander || this.atCommander;
			this.buttonList.add(turnIn);
			x += 124;
		}
		this.buttonList.add(new GuiButton(BTN_JOURNAL, x, bottom, 80, 20, I18n.format("techguns.campaign.gui.journal")));
		x += 84;
		this.buttonList.add(new GuiButton(BTN_CLOSE, x, bottom, 70, 20, I18n.format("techguns.campaign.gui.close")));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case BTN_ACCEPT:
			TGPackets.network.sendToServer(new PacketCampaignAction(TGCampaign.ACTION_ACCEPT));
			break;
		case BTN_TURN_IN:
			TGPackets.network.sendToServer(new PacketCampaignAction(TGCampaign.ACTION_TURN_IN));
			break;
		case BTN_JOURNAL:
			this.journal = true;
			this.initGui();
			break;
		case BTN_BACK:
			this.journal = false;
			this.initGui();
			break;
		case BTN_CLOSE:
		default:
			this.mc.displayGuiScreen(null);
			break;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		int left = this.panelLeft();
		int top = this.panelTop();

		drawRect(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF4a5a3a);
		drawRect(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101410);

		if (this.journal) {
			this.drawJournal(left, top);
		} else {
			this.drawDialog(left, top);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void drawDialog(int left, int top) {
		int x = left + 10;
		int y = top + 8;

		this.fontRenderer.drawString(I18n.format("techguns.campaign.commander.name"), x, y, 0xFFD700);
		y += 14;

		boolean finished = this.mission > TGCampaignData.LAST_MISSION;
		CampaignMission m = CampaignMission.byId(this.mission);

		if (finished || m == null) {
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.finished.text"), x, y, PANEL_WIDTH - 20, 0xE0E0E0);
			return;
		}

		this.fontRenderer.drawString(I18n.format("techguns.campaign.gui.mission_no", m.id, TGCampaignData.LAST_MISSION)
				+ " " + I18n.format(m.getTitleKey()), x, y, 0xFFFF55);
		y += 14;

		String text;
		switch (this.state) {
		case TGCampaignData.STATE_ACTIVE:
			text = I18n.format(m.getActiveKey());
			break;
		case TGCampaignData.STATE_READY:
			text = I18n.format(m.getDoneKey());
			break;
		case TGCampaignData.STATE_OFFERED:
		default:
			text = I18n.format(m.getBriefKey());
			break;
		}
		this.fontRenderer.drawSplitString(text, x, y, PANEL_WIDTH - 20, 0xE0E0E0);
		y += this.fontRenderer.getWordWrappedHeight(text, PANEL_WIDTH - 20) + 8;

		if (this.state == TGCampaignData.STATE_ACTIVE && m.required > 1) {
			this.fontRenderer.drawString(I18n.format("techguns.campaign.gui.progress", this.progress, m.required), x, y, 0x55FF55);
			y += 12;
		}
		if (this.state == TGCampaignData.STATE_READY && m.requiresCommander && !this.atCommander) {
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.gui.come_in_person"), x, y, PANEL_WIDTH - 20, 0xFF5555);
		}
	}

	protected void drawJournal(int left, int top) {
		int x = left + 10;
		int y = top + 8;

		this.fontRenderer.drawString(I18n.format("techguns.campaign.journal.title"), x, y, 0xFFD700);
		y += 14;

		boolean finished = this.mission > TGCampaignData.LAST_MISSION;
		for (CampaignMission m : CampaignMission.values()) {
			String line;
			int color;
			if (finished || m.id < this.mission) {
				line = "✔ " + I18n.format(m.getTitleKey());
				color = 0x55FF55;
			} else if (m.id == this.mission) {
				String status = this.state == TGCampaignData.STATE_READY
						? I18n.format("techguns.campaign.journal.ready")
						: (this.state == TGCampaignData.STATE_ACTIVE
								? I18n.format("techguns.campaign.gui.progress", this.progress, m.required)
								: I18n.format("techguns.campaign.journal.new"));
				line = "▶ " + I18n.format(m.getTitleKey()) + " - " + status;
				color = 0xFFFF55;
			} else {
				line = "? " + I18n.format("techguns.campaign.journal.locked");
				color = 0x777777;
			}
			this.fontRenderer.drawString(I18n.format("techguns.campaign.gui.mission_no", m.id, TGCampaignData.LAST_MISSION) + " " + line, x, y, color);
			y += 12;
		}

		if (finished) {
			this.fontRenderer.drawString(I18n.format("techguns.campaign.journal.finished"), x, y + 4, 0xFFD700);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
