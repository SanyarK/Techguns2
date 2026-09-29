# Techguns2

1.12.2 Port/Rewrite of Techguns mod for Minecraft. WIP

Techguns 2 is still an early version and not well tested. Bugs may happen. Use at own risk.

Mod Information: https://github.com/pWn3d1337/Techguns2/wiki

## Military expansion

New content added on top of Techguns 2:

* **New NPCs**: Elite Soldier (T3 combat armor, SCAR/AUG/Vector/AS50), Heavy Soldier (riot armor, minigun/rocket launcher/flamethrower), Paratrooper (glides down on a parachute), Mutant Warrior (fast melee super mutant). Elite/Heavy soldiers and Mutant Warriors spawn naturally far away from the world spawn (danger level 3).
* **Bosses** (boss bar, no despawn):
  * **General** - calls in elite soldier reinforcements, switches to a rocket launcher at half health and orders airstrikes on his target when outdoors.
  * **Mutant Warlord** - giant super mutant, smashes the ground, summons mutant warriors, goes berserk at low health.
* **Attack Jet** - flies attack runs, strafes with its cannon, fires rockets when passing over its target and drops paratroopers. Random **air raids** attack players far away from spawn (configurable in the "NPC Spawn" config category).
* **Structures** (config `SpawnMilitaryExpansionStructures`):
  * **Underground Military Mine** (big) - guarded mine head, shaft to a level with barracks, armory and a mining gallery, a staircase down to the General's command center.
  * **Airfield** (big) - runway, hangar with a parked jet, control tower, barracks, fuel depot, jets take off from here.
  * **Bunker Complex** (medium) - guarded entrance, underground hall, armory and dormitory.
  * **Mutant Lair** (medium) - ruined crater with the Mutant Warlord and his warriors.
  * Existing military bases now also contain elite and heavy soldiers.
* **Military Contracts** (quests) - found in the new structures and dropped by bosses, or crafted from paper, an ink sac and a gold ingot. Right click to get a random assignment (kill soldiers, mutants, undead, aircraft, a boss or any hostiles), kills are counted while the contract is in your inventory. Right click a completed contract to get loot, experience and combat buffs (tier I-III).
* **Testing**: operators can place the new structures around themselves with `/tgstructure <underground_mine|airfield|bunker|mutant_lair>` and start an air raid with `/tgstructure airraid`.

## Story campaign (Сюжетная кампания)

A 10 mission story campaign on top of the military expansion (config category `Campaign`, `CampaignEnabled`):

* **Radio** - given to every player on their first world join (also craftable: iron, redstone, an iron plate and iron bars). Right click to contact **Colonel Sokolov** from anywhere: a dialog screen with the mission briefing, accept / turn in buttons and the **mission journal**.
* **Commander NPC** - friendly quest giver at your personal **Command Post** (placed by mission 1). Talking to him in person is required for a few key missions (first contact, escort, the finale).
* **Mission objectives on the map** - when a mission starts, a point 300-600 blocks away (configurable) is chosen and saved in the world data; the objective structure is built when the player approaches it. The **GPS Navigator** item (given in mission 1, craftable with a compass) shows direction and distance to the current objective in the action bar while held.
* **The mission chain**: reach the command post → prove yourself in combat → scout an airfield → shoot down 2 attack jets → free the **Captured Scientist** from a bunker and escort him back → kill the **General** and take his **Intel Documents** → find the secret **Mutagen Lab** → steal the **Mutagen Sample** from its safe → kill the **Prototype** → return for the ceremony.
* **Underground Mutagen Laboratory** - big 3-level structure: a lab hall with holding cages and equipment, a containment block with mutant cells and the sample safe behind a bunker door, and the boss hall with giant mutagen vats.
* **The Prototype** - final boss, a mutated super soldier with a boss health bar and three phases: melee brawler → grabs a minigun and summons mutants → berserk with shockwaves.
* **Rewards** - experience and military contracts per mission, the unique rifle "General's Talon", the "Prototype Slayer" power armor pieces, the golden revolver "Retribution" and a server-wide title announcement ("Scourge of the Wasteland") in the finale.
* **Progress** is stored per player (capability, synced to the client), all texts are in English and Russian.
* **Testing**: `/tgcampaign set <1-10>` jumps to a mission (accept it via the radio), `/tgcampaign reset` restarts the campaign, `/tgcampaign info` prints the state, `/tgstructure mutagen_lab` and `/tgstructure command_post` place the new structures.

## Apocalypse world (Мир постапокалипсиса)

A post-apocalyptic world type with endless wastelands and a start bunker (config category `Apocalypse`, every part can be switched off):

* **World type "Apocalypse"** - choose it in *More World Options → World Type* (dedicated servers: `level-type=tg_apocalypse`). Its own biome provider generates only wasteland biomes in big regions (`ApocalypseBiomeSize`, default 5 = twice the vanilla size), no oceans, rivers, villages or temples. The sea level is lowered to 50 so the dried sea stays dry, water lakes are rare. `WastelandBiomesInNormalWorlds` adds the biomes (except the dried sea) to normal worlds.
* **Biomes** (BiomeDictionary WASTELAND/DEAD/DRY, so Techguns NPCs spawn with danger level 2), grey-brown grass and foliage, a dull sky, dust fog (client option `WastelandFog`), almost no animals but husks:
  * **Wasteland** - flat cracked ground of coarse dirt, sand, gravel and terracotta, dry grass, dead bushes and rare dead trees.
  * **Scorched Hills** - burnt hills of stone, black terracotta and gravel with charred trees.
  * **Dead Forest** - leafless dead trees.
  * **Radioactive Zone** - green terracotta, uranium ore at the surface and a greenish fog.
  * **Dried Sea** - a salt flat basin with ship wrecks and bones.
  * **City Ruins** - every chunk is one city block with roads along the chunk borders: ruined high rises (collapsed corners and floors, broken windows, rebar, rubble, a ladder to the upper floors), burnt houses, collapsed buildings, parking lots, bomb craters, dead parks, abandoned cars and street lamps, loot chests and rare spawners with zombies, bandits or mutants.
* **Ruins in the other wastelands** (`WastelandRuinRarity`): single ruined houses, broken highways, gas stations, radio towers with a working radio, bomb craters, car wrecks and ship wrecks. Everything is generated inside the chunk area shifted by +8 like vanilla decorators, so no cascading worldgen: a city block is built in four parts, each part when the area over it is populated (like vanilla structures). The existing Techguns structures (bases, bunkers, airfield, mine, mutant lair) spawn in the wastelands as well.
* **Start bunker** - when an Apocalypse world is created a small shelter is built under the spawn and the world spawn is moved inside (stored in the world data, built only once): bed, chest with a start kit (food, bandages, pistol, magazines, torches, a water bucket), workbench, furnace, a **Radio Station** on the wall (right click opens the commander dialog of the story campaign) and a ladder up to a hatch. `StartBunkerNormalWorlds` builds it in new worlds of other types too.
* **New items**: Radio Station block (crafted from the radio item and iron) and Bandage (paper and string, heals 2 hearts).
* **Testing**: `/tgstartbunker [nospawn]` builds the bunker under the player, moves the world spawn inside and teleports the player in; `/tgstructure <city_block|ruined_building|ruined_house|burnt_house|gas_station|radio_tower|crater|road|car_wreck|shipwreck>` places a ruin in the 16x16 area around the player.

## Building

Minecraft 1.12.2 / ForgeGradle 2.3 needs **Java 8** and **Gradle 4.10.3**: `gradle build`, the mod jar is in `build/libs`. The GitHub Actions workflow in `.github/workflows/build.yml` builds the jar on every push and uploads it as the `techguns-mod-jar` artifact.
