package techguns.world.wasteland;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.BiomeProperties;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.BiomeManager.BiomeType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import techguns.TGConfig;
import techguns.Techguns;
import techguns.world.wasteland.BiomeWasteland.Kind;

/**
 * Wasteland biomes and the Apocalypse world type.
 * The biomes are always registered so worlds that contain them keep loading when the features are disabled.
 */
@Mod.EventBusSubscriber(modid = Techguns.MODID)
public class TGBiomes {

	public static BiomeWasteland WASTELAND;
	public static BiomeWasteland SCORCHED_HILLS;
	public static BiomeWasteland DEAD_FOREST;
	public static BiomeWasteland RADIOACTIVE_ZONE;
	public static BiomeWasteland DRIED_SEA;
	public static BiomeWasteland CITY_RUINS;

	public static WorldTypeApocalypse APOCALYPSE;

	/**
	 * called in preInit, a world type has to exist before the world selection screen opens
	 */
	public static void createWorldType() {
		if (TGConfig.apocalypseWorldType && APOCALYPSE == null) {
			APOCALYPSE = new WorldTypeApocalypse();
		}
	}

	protected static BiomeProperties props(String name, float baseHeight, float heightVariation) {
		return new BiomeProperties(name).setBaseHeight(baseHeight).setHeightVariation(heightVariation).setTemperature(2.0f).setRainfall(0.0f).setRainDisabled().setWaterColor(0x6E6A46);
	}

	@SubscribeEvent
	public static void registerBiomes(RegistryEvent.Register<Biome> event) {
		WASTELAND = new BiomeWasteland(Kind.WASTELAND, props("Wasteland", 0.1f, 0.02f));
		SCORCHED_HILLS = new BiomeWasteland(Kind.SCORCHED_HILLS, props("Scorched Hills", 0.45f, 0.35f));
		DEAD_FOREST = new BiomeWasteland(Kind.DEAD_FOREST, props("Dead Forest", 0.15f, 0.08f));
		RADIOACTIVE_ZONE = new BiomeWasteland(Kind.RADIOACTIVE_ZONE, props("Radioactive Zone", 0.1f, 0.04f).setWaterColor(0x7FBF3F));
		//below the neighbours, the apocalypse world has a lower sea level so the basin stays dry
		DRIED_SEA = new BiomeWasteland(Kind.DRIED_SEA, props("Dried Sea", -0.35f, 0.0f));
		CITY_RUINS = new BiomeWasteland(Kind.CITY_RUINS, props("City Ruins", 0.1f, 0.0f));

		IForgeRegistry<Biome> reg = event.getRegistry();
		register(reg, WASTELAND, "wasteland", Type.WASTELAND, Type.DEAD, Type.DRY, Type.HOT, Type.SANDY);
		register(reg, SCORCHED_HILLS, "scorched_hills", Type.WASTELAND, Type.DEAD, Type.DRY, Type.HOT, Type.HILLS);
		register(reg, DEAD_FOREST, "dead_forest", Type.WASTELAND, Type.DEAD, Type.DRY, Type.FOREST, Type.SPOOKY);
		register(reg, RADIOACTIVE_ZONE, "radioactive_zone", Type.WASTELAND, Type.DEAD, Type.DRY, Type.SPOOKY);
		register(reg, DRIED_SEA, "dried_sea", Type.WASTELAND, Type.DEAD, Type.DRY, Type.HOT, Type.SANDY);
		register(reg, CITY_RUINS, "city_ruins", Type.WASTELAND, Type.DEAD, Type.DRY);

		if (TGConfig.wastelandBiomesInNormalWorlds) {
			int w = TGConfig.wastelandBiomeWeight;
			BiomeManager.addBiome(BiomeType.DESERT, new BiomeEntry(WASTELAND, w));
			BiomeManager.addBiome(BiomeType.DESERT, new BiomeEntry(SCORCHED_HILLS, w));
			BiomeManager.addBiome(BiomeType.DESERT, new BiomeEntry(RADIOACTIVE_ZONE, w));
			BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(CITY_RUINS, w));
			BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(DEAD_FOREST, w));
			BiomeManager.addBiome(BiomeType.COOL, new BiomeEntry(DEAD_FOREST, w));
		}
	}

	protected static void register(IForgeRegistry<Biome> reg, Biome biome, String name, Type... types) {
		biome.setRegistryName(new ResourceLocation(Techguns.MODID, name));
		reg.register(biome);
		BiomeDictionary.addTypes(biome, types);
	}

	public static boolean isWasteland(Biome biome) {
		return biome instanceof BiomeWasteland;
	}
}
