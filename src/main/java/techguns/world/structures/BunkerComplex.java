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
import techguns.entities.npcs.Commando;
import techguns.entities.npcs.EliteSoldier;
import techguns.entities.npcs.HeavySoldier;
import techguns.tileentities.TGSpawnerTileEnt;
import techguns.util.BlockUtils;

/**
 * Medium military bunker. A guarded entrance on the surface, the actual bunker with
 * a command hall, armory and dormitory is underground.
 */
public class BunkerComplex extends WorldgenStructure {

	private static final ResourceLocation CHEST_LOOT = new ResourceLocation(Techguns.MODID, "chests/bunker_complex");

	protected static final int SIZE = 15;
	protected static final int DEPTH = 8;

	public BunkerComplex() {
		super(SIZE, 12, SIZE, SIZE, 12, SIZE);
		this.setXZSize(SIZE, SIZE);
		this.lootTier = techguns.world.EnumLootType.TIER1;
	}

	@Override
	public void spawnStructureWorldgen(World world, int chunkX, int chunkZ, int sizeX, int sizeY, int sizeZ, Random rnd, Biome biome) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		int y = BlockUtils.getValidSpawnYArea(world, x, z, SIZE, SIZE, 4, this.getStep());
		if (y < 20) {
			return;
		}
		this.setBlocks(world, x, y - 1, z, SIZE, sizeY, SIZE, 0, getBiomeColorTypeFromBiome(biome), rnd);
	}

	@Override
	public void setBlocks(World world, int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ, int direction, BiomeColorType colorType, Random rnd) {
		StructureBuilder b = new StructureBuilder(world, rnd);

		int ground = posY;
		int cx = posX + SIZE / 2;
		int cz = posZ + SIZE / 2;
		int yb = ground + 1 - DEPTH; //walking level of the underground part

		IBlockState wall = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY);
		IBlockState floor = StructureBuilder.concrete(EnumConcreteType.CONCRETE_GREY_DARK);
		IBlockState ceiling = StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN_LIGHT);
		IBlockState panel = StructureBuilder.metalPanel(TGMetalPanelType.STEELFRAME_DARK);

		/*
		 * UNDERGROUND
		 */
		//hall, armory (west), dormitory (east)
		b.room(cx - 6, yb, cz - 15, cx + 6, yb + 3, cz - 8, wall, floor, ceiling);
		b.room(cx - 14, yb, cz - 14, cx - 8, yb + 3, cz - 9, panel, floor, ceiling);
		b.room(cx + 8, yb, cz - 14, cx + 14, yb + 3, cz - 9, wall, floor, ceiling);
		b.clear(cx - 7, yb, cz - 12, cx - 7, yb + 2, cz - 11);
		b.clear(cx + 7, yb, cz - 12, cx + 7, yb + 2, cz - 11);

		//staircase from the entrance building down to the hall, going north
		for (int i = 1; i <= DEPTH; i++) {
			int z = cz + 2 - i;
			int walk = ground + 1 - i;
			b.tunnel(cx - 1, walk, z, cx + 1, walk + 3, z, wall, floor, wall);
		}
		b.clear(cx - 1, yb, cz - 7, cx + 1, yb + 2, cz - 7);

		//hall details
		for (int x = cx - 4; x <= cx + 4; x += 4) {
			b.ceilingLamp(x, yb + 3, cz - 10);
			b.ceilingLamp(x, yb + 3, cz - 13);
		}
		IBlockState radio = StructureBuilder.metalPanel(TGMetalPanelType.CONTAINER_GREEN);
		for (int x = cx - 2; x <= cx + 2; x++) {
			b.set(x, yb, cz - 15, radio);
		}
		b.crateStack(cx - 6, yb, cz - 15, 3);
		b.crateStack(cx + 6, yb, cz - 15, 3);
		b.crateStack(cx - 6, yb, cz - 8, 2);
		b.crateStack(cx + 6, yb, cz - 8, 2);
		TGSpawnerTileEnt hall = b.spawner(cx, yb, cz - 12, EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1);
		StructureBuilder.addMob(hall, HeavySoldier.class, 1);
		StructureBuilder.addMob(hall, EliteSoldier.class, 1);
		TGSpawnerTileEnt hall2 = b.spawner(cx + 4, yb, cz - 10, EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 2, 250, 1);
		StructureBuilder.addMob(hall2, ArmySoldier.class, 2);
		StructureBuilder.addMob(hall2, Commando.class, 1);

		//armory
		for (int z = cz - 14; z <= cz - 9; z++) {
			b.crateStack(cx - 14, yb, z, 3);
		}
		b.lootChest(cx - 12, yb, cz - 14, EnumFacing.SOUTH, CHEST_LOOT);
		b.lootChest(cx - 11, yb, cz - 14, EnumFacing.SOUTH, CHEST_LOOT);
		b.ceilingLamp(cx - 11, yb + 3, cz - 11);
		b.spawner(cx - 10, yb, cz - 11, EnumMonsterSpawnerType.SOLDIER_SPAWN, 3, 1, 300, 1, EliteSoldier.class);

		//dormitory
		IBlockState bunk = Blocks.WOOL.getStateFromMeta(13);
		IBlockState post = Blocks.OAK_FENCE.getDefaultState();
		for (int x = cx + 9; x <= cx + 13; x += 2) {
			for (int z = cz - 14; z <= cz - 9; z += 5) {
				b.set(x, yb, z, bunk);
				b.set(x, yb + 1, z, post);
				b.set(x, yb + 2, z, bunk);
			}
		}
		b.lootChest(cx + 14, yb, cz - 12, EnumFacing.WEST, CHEST_LOOT);
		b.ceilingLamp(cx + 11, yb + 3, cz - 11);
		b.spawner(cx + 11, yb, cz - 11, EnumMonsterSpawnerType.SOLDIER_SPAWN, 4, 2, 250, 1, ArmySoldier.class);

		/*
		 * SURFACE
		 */
		b.prepareGround(posX + 1, posZ + 1, posX + SIZE - 2, posZ + SIZE - 2, ground + 1, 6, Blocks.DIRT.getDefaultState());
		b.fill(posX + 1, ground, posZ + 1, posX + SIZE - 2, ground, posZ + SIZE - 2, StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN));

		IBlockState sandbags = TGBlocks.SANDBAGS.getDefaultState();
		for (int i = 1; i <= SIZE - 2; i++) {
			if (Math.abs(posX + i - cx) > 1) {
				b.set(posX + i, ground + 1, posZ + 1, sandbags);
				b.set(posX + i, ground + 1, posZ + SIZE - 2, sandbags);
			}
			if (Math.abs(posZ + i - cz) > 1) {
				b.set(posX + 1, ground + 1, posZ + i, sandbags);
				b.set(posX + SIZE - 2, ground + 1, posZ + i, sandbags);
			}
		}

		//entrance building
		b.room(cx - 2, ground + 1, cz - 2, cx + 2, ground + 3, cz + 2, StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN), StructureBuilder.concrete(EnumConcreteType.CONCRETE_BROWN), wall);
		for (int x = cx - 3; x <= cx + 3; x++) {
			b.set(x, ground + 5, cz - 3, sandbags);
			b.set(x, ground + 5, cz + 3, sandbags);
		}
		for (int z = cz - 2; z <= cz + 2; z++) {
			b.set(cx - 3, ground + 5, z, sandbags);
			b.set(cx + 3, ground + 5, z, sandbags);
		}
		b.door(cx, ground + 1, cz + 3, EnumFacing.SOUTH);
		b.ceilingLamp(cx - 2, ground + 3, cz + 2);
		b.ceilingLamp(cx + 2, ground + 3, cz + 2);

		//open the floor above the stairs and place them
		for (int i = 1; i <= DEPTH; i++) {
			int z = cz + 2 - i;
			int walk = ground + 1 - i;
			if (walk + 3 >= ground) {
				b.clear(cx - 1, walk, z, cx + 1, Math.min(ground, walk + 3), z);
			}
			for (int x = cx - 1; x <= cx + 1; x++) {
				b.stairs(x, walk - 1, z, TGBlocks.CONCRETE_STAIRS.getDefaultState(), EnumFacing.SOUTH);
			}
		}
		b.ceilingLamp(cx, ground - 2, cz - 4);

		//guards
		b.spawner(posX + 2, ground + 1, posZ + 2, EnumMonsterSpawnerType.SOLDIER_SPAWN, 2, 1, 300, 0, ArmySoldier.class);
		b.spawner(posX + SIZE - 3, ground + 1, posZ + SIZE - 3, EnumMonsterSpawnerType.SOLDIER_SPAWN, 2, 1, 300, 0, ArmySoldier.class);
		b.crateStack(posX + SIZE - 3, ground + 1, posZ + 2, 2);
		b.crateStack(posX + 2, ground + 1, posZ + SIZE - 3, 2);
	}
}
