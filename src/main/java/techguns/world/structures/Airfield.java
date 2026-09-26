package techguns.world.structures;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import techguns.TGBlocks;
import techguns.Techguns;
import techguns.blocks.EnumConcreteType;
import techguns.blocks.EnumMonsterSpawnerType;
import techguns.blocks.TGMetalPanelType;
import techguns.entities.npcs.ArmySoldier;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.HeavySoldier;
import techguns.entities.npcs.MilitaryJet;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.util.BlockUtils;

/**
 * Military airfield with a runway, a hangar, a control tower and barracks. Jets take off from here.
 * Built in local coordinates u (along the runway) and v (across), the axes are randomly swapped.
 */
public class Airfield extends WorldgenStructure {

	private static final ResourceLocation CHEST_LOOT = new ResourceLocation(Techguns.MODID, "chests/airfield");

	protected static final int LENGTH = 48;
	protected static final int WIDTH = 26;

	public Airfield() {
		super(LENGTH, 14, WIDTH, LENGTH, 14, WIDTH);
		this.setXZSize(LENGTH, WIDTH);
		this.lootTier = techguns.world.EnumLootType.TIER1;
	}

	@Override
	protected int getStep() {
		return 8;
	}

	@Override
	public void spawnStructureWorldgen(World world, int chunkX, int chunkZ, int sizeX, int sizeY, int sizeZ, Random rnd, Biome biome) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		boolean swap = rnd.nextBoolean();
		int y = BlockUtils.getValidSpawnYArea(world, x, z, swap ? WIDTH : LENGTH, swap ? LENGTH : WIDTH, 6, this.getStep());
		if (y < 5) {
			return;
		}
		this.build(world, x, y - 1, z, swap, rnd);
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		this.build(world, posX, posY, posZ, direction % 2 == 1, rnd);
	}

	/**
	 * Maps the local coordinates to world coordinates
	 */
	protected static class Local {
		final StructureBuilder b;
		final int posX;
		final int posZ;
		final boolean swap;

		Local(StructureBuilder b, int posX, int posZ, boolean swap) {
			this.b = b;
			this.posX = posX;
			this.posZ = posZ;
			this.swap = swap;
		}

		int x(int u, int v) {
			return this.swap ? this.posX + v : this.posX + u;
		}

		int z(int u, int v) {
			return this.swap ? this.posZ + u : this.posZ + v;
		}

		/** direction of +u */
		EnumFacing facingU() {
			return this.swap ? EnumFacing.SOUTH : EnumFacing.EAST;
		}

		/** direction of +v */
		EnumFacing facingV() {
			return this.swap ? EnumFacing.EAST : EnumFacing.SOUTH;
		}

		void set(int u, int y, int v, IBlockState state) {
			this.b.set(this.x(u, v), y, this.z(u, v), state);
		}

		void fill(int u1, int y1, int v1, int u2, int y2, int v2, IBlockState state) {
			this.b.fill(this.x(u1, v1), y1, this.z(u1, v1), this.x(u2, v2), y2, this.z(u2, v2), state);
		}

		void clear(int u1, int y1, int v1, int u2, int y2, int v2) {
			this.b.clear(this.x(u1, v1), y1, this.z(u1, v1), this.x(u2, v2), y2, this.z(u2, v2));
		}

		void room(int u1, int y1, int v1, int u2, int y2, int v2, IBlockState wall, IBlockState floor, IBlockState ceiling) {
			int xa = this.x(u1, v1);
			int xb = this.x(u2, v2);
			int za = this.z(u1, v1);
			int zb = this.z(u2, v2);
			this.b.room(Math.min(xa, xb), y1, Math.min(za, zb), Math.max(xa, xb), y2, Math.max(za, zb), wall, floor, ceiling);
		}

		TGSpawnerTileEnt spawner(int u, int y, int v, int mobsLeft, int maxActive, int delay) {
			return this.b.spawner(this.x(u, v), y, this.z(u, v), EnumMonsterSpawnerType.SOLDIER_SPAWN, mobsLeft, maxActive, delay, 0);
		}
	}

	protected void build(World world, int posX, int posY, int posZ, boolean swap, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);
		Local l = new Local(b, posX, posZ, swap);
		int g = posY;

		IBlockState concreteDark = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState concreteGrey = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState concreteBrown = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN);
		IBlockState white = Blocks.CONCRETE.getStateFromMeta(0);
		IBlockState yellow = Blocks.CONCRETE.getStateFromMeta(4);
		IBlockState light = TGBlocks.NEONLIGHT_BLOCK.getDefaultState();
		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();

		//level the whole area
		b.prepareGround(l.x(0, 0), l.z(0, 0), l.x(LENGTH - 1, WIDTH - 1), l.z(LENGTH - 1, WIDTH - 1), g + 1, 12, Blocks.DIRT.getDefaultState());

		/*
		 * RUNWAY
		 */
		l.fill(0, g, 1, LENGTH - 1, g, 7, concreteDark);
		for (int u = 0; u < LENGTH; u++) {
			if (u % 5 < 3) {
				l.set(u, g, 4, white);
			}
			if (u % 6 == 0) {
				l.set(u, g, 0, light);
				l.set(u, g, 8, light);
			}
		}
		for (int v = 2; v <= 6; v += 2) {
			l.fill(1, g, v, 3, g, v, white);
			l.fill(LENGTH - 4, g, v, LENGTH - 2, g, v, white);
		}

		/*
		 * APRON
		 */
		l.fill(2, g, 9, 35, g, WIDTH - 1, concreteGrey);

		/*
		 * HANGAR
		 */
		IBlockState hangarWall = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_BLUE);
		IBlockState hangarRoof = StructureBuilder.metalPanel(TGMetalPanelType.PANEL_LARGE_BORDER);
		l.room(5, g + 1, 13, 18, g + 7, 23, hangarWall, concreteGrey, hangarRoof);
		l.clear(6, g + 1, 12, 17, g + 6, 12);
		for (int u = 7; u <= 16; u += 4) {
			for (int v = 15; v <= 21; v += 6) {
				b.ceilingLamp(l.x(u, v), g + 7, l.z(u, v));
			}
		}
		this.buildParkedJet(l, g);
		b.lootChest(l.x(5, 23), g + 1, l.z(5, 23), l.facingV().getOpposite(), CHEST_LOOT);
		b.lootChest(l.x(18, 23), g + 1, l.z(18, 23), l.facingV().getOpposite(), CHEST_LOOT);
		for (int u = 6; u <= 9; u++) {
			b.crateStack(l.x(u, 23), g + 1, l.z(u, 23), 3);
		}
		b.crateStack(l.x(17, 23), g + 1, l.z(17, 23), 2);
		TGSpawnerTileEnt hangarGuard = l.spawner(16, g + 1, 20, 3, 1, 300);
		StructureBuilder.addMob(hangarGuard, HeavySoldier.class, 1);
		StructureBuilder.addMob(hangarGuard, ArmySoldier.class, 2);

		/*
		 * JET TAKEOFF POINT, jets spawn high above it
		 */
		l.fill(25, g, 13, 29, g, 17, yellow);
		l.fill(26, g, 14, 28, g, 16, concreteDark);
		TGSpawnerTileEnt jets = l.spawner(27, g + 1, 15, 2, 1, 1200);
		StructureBuilder.addMob(jets, MilitaryJet.class, 1);
		if (jets != null) {
			int h = Math.min(g + 24, world.getActualHeight() - 6) - g;
			jets.setSpawnHeightOffset(h);
		}

		/*
		 * CONTROL TOWER
		 */
		l.room(38, g + 1, 13, 40, g + 9, 15, concreteGrey, concreteGrey, concreteGrey);
		l.room(36, g + 11, 11, 42, g + 13, 17, Blocks.GLASS.getDefaultState(), concreteGrey, concreteGrey);
		for (int y = g + 11; y <= g + 13; y++) {
			l.set(35, y, 10, concreteGrey);
			l.set(43, y, 10, concreteGrey);
			l.set(35, y, 18, concreteGrey);
			l.set(43, y, 18, concreteGrey);
		}
		b.ladder(l.x(39, 15), g + 1, g + 10, l.z(39, 15), l.facingV());
		l.clear(39, g + 1, 12, 39, g + 2, 12);
		b.door(l.x(39, 12), g + 1, l.z(39, 12), l.facingV().getOpposite());
		b.ceilingLamp(l.x(39, 13), g + 9, l.z(39, 13));
		b.ceilingLamp(l.x(37, 12), g + 13, l.z(37, 12));
		b.ceilingLamp(l.x(41, 16), g + 13, l.z(41, 16));
		l.set(37, g + 11, 16, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN));
		l.set(38, g + 11, 16, StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN));
		TGSpawnerTileEnt sniper = l.spawner(41, g + 11, 12, 2, 1, 400);
		StructureBuilder.addMob(sniper, EliteSoldier.class, 1);

		/*
		 * BARRACKS
		 */
		l.room(24, g + 1, 20, 33, g + 3, 24, concreteBrown, concreteGrey, concreteGrey);
		l.clear(28, g + 1, 19, 28, g + 2, 19);
		b.door(l.x(28, 19), g + 1, l.z(28, 19), l.facingV().getOpposite());
		IBlockState bunk = Blocks.WOOL.getStateFromMeta(13);
		IBlockState post = Blocks.OAK_FENCE.getDefaultState();
		for (int u = 24; u <= 33; u += 3) {
			l.set(u, g + 1, 24, bunk);
			l.set(u, g + 2, 24, post);
			l.set(u, g + 3, 24, bunk);
		}
		b.ceilingLamp(l.x(26, 22), g + 3, l.z(26, 22));
		b.ceilingLamp(l.x(31, 22), g + 3, l.z(31, 22));
		b.lootChest(l.x(33, 21), g + 1, l.z(33, 21), l.facingU().getOpposite(), CHEST_LOOT);
		TGSpawnerTileEnt barracks = l.spawner(28, g + 1, 22, 4, 2, 250);
		StructureBuilder.addMob(barracks, ArmySoldier.class, 3);
		StructureBuilder.addMob(barracks, EliteSoldier.class, 1);

		/*
		 * FUEL DEPOT
		 */
		IBlockState tank = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_RED);
		for (int u = 37; u <= 43; u += 3) {
			l.fill(u, g + 1, 21, u + 1, g + 3, 22, tank);
		}
		for (int u = 36; u <= 45; u++) {
			l.set(u, g + 1, 19, sandbags);
			l.set(u, g + 1, 24, sandbags);
		}
		TGSpawnerTileEnt depot = l.spawner(40, g + 1, 20, 2, 1, 300);
		StructureBuilder.addMob(depot, HeavySoldier.class, 1);

		/*
		 * GUARD POSTS on the apron
		 */
		for (int u = 21; u <= 33; u += 12) {
			for (int du = -1; du <= 1; du++) {
				l.set(u + du, g + 1, 10, sandbags);
				l.set(u + du, g + 1, 12, sandbags);
			}
			l.set(u - 1, g + 1, 11, sandbags);
			l.set(u + 1, g + 1, 11, sandbags);
			TGSpawnerTileEnt guard = l.spawner(u, g + 1, 11, 3, 1, 300);
			StructureBuilder.addMob(guard, ArmySoldier.class, 1);
		}
	}

	/**
	 * Decorative jet made of blocks inside the hangar, nose points to +u
	 */
	protected void buildParkedJet(Local l, int g) {
		IBlockState body = Blocks.CONCRETE.getStateFromMeta(8);
		IBlockState cockpit = Blocks.STAINED_GLASS.getStateFromMeta(3);
		IBlockState wing = Blocks.STONE_SLAB.getStateFromMeta(0);
		IBlockState gear = Blocks.OAK_FENCE.getDefaultState();
		int v = 18;

		l.set(9, g + 1, v, gear);
		l.set(13, g + 1, v - 1, gear);
		l.set(13, g + 1, v + 1, gear);

		for (int u = 7; u <= 15; u++) {
			l.set(u, g + 2, v, body);
		}
		l.set(16, g + 2, v, Blocks.IRON_BLOCK.getDefaultState());
		l.set(13, g + 3, v, cockpit);
		l.set(14, g + 3, v, cockpit);

		for (int dv = -4; dv <= 4; dv++) {
			if (dv == 0) continue;
			l.set(11, g + 2, v + dv, wing);
			l.set(12, g + 2, v + dv, wing);
		}
		for (int dv = -2; dv <= 2; dv++) {
			if (dv == 0) continue;
			l.set(7, g + 2, v + dv, wing);
		}
		l.set(7, g + 3, v, body);
		l.set(7, g + 4, v, body);
		l.set(8, g + 3, v, body);
	}
}
