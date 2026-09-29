package techguns.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.TGPackets;
import techguns.campaign.CampaignMission;
import techguns.campaign.CampaignMission.ObjectiveType;
import techguns.campaign.CampaignMissions;
import techguns.campaign.TGCampaign;
import techguns.capabilities.TGCampaignData;
import techguns.packets.PacketCampaignAction;

/**
 * Dialog screen of the campaign commander (via radio or talking to the NPC): act, difficulty,
 * briefing, goal with progress, hint and reward, accept / turn in buttons and the mission journal
 * with one page per act.
 */
@SideOnly(Side.CLIENT)
public class GuiCampaignDialog extends GuiScreen {

	protected static final int PANEL_WIDTH = 300;
	protected static final int PANEL_HEIGHT = 214;
	protected static final int TEXT_WIDTH = PANEL_WIDTH - 20;

	protected static final int BTN_ACCEPT = 0;
	protected static final int BTN_TURN_IN = 1;
	protected static final int BTN_JOURNAL = 2;
	protected static final int BTN_CLOSE = 3;
	protected static final int BTN_BACK = 4;
	protected static final int BTN_PREV = 5;
	protected static final int BTN_NEXT = 6;
	protected static final int BTN_CONTRACT = 7;
	protected static final int BTN_EPILOGUE = 8;

	protected int mission;
	protected byte state;
	protected int progress;
	protected int progress2;
	protected final boolean atCommander;

	protected boolean journal = false;
	protected int journalAct = 1;

	public GuiCampaignDialog(int mission, byte state, int progress, boolean atCommander) {
		this.mission = mission;
		this.state = state;
		this.progress = progress;
		this.atCommander = atCommander;
		CampaignMission m = CampaignMissions.byId(mission);
		this.journalAct = m != null ? m.act : CampaignMissions.ACTS;
	}

	protected int panelLeft() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	protected int panelTop() {
		return (this.height - PANEL_HEIGHT) / 2;
	}

	/**
	 * the synced capability is more recent than the packet that opened the screen
	 */
	protected void refreshData() {
		if (this.mc != null && this.mc.player != null) {
			TGCampaignData data = TGCampaignData.get(this.mc.player);
			if (data != null) {
				boolean changed = data.getMission() != this.mission || data.getState() != this.state;
				this.mission = data.getMission();
				this.state = data.getState();
				this.progress = data.getProgress();
				this.progress2 = data.getProgress2();
				if (changed) {
					this.initGui();
				}
			}
		}
	}

	protected boolean isFinished() {
		return this.mission > TGCampaignData.LAST_MISSION;
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		int bottom = this.panelTop() + PANEL_HEIGHT - 26;
		int left = this.panelLeft();

		if (this.journal) {
			this.buttonList.add(new GuiButton(BTN_PREV, left + 8, bottom, 30, 20, "<"));
			this.buttonList.add(new GuiButton(BTN_BACK, left + PANEL_WIDTH / 2 - 50, bottom, 100, 20, I18n.format("techguns.campaign.gui.back")));
			this.buttonList.add(new GuiButton(BTN_NEXT, left + PANEL_WIDTH - 38, bottom, 30, 20, ">"));
			return;
		}

		CampaignMission m = CampaignMissions.byId(this.mission);
		int x = left + 8;
		if (this.isFinished()) {
			//free play: contracts from the colonel and the epilogue once more
			GuiButton contract = new GuiButton(BTN_CONTRACT, x, bottom, 70, 20, I18n.format("techguns.campaign.gui.contract"));
			contract.enabled = this.atCommander;
			this.buttonList.add(contract);
			x += 74;
			this.buttonList.add(new GuiButton(BTN_EPILOGUE, x, bottom, 56, 20, I18n.format("techguns.campaign.gui.epilogue")));
			x += 60;
		}
		if (!this.isFinished() && m != null && this.state == TGCampaignData.STATE_OFFERED) {
			GuiButton accept = new GuiButton(BTN_ACCEPT, x, bottom, 110, 20, I18n.format(m.type == ObjectiveType.TALK && !m.inPerson ? "techguns.campaign.gui.answer" : "techguns.campaign.gui.accept"));
			accept.enabled = m.playable;
			this.buttonList.add(accept);
			x += 114;
		}
		if (!this.isFinished() && m != null && this.state == TGCampaignData.STATE_READY) {
			GuiButton turnIn = new GuiButton(BTN_TURN_IN, x, bottom, 110, 20, I18n.format("techguns.campaign.gui.turn_in"));
			turnIn.enabled = !m.inPerson || this.atCommander;
			this.buttonList.add(turnIn);
			x += 114;
		}
		this.buttonList.add(new GuiButton(BTN_JOURNAL, x, bottom, 80, 20, I18n.format("techguns.campaign.gui.journal")));
		x += 84;
		this.buttonList.add(new GuiButton(BTN_CLOSE, Math.min(x, left + PANEL_WIDTH - 78), bottom, 70, 20, I18n.format("techguns.campaign.gui.close")));
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
		case BTN_CONTRACT:
			TGPackets.network.sendToServer(new PacketCampaignAction(TGCampaign.ACTION_CONTRACT));
			break;
		case BTN_EPILOGUE:
			this.mc.displayGuiScreen(new GuiCampaignEpilogue());
			break;
		case BTN_JOURNAL:
			this.journal = true;
			this.initGui();
			break;
		case BTN_BACK:
			this.journal = false;
			this.initGui();
			break;
		case BTN_PREV:
			this.journalAct = this.journalAct <= 1 ? CampaignMissions.ACTS : this.journalAct - 1;
			break;
		case BTN_NEXT:
			this.journalAct = this.journalAct >= CampaignMissions.ACTS ? 1 : this.journalAct + 1;
			break;
		case BTN_CLOSE:
		default:
			this.mc.displayGuiScreen(null);
			break;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.refreshData();
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

	protected static String stars(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append('★');
		}
		return sb.toString();
	}

	protected void drawDialog(int left, int top) {
		int x = left + 10;
		int y = top + 8;

		this.fontRenderer.drawString(I18n.format("techguns.campaign.commander.name"), x, y, 0xFFD700);
		String faction = I18n.format("techguns.campaign.commander.faction");
		this.fontRenderer.drawString(faction, left + PANEL_WIDTH - 10 - this.fontRenderer.getStringWidth(faction), y, 0x9aa88a);
		y += 13;

		CampaignMission m = CampaignMissions.byId(this.mission);
		if (this.isFinished() || m == null) {
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.finished.text"), x, y, TEXT_WIDTH, 0xE0E0E0);
			y += this.fontRenderer.getWordWrappedHeight(I18n.format("techguns.campaign.finished.text"), TEXT_WIDTH) + 8;
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.finished.free_play"), x, y, TEXT_WIDTH, 0x9aa88a);
			return;
		}

		this.fontRenderer.drawString(I18n.format(m.getActKey()) + "  " + stars(m.stars), x, y, 0xE08A3C);
		y += 11;
		this.fontRenderer.drawString(I18n.format("techguns.campaign.gui.mission_no", m.id, TGCampaignData.LAST_MISSION) + " " + I18n.format(m.getTitleKey()), x, y, 0xFFFF55);
		y += 13;

		if (!m.playable) {
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.planned.text", I18n.format(m.getActKey())), x, y, TEXT_WIDTH, 0xE0E0E0);
			return;
		}

		String text = I18n.format(this.state == TGCampaignData.STATE_READY ? m.getDoneKey() : m.getBriefKey());
		this.fontRenderer.drawSplitString(text, x, y, TEXT_WIDTH, 0xE0E0E0);
		y += this.fontRenderer.getWordWrappedHeight(text, TEXT_WIDTH) + 6;

		if (this.state != TGCampaignData.STATE_READY) {
			String goal = I18n.format("techguns.campaign.gui.goal") + " " + I18n.format(m.getGoalKey());
			if (this.state == TGCampaignData.STATE_ACTIVE) {
				goal += " " + this.progressText(m);
			}
			this.fontRenderer.drawSplitString(goal, x, y, TEXT_WIDTH, 0x55FF55);
			y += this.fontRenderer.getWordWrappedHeight(goal, TEXT_WIDTH) + 3;
		}
		if (this.state == TGCampaignData.STATE_ACTIVE) {
			String hint = I18n.format("techguns.campaign.gui.hint") + " " + I18n.format(m.getHintKey());
			this.fontRenderer.drawSplitString(hint, x, y, TEXT_WIDTH, 0x9a9a9a);
			y += this.fontRenderer.getWordWrappedHeight(hint, TEXT_WIDTH) + 3;
		}
		if (this.state != TGCampaignData.STATE_ACTIVE) {
			String reward = I18n.format("techguns.campaign.gui.reward") + " " + I18n.format(m.getRewardKey());
			this.fontRenderer.drawSplitString(reward, x, y, TEXT_WIDTH, 0xFFB84D);
			y += this.fontRenderer.getWordWrappedHeight(reward, TEXT_WIDTH) + 3;
		}
		String where = I18n.format(m.inPerson ? "techguns.campaign.gui.in_person" : "techguns.campaign.gui.by_radio");
		if (this.state == TGCampaignData.STATE_READY && m.inPerson && !this.atCommander) {
			this.fontRenderer.drawSplitString(I18n.format("techguns.campaign.gui.come_in_person"), x, y, TEXT_WIDTH, 0xFF5555);
		} else if (m.type != ObjectiveType.TALK || m.inPerson) {
			this.fontRenderer.drawString(where, x, y, 0x7f8f6f);
		}
	}

	protected String progressText(CampaignMission m) {
		switch (m.type) {
		case DEFEND:
			if (m.seconds > 0) {
				return I18n.format("techguns.campaign.gui.charge", this.progress);
			}
			return I18n.format("techguns.campaign.gui.waves", this.progress, m.getRequired());
		case SURVIVE:
			return I18n.format("techguns.campaign.gui.time", Math.max(m.seconds - this.progress, 0));
		case DESTROY:
			if (m.target2 != null) {
				return I18n.format("techguns.campaign.gui.progress_garrison", this.progress, m.count, this.progress2, m.count2);
			}
			return I18n.format("techguns.campaign.gui.progress", this.progress, m.count);
		case BOSS:
		case ITEM:
		case REACH:
		case TALK:
			return "";
		default:
			return m.count > 1 ? I18n.format("techguns.campaign.gui.progress", this.progress, m.count) : "";
		}
	}

	protected void drawJournal(int left, int top) {
		int x = left + 10;
		int y = top + 8;

		this.fontRenderer.drawString(I18n.format("techguns.campaign.journal.title"), x, y, 0xFFD700);
		String page = I18n.format("techguns.campaign.gui.page", this.journalAct, CampaignMissions.ACTS);
		this.fontRenderer.drawString(page, left + PANEL_WIDTH - 10 - this.fontRenderer.getStringWidth(page), y, 0x9aa88a);
		y += 14;

		List<CampaignMission> missions = CampaignMissions.ofAct(this.journalAct);
		if (!missions.isEmpty()) {
			this.fontRenderer.drawString(I18n.format(missions.get(0).getActKey()) + "  " + stars(missions.get(0).stars), x, y, 0xE08A3C);
			y += 14;
		}
		for (CampaignMission m : missions) {
			String line;
			int color;
			if (this.isFinished() || m.id < this.mission) {
				line = "✔ " + I18n.format(m.getTitleKey());
				color = 0x55FF55;
			} else if (m.id == this.mission) {
				String status;
				if (!m.playable) {
					status = I18n.format("techguns.campaign.journal.planned");
				} else if (this.state == TGCampaignData.STATE_READY) {
					status = I18n.format("techguns.campaign.journal.ready");
				} else if (this.state == TGCampaignData.STATE_ACTIVE) {
					status = I18n.format("techguns.campaign.journal.active");
				} else {
					status = I18n.format("techguns.campaign.journal.new");
				}
				line = "▶ " + I18n.format(m.getTitleKey()) + " - " + status;
				color = 0xFFFF55;
			} else {
				line = "? " + I18n.format(m.playable ? "techguns.campaign.journal.locked" : "techguns.campaign.journal.planned");
				color = 0x777777;
			}
			this.fontRenderer.drawString(I18n.format("techguns.campaign.gui.mission_no", m.id, TGCampaignData.LAST_MISSION) + " " + line, x, y, color);
			y += 13;
		}

		if (this.isFinished()) {
			this.fontRenderer.drawString(I18n.format("techguns.campaign.journal.finished"), x, y + 6, 0xFFD700);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
