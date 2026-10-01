package techguns.campaign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.util.Constants;
import techguns.TGPackets;
import techguns.packets.PacketCampaignEvent;
import techguns.world.wasteland.BiomeWasteland;

/**
 * The world after the Purifier was launched (overworld saved data): the "world restored" flag that
 * gives back rain, sky and fog on the clients, and the circles of new life around the command posts
 * and the start bunker. Each circle grows ring by ring up to {@link #MAX_RADIUS} blocks, a limited
 * number of columns per tick: coarse dirt turns into grass, dead bushes into flowers and saplings.
 * Columns in chunks that are not loaded are finished when the chunk is loaded again.
 */
public class WorldRestoration extends WorldSavedData {

	public static final String DATA_NAME = "techguns_restoration";

	public static final int MAX_RADIUS = 150;
	/** a ring is at least this many ticks, the circle grows by one block per second */
	protected static final int TICKS_PER_RING = 20;
	protected static final int COLUMNS_PER_TICK = 64;
	/** hostile creatures spawn this much less on restored land */
	public static final float SPAWN_DENY_CHANCE = 0.75f;

	protected boolean restored = false;
	protected final List<Circle> circles = new ArrayList<>();
	/** chunks the wave passed while they were not loaded */
	protected final Set<Long> pending = new HashSet<>();

	public WorldRestoration() {
		super(DATA_NAME);
	}

	public WorldRestoration(String name) {
		super(name);
	}

	public static WorldRestoration get(World world) {
		MapStorage storage = world.getPerWorldStorage();
		WorldRestoration data = (WorldRestoration) storage.getOrLoadData(WorldRestoration.class, DATA_NAME);
		if (data == null) {
			data = new WorldRestoration();
			storage.setData(DATA_NAME, data);
		}
		return data;
	}

	public boolean isRestored() {
		return this.restored;
	}

	/**
	 * The Purifier works: the flag is set, the clients learn it, clean rain begins
	 */
	public void restore(World world) {
		if (!this.restored) {
			this.restored = true;
			this.markDirty();
		}
		BiomeWasteland.setWorldRestored(true);
		TGPackets.network.sendToDimension(new PacketCampaignEvent(PacketCampaignEvent.RESTORED, 1), world.provider.getDimension());
		WorldInfo info = world.getWorldInfo();
		info.setCleanWeatherTime(0);
		info.setRainTime(12000);
		info.setThunderTime(12000);
		info.setRaining(true);
		info.setThundering(false);
	}

	/**
	 * starts a circle of new life, circles closer than 32 blocks to an existing one are merged
	 */
	public void addCircle(BlockPos center) {
		for (Circle c : this.circles) {
			double dx = c.x - center.getX();
			double dz = c.z - center.getZ();
			if (dx * dx + dz * dz < 32.0D * 32.0D) {
				return;
			}
		}
		this.circles.add(new Circle(center.getX(), center.getZ()));
		this.markDirty();
	}

	public boolean isInRestoredArea(double x, double z) {
		for (Circle c : this.circles) {
			double dx = x - c.x;
			double dz = z - c.z;
			if (dx * dx + dz * dz <= (double) c.radius * c.radius) {
				return true;
			}
		}
		return false;
	}

	/**
	 * one server tick of the overworld
	 */
	public void tick(WorldServer world) {
		if (this.circles.isEmpty()) {
			return;
		}
		int budget = COLUMNS_PER_TICK;
		for (Circle c : this.circles) {
			if (c.radius <= MAX_RADIUS && budget > 0) {
				budget = c.grow(world, this, budget);
			}
		}
		if (!this.pending.isEmpty() && world.getTotalWorldTime() % 100L == 0L) {
			this.finishPending(world);
		}
	}

	/**
	 * up to two chunks the wave missed are brought to life when they are loaded
	 */
	protected void finishPending(WorldServer world) {
		int done = 0;
		Iterator<Long> it = this.pending.iterator();
		while (it.hasNext() && done < 2) {
			long key = it.next();
			int cx = (int) (key & 0xFFFFFFFFL);
			int cz = (int) (key >>> 32);
			if (!world.isBlockLoaded(new BlockPos(cx << 4, 0, cz << 4))) {
				continue;
			}
			it.remove();
			done++;
			for (int x = cx << 4; x < (cx << 4) + 16; x++) {
				for (int z = cz << 4; z < (cz << 4) + 16; z++) {
					if (this.isInRestoredArea(x + 0.5D, z + 0.5D)) {
						this.restoreColumn(world, x, z);
					}
				}
			}
			this.markDirty();
		}
	}

	protected static long chunkKey(int cx, int cz) {
		return (cx & 0xFFFFFFFFL) | ((long) cz << 32);
	}

	/**
	 * new life for one column of the surface
	 */
	protected void restoreColumn(WorldServer world, int x, int z) {
		BlockPos probe = new BlockPos(x, 0, z);
		if (!world.isBlockLoaded(probe)) {
			if (this.pending.add(chunkKey(x >> 4, z >> 4))) {
				this.markDirty();
			}
			return;
		}
		BlockPos top = world.getHeight(probe);
		if (top.getY() < 2 || top.getY() > 254) {
			return;
		}
		BlockPos groundPos = top.down();
		IBlockState ground = world.getBlockState(groundPos);
		IBlockState above = world.getBlockState(top);
		if (above.getMaterial().isLiquid()) {
			return;
		}
		Random rand = world.rand;
		boolean grass = ground.getBlock() == Blocks.GRASS;
		if (!grass && canBecomeGrass(ground, rand)) {
			world.setBlockState(groundPos, Blocks.GRASS.getDefaultState(), 2);
			grass = true;
		}
		if (!grass) {
			return;
		}
		if (above.getBlock() == Blocks.DEADBUSH) {
			world.setBlockState(top, plant(rand, true), 2);
		} else if (above.getMaterial() == Material.AIR) {
			IBlockState plant = plant(rand, false);
			if (plant != null) {
				world.setBlockState(top, plant, 2);
			}
		}
	}

	protected static boolean canBecomeGrass(IBlockState state, Random rand) {
		Block b = state.getBlock();
		if (b == Blocks.DIRT) {
			return true;
		}
		if (b == Blocks.GRAVEL) {
			return rand.nextInt(3) == 0;
		}
		if (b == Blocks.HARDENED_CLAY || b == Blocks.STAINED_HARDENED_CLAY) {
			return rand.nextBoolean();
		}
		if (b == Blocks.SAND) {
			return rand.nextInt(6) == 0;
		}
		return false;
	}

	/**
	 * grass, flowers and saplings, dead bushes always turn into something alive
	 */
	protected static IBlockState plant(Random rand, boolean deadBush) {
		int i = deadBush ? rand.nextInt(160) + 15 : rand.nextInt(1000);
		if (i < 8 || (deadBush && i < 45)) {
			return Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, rand.nextInt(3) == 0 ? BlockPlanks.EnumType.BIRCH : BlockPlanks.EnumType.OAK);
		}
		if (i < 55 || (deadBush && i < 125)) {
			return rand.nextInt(4) == 0 ? Blocks.YELLOW_FLOWER.getDefaultState() : Blocks.RED_FLOWER.getStateFromMeta(rand.nextInt(9));
		}
		if (i < 175) {
			return Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
		}
		return null;
	}

	/**
	 * smallest s with s*s >= v
	 */
	protected static int ceilSqrt(int v) {
		if (v <= 0) {
			return 0;
		}
		int s = (int) Math.sqrt(v);
		while (s * s < v) {
			s++;
		}
		while (s > 0 && (s - 1) * (s - 1) >= v) {
			s--;
		}
		return s;
	}

	/**
	 * a growing circle of life, the current ring is worked row by row
	 */
	protected static class Circle {
		protected final int x;
		protected final int z;
		protected int radius = 0;
		/** next row of the current ring, -radius..radius */
		protected int row = 0;
		protected int ringTicks = 0;

		protected Circle(int x, int z) {
			this.x = x;
			this.z = z;
		}

		protected int grow(WorldServer world, WorldRestoration data, int budget) {
			while (budget > 0 && this.row <= this.radius) {
				budget -= this.restoreRow(world, data, this.row);
				this.row++;
			}
			this.ringTicks++;
			if (this.row > this.radius && this.ringTicks >= TICKS_PER_RING) {
				this.radius++;
				this.row = -this.radius;
				this.ringTicks = 0;
				data.markDirty();
			}
			return budget;
		}

		/**
		 * the columns of the current ring (floor of the distance == radius) in one row
		 * @return number of columns worked
		 */
		protected int restoreRow(WorldServer world, WorldRestoration data, int dx) {
			int r = this.radius;
			int lo = r * r - dx * dx;
			int hi = (r + 1) * (r + 1) - dx * dx;
			if (hi <= 0) {
				return 1;
			}
			int zMin = ceilSqrt(lo);
			int zMax = ceilSqrt(hi) - 1;
			int n = 0;
			for (int dz = zMin; dz <= zMax; dz++) {
				data.restoreColumn(world, this.x + dx, this.z + dz);
				n++;
				if (dz != 0) {
					data.restoreColumn(world, this.x + dx, this.z - dz);
					n++;
				}
			}
			return Math.max(n, 1);
		}

		protected NBTTagCompound toNBT() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("x", this.x);
			tag.setInteger("z", this.z);
			tag.setInteger("radius", this.radius);
			tag.setInteger("row", this.row);
			return tag;
		}

		protected static Circle fromNBT(NBTTagCompound tag) {
			Circle c = new Circle(tag.getInteger("x"), tag.getInteger("z"));
			c.radius = tag.getInteger("radius");
			c.row = tag.getInteger("row");
			return c;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		this.restored = nbt.getBoolean("restored");
		this.circles.clear();
		NBTTagList list = nbt.getTagList("circles", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++) {
			this.circles.add(Circle.fromNBT(list.getCompoundTagAt(i)));
		}
		this.pending.clear();
		int[] chunks = nbt.getIntArray("pending");
		for (int i = 0; i + 1 < chunks.length; i += 2) {
			this.pending.add(chunkKey(chunks[i], chunks[i + 1]));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setBoolean("restored", this.restored);
		NBTTagList list = new NBTTagList();
		for (Circle c : this.circles) {
			list.appendTag(c.toNBT());
		}
		compound.setTag("circles", list);
		int[] chunks = new int[this.pending.size() * 2];
		int i = 0;
		for (long key : this.pending) {
			chunks[i++] = (int) (key & 0xFFFFFFFFL);
			chunks[i++] = (int) (key >>> 32);
		}
		compound.setIntArray("pending", chunks);
		return compound;
	}
}
