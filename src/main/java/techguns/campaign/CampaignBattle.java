package techguns.campaign;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import techguns.Techguns;
import techguns.campaign.CampaignMission.Group;
import techguns.campaign.CampaignMission.ObjectiveType;
import techguns.campaign.CampaignMission.Wave;
import techguns.capabilities.TGCampaignData;
import techguns.entities.npcs.Paratrooper;

/**
 * A running DEFEND (waves) or SURVIVE (timer) fight of one player around a point, with a
 * progress bar at the top of the screen. Lives only in memory: leaving the area, dying or
 * logging out cancels it and it starts over when the player comes back.
 */
public class CampaignBattle {

	/** the fight starts when the player is this close to the point */
	public static final double START_RADIUS = 24.0D;
	/** leaving this radius cancels the fight */
	public static final double LEAVE_RADIUS = 64.0D;

	protected final CampaignMission mission;
	protected final BlockPos center;
	protected final World world;
	protected final BossInfoServer bar;
	protected final List<EntityLiving> alive = new ArrayList<>();

	protected boolean started = false;
	protected boolean waveRunning = false;
	/** seconds until the next wave */
	protected int cooldown = 0;
	protected int waveSize = 0;
	/** seconds the current wave is running */
	protected int waveTime = 0;
	/** SURVIVE: seconds left */
	protected int timeLeft;

	public CampaignBattle(CampaignMission mission, World world, BlockPos center) {
		this.mission = mission;
		this.world = world;
		this.center = center;
		this.timeLeft = mission.seconds;
		this.bar = new BossInfoServer(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.waiting"), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10);
		this.bar.setPercent(1.0f);
	}

	public boolean isStarted() {
		return this.started;
	}

	public BlockPos getCenter() {
		return this.center;
	}

	/**
	 * Called once per second
	 * @return true when the fight is won
	 */
	public boolean tick(EntityPlayerMP player, TGCampaignData data) {
		if (!this.started) {
			this.started = true;
			this.cooldown = 5;
			this.bar.addPlayer(player);
			TGCampaign.commanderSays(player, "battle.start_" + (this.mission.type == ObjectiveType.SURVIVE ? "survive" : "defend"));
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.4f, 1.4f);
		}

		//count the losses of the attackers, remove the ones that got lost far away
		Iterator<EntityLiving> it = this.alive.iterator();
		while (it.hasNext()) {
			EntityLiving e = it.next();
			if (!e.isEntityAlive()) {
				it.remove();
			} else if (horizontalDistSq(e.posX, e.posZ) > 80.0D * 80.0D) {
				e.setDead();
				it.remove();
			} else if (e.getAttackTarget() == null) {
				e.setAttackTarget(player);
			}
		}

		if (this.mission.type == ObjectiveType.SURVIVE) {
			return this.tickSurvive(player, data);
		}
		return this.tickDefend(player, data);
	}

	protected boolean tickDefend(EntityPlayerMP player, TGCampaignData data) {
		int waves = this.mission.waves.size();
		int wave = data.getProgress();
		if (wave >= waves) {
			return true;
		}

		if (this.waveRunning) {
			this.waveTime++;
			if (this.waveTime == 90) {
				//help to find the last ones
				for (EntityLiving e : this.alive) {
					e.setGlowing(true);
				}
			}
			this.bar.setName(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.wave", wave + 1, waves, this.alive.size()));
			this.bar.setPercent(this.waveSize > 0 ? (float) this.alive.size() / this.waveSize : 0f);
			if (this.alive.isEmpty()) {
				this.waveRunning = false;
				data.setProgress(wave + 1);
				TGCampaign.sync(player);
				if (wave + 1 >= waves) {
					this.bar.setPercent(0f);
					return true;
				}
				this.cooldown = 10;
				TGCampaign.commanderSays(player, "battle.wave_cleared", wave + 1, waves);
			}
		} else {
			this.bar.setName(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.next", wave + 1, waves, Math.max(this.cooldown, 0)));
			this.bar.setPercent(1.0f);
			if (--this.cooldown <= 0) {
				this.spawnWave(player, this.mission.waves.get(wave));
				this.waveRunning = true;
				this.waveTime = 0;
				player.sendStatusMessage(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.incoming", wave + 1, waves), true);
			}
		}
		return false;
	}

	protected boolean tickSurvive(EntityPlayerMP player, TGCampaignData data) {
		this.timeLeft--;
		data.setProgress(this.mission.seconds - Math.max(this.timeLeft, 0));
		this.bar.setName(new TextComponentTranslation(Techguns.MODID + ".campaign.battle.survive", this.timeLeft / 60, String.format("%02d", Math.max(this.timeLeft, 0) % 60)));
		this.bar.setPercent(this.mission.seconds > 0 ? Math.max(this.timeLeft, 0) / (float) this.mission.seconds : 0f);
		if (this.timeLeft <= 0) {
			return true;
		}
		int interval = Math.max(this.mission.raidInterval, 5);
		if (this.mission.target != null && this.timeLeft % interval == 0 && this.alive.size() < this.mission.raidSize * 3) {
			this.spawnGroup(player, Group.of(this.mission.target, this.mission.raidSize));
		}
		return false;
	}

	protected void spawnWave(EntityPlayerMP player, Wave wave) {
		this.waveSize = 0;
		for (Group g : wave.groups) {
			this.spawnGroup(player, g);
		}
		this.waveSize = Math.max(this.alive.size(), 1);
	}

	protected void spawnGroup(EntityPlayerMP player, Group group) {
		for (int i = 0; i < group.count; i++) {
			BlockPos pos = TGCampaign.randomSurface(this.world, this.center, 18, 30);
			double y = group.airdrop ? pos.getY() + 24 + this.world.rand.nextInt(8) : pos.getY();
			if (group.target == CampaignTarget.JET) {
				//jets attack the player, they don't have to be shot down to win the wave
				techguns.events.MilitaryExpansionEventHandler.startAirRaid(this.world, player);
				continue;
			}
			EntityLiving e = TGCampaign.spawnMob(this.world, group.target, pos.getX() + 0.5D, y, pos.getZ() + 0.5D, player, true);
			if (e != null) {
				if (group.airdrop && e instanceof Paratrooper) {
					((Paratrooper) e).setParachute(true);
				}
				this.alive.add(e);
			}
		}
	}

	protected double horizontalDistSq(double x, double z) {
		double dx = x - (this.center.getX() + 0.5D);
		double dz = z - (this.center.getZ() + 0.5D);
		return dx * dx + dz * dz;
	}

	/**
	 * Ends the fight: hides the bar, removes the remaining attackers when it was lost
	 */
	public void stop(EntityPlayerMP player, boolean removeAttackers) {
		this.bar.removePlayer(player);
		this.bar.setVisible(false);
		if (removeAttackers) {
			for (EntityLiving e : this.alive) {
				e.setDead();
			}
		}
		this.alive.clear();
	}
}
