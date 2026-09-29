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

## Story campaign "Dawn" (Сюжетная кампания «Рассвет»)

A story of 30 missions in 5 acts with growing difficulty (config category `Campaign`: `CampaignEnabled`, `CampaignGiveRadio`, `CampaignDistancePercent`). The full story and the mission table are in [CAMPAIGN.md](CAMPAIGN.md). This version plays **missions 1-18** (acts I-III), acts IV "Plague" and V "Dawn" are in the table and follow in the next update.

* **Story** - 20 years after the Catastrophe (wars and the leak of the mutagen of project "Chimera") **Colonel Gromov** of the **Dawn Resistance** calls on the radio. The enemies are the **Legion** junta of the **General** and the mutants; the goal is to build the **Purifier** and bring life back to the land.
* **Start** - in the start bunker of the Apocalypse world the **Radio Station** on the wall calls the player ("answer the call"), in other worlds the player gets a **Radio** item on the first join. Right click opens the dialog: act and difficulty stars, briefing, goal with progress, hint, reward, accept / report buttons and the **mission journal** (one page per act).
* **Table driven missions** (`CampaignMissions`): number, act, objective type and parameters, place (site and distance), reward (loot table, experience, items), dialog keys and whether the mission is reported by radio or in person at the commander. Objective types: TALK, REACH, KILL, COLLECT, DELIVER, ESCORT, DEFEND (waves with a progress bar), DESTROY (targets and garrison), SURVIVE (timer), BOSS, ITEM.
* **Growing difficulty** - objectives move from ~100 blocks to 1500 blocks away, enemies go zombies → bandits → Legion soldiers → elite, heavy soldiers and jets → bosses, rewards go T1 → T2 → T3 armor and weapons (knife, revolver, AK-47, M4, SCAR), experience and money loot.
* **Missions 1-18**: answer the call → search 3 survivor stashes → hold the shelter against 3 night waves of zombies → kill the bandit scouts → repair the radio mast (GPS) → reach the command post → clear the area → medicine from the hospital ruins → scout the Legion airfield → shoot down 2 jets → rescue Doctor Volkov → defend the post (infantry, paratroopers, elite) → blow up the fuel depot → ambush the convoy → the second level of the mine → kill the General and take his intel → lead 3 prisoners out of the camp → destroy the launch point and garrison of the airfield.
* **New places**: ruined houses with stashes, bandit camp, radio mast, **hospital ruins**, fuel depot next to a Legion base, convoy on an old road, **Legion prison camp** with an extraction point, launch point next to the airfield; existing structures (command post, airfield, bunker, military base, underground mine) are reused. Structures are built when the player gets close, the airfield scouted in act II is the one taken at the end of act III.
* **New content**: **Prisoner** NPC, **Antenna Parts** and **Medicine Kit** quest items, **Fuel Tank** (explodes 3 seconds after it was broken, chain reaction) and **Flight Control Console** sabotage targets.
* **GPS Navigator** (reward of mission 5) points to the current objective; before that the chat gives distance, direction and coordinates.
* **Progress** is stored per player (capability, synced to the client). The progress of the old 10 mission campaign is converted to the matching new mission (with a chat message), the old command post is reused. All texts are in English and Russian.
* **Testing**: `/tgcampaign set <1-30>` jumps to a mission (accept it via the radio; builds a command post next to you if needed), `/tgcampaign complete` finishes the current mission with its rewards, `/tgcampaign reset` restarts, `/tgcampaign info` prints the state; `/tgstructure <stash_house|bandit_camp|radio_mast|hospital|convoy|prison_camp|evac|fuel_depot|launch_point>` places the new campaign places.

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
