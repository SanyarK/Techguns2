package techguns.world.wasteland;

import java.util.Random;

import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import techguns.TGConfig;

/**
 * All wasteland biomes of the apocalypse world share this class, the kind decides the surface,
 * the vegetation, the colors and which ruins are generated.
 */
public class BiomeWasteland extends Biome {

	public enum Kind {
		WASTELAND, SCORCHED_HILLS, DEAD_FOREST, RADIOACTIVE_ZONE, DRIED_SEA, CITY_RUINS
	}

	protected static final IBlockState GRASS = Blocks.GRASS.getDefaultState();
	protected static final IBlockState DIRT = Blocks.DIRT.getDefaultState();
	protected static final IBlockState COARSE_DIRT = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
	protected static final IBlockState PODZOL = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL);
	protected static final IBlockState SAND = Blocks.SAND.getDefaultState();
	protected static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
	protected static final IBlockState STONE = Blocks.STONE.getDefaultState();
	protected static final IBlockState COBBLESTONE = Blocks.COBBLESTONE.getDefaultState();
	protected static final IBlockState CLAY = Blocks.CLAY.getDefaultState();
	protected static final IBlockState HARDENED_CLAY = Blocks.HARDENED_CLAY.getDefaultState();

	public final Kind kind;
	protected final WorldGenDeadTree deadTree;

	public BiomeWasteland(Kind kind, BiomeProperties properties) {
		super(properties);
		this.kind = kind;
		this.deadTree = new WorldGenDeadTree(false, kind == Kind.SCORCHED_HILLS, kind == Kind.DEAD_FOREST);
		this.topBlock = COARSE_DIRT;
		this.fillerBlock = DIRT;

		//almost no animals, husks don't burn in the sun
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		if (kind == Kind.WASTELAND || kind == Kind.DEAD_FOREST || kind == Kind.DRIED_SEA) {
			this.spawnableCreatureList.add(new SpawnListEntry(EntityRabbit.class, 2, 1, 2));
		}
		if (kind == Kind.WASTELAND || kind == Kind.DRIED_SEA || kind == Kind.SCORCHED_HILLS || kind == Kind.CITY_RUINS) {
			this.spawnableMonsterList.add(new SpawnListEntry(EntityHusk.class, 40, 2, 4));
		}

		this.decorator.flowersPerChunk = 0;
		this.decorator.reedsPerChunk = 0;
		this.decorator.generateFalls = false;
		this.decorator.treesPerChunk = 0;
		switch (kind) {
		case DEAD_FOREST:
			this.decorator.treesPerChunk = 5;
			this.decorator.grassPerChunk = 3;
			this.decorator.deadBushPerChunk = 2;
			break;
		case SCORCHED_HILLS:
			this.decorator.extraTreeChance = 0.15f;
			this.decorator.grassPerChunk = 0;
			this.decorator.deadBushPerChunk = 2;
			break;
		case DRIED_SEA:
			this.decorator.extraTreeChance = 0.0f;
			this.decorator.grassPerChunk = 0;
			this.decorator.deadBushPerChunk = 1;
			break;
		case CITY_RUINS:
			this.decorator.extraTreeChance = 0.02f;
			this.decorator.grassPerChunk = 0;
			this.decorator.deadBushPerChunk = 1;
			break;
		default:
			this.decorator.extraTreeChance = 0.08f;
			this.decorator.grassPerChunk = 1;
			this.decorator.deadBushPerChunk = 3;
			break;
		}
	}

	protected static IBlockState terracotta(EnumDyeColor color) {
		return Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, color);
	}

	@Override
	public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
		//patches of different ground follow the surface noise, the random part blurs their borders
		double n = noise + rand.nextDouble() * 0.5D - 0.25D;
		IBlockState top = COARSE_DIRT;
		IBlockState filler = DIRT;
		switch (this.kind) {
		case SCORCHED_HILLS:
			if (n > 1.4D) {
				top = STONE;
				filler = STONE;
			} else if (n > 0.6D) {
				top = terracotta(EnumDyeColor.BLACK);
				filler = HARDENED_CLAY;
			} else if (n < -1.2D) {
				top = GRAVEL;
			}
			break;
		case DEAD_FOREST:
			if (n > 1.2D) {
				top = PODZOL;
			} else if (n > -1.0D) {
				top = GRASS;
			}
			break;
		case RADIOACTIVE_ZONE:
			if (n > 1.5D) {
				top = terracotta(EnumDyeColor.GREEN);
				filler = HARDENED_CLAY;
			} else if (n > 0.9D) {
				top = terracotta(EnumDyeColor.LIME);
				filler = HARDENED_CLAY;
			} else if (n < -1.3D) {
				top = GRAVEL;
			} else if (n > -0.2D && n < 0.2D) {
				top = GRASS;
			}
			break;
		case DRIED_SEA:
			//salt flat: white crust over sand with patches of dried clay
			if (n > 1.2D) {
				top = SAND;
				filler = SAND;
			} else if (n < -1.4D) {
				top = GRAVEL;
				filler = SAND;
			} else if (n < -0.8D) {
				top = CLAY;
				filler = CLAY;
			} else {
				top = terracotta(EnumDyeColor.WHITE);
				filler = SAND;
			}
			break;
		case CITY_RUINS:
			if (n > 1.0D) {
				top = GRAVEL;
			} else if (n < -1.2D) {
				top = COBBLESTONE;
			}
			break;
		default:
			if (n > 1.8D) {
				top = SAND;
				filler = SAND;
			} else if (n > 1.0D) {
				top = n > 1.4D ? HARDENED_CLAY : terracotta(EnumDyeColor.BROWN);
				filler = HARDENED_CLAY;
			} else if (n < -1.6D) {
				top = GRAVEL;
			} else if (n > -0.2D && n < 0.3D) {
				top = GRASS;
			}
			break;
		}
		this.topBlock = top;
		this.fillerBlock = filler;
		this.generateBiomeTerrain(world, rand, primer, x, z, noise);
	}

	@Override
	public WorldGenAbstractTree getRandomTreeFeature(Random rand) {
		return this.deadTree;
	}

	@Override
	public void decorate(World world, Random rand, BlockPos pos) {
		super.decorate(world, rand, pos);
		//like the vanilla decorators everything stays inside the chunk area shifted by +8
		if (TGConfig.wastelandRuins) {
			WastelandRuins.decorate(world, rand, pos.getX() + 8, pos.getZ() + 8, this);
		}
	}

	@Override
	public float getSpawningChance() {
		return 0.02f;
	}

	@Override
	public boolean ignorePlayerSpawnSuitability() {
		return true;
	}

	/**
	 * RGB color of the dust fog
	 */
	public int getFogColor() {
		switch (this.kind) {
		case RADIOACTIVE_ZONE:
			return 0x72A04A;
		case DRIED_SEA:
			return 0xC2B9A0;
		case CITY_RUINS:
			return 0x8C8780;
		case SCORCHED_HILLS:
			return 0x85705C;
		case DEAD_FOREST:
			return 0x8F8672;
		default:
			return 0xA8946F;
		}
	}

	/**
	 * 0..1, how much the fog replaces the normal fog and how close it comes
	 */
	public float getFogStrength() {
		switch (this.kind) {
		case RADIOACTIVE_ZONE:
			return 0.9f;
		case CITY_RUINS:
		case SCORCHED_HILLS:
			return 0.7f;
		case DEAD_FOREST:
			return 0.5f;
		default:
			return 0.6f;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int getSkyColorByTemp(float currentTemperature) {
		switch (this.kind) {
		case RADIOACTIVE_ZONE:
			return 0x8A9C78;
		case DRIED_SEA:
			return 0xA2A7A6;
		default:
			return 0x8F887A;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int getGrassColorAtPos(BlockPos pos) {
		switch (this.kind) {
		case RADIOACTIVE_ZONE:
			return 0x9AA44E;
		case DEAD_FOREST:
			return 0x877B5C;
		default:
			return 0x8E8458;
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int getFoliageColorAtPos(BlockPos pos) {
		switch (this.kind) {
		case RADIOACTIVE_ZONE:
			return 0x8C9A48;
		default:
			return 0x7D7358;
		}
	}
}
