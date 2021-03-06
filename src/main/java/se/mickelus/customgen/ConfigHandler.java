package se.mickelus.customgen;

import java.io.File;


import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {

	
	public static void init(File file) {
		Configuration config = new Configuration(file);
		config.load();
		
		Constants.DUNGEON_CHANCE_SURFACE = config.getInt(
				"dungeon_distribution_surface", "generation_settings",
				Constants.DUNGEON_CHANCE_SURFACE, 1, 4096,
				"Per how many chunks will a surface level gen generate.");
		
		Constants.DUNGEON_CHANCE_UNDERGROUND = config.getInt(
				"dungeon_distribution_underground", "generation_settings",
				Constants.DUNGEON_CHANCE_UNDERGROUND, 1, 4096,
				"Per how many chunks will a underground level gen generate.");
		
		Constants.DUNGEON_CHANCE_SEA = config.getInt(
				"dungeon_distribution_sea", "generation_settings",
				Constants.DUNGEON_CHANCE_SEA, 1, 4096,
				"Per how many chunks will a seafloor level gen generate.");
		
		Constants.SEGMENT_LIMIT = config.getInt(
				"segment_limit", "generation_settings",
				Constants.SEGMENT_LIMIT, 1, 4096,
				"The maximum amount of segments generated during the generation of a gen.");
		
		config.save();
	}
}
