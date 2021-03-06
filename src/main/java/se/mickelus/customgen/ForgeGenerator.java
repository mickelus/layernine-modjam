package se.mickelus.customgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import se.mickelus.customgen.blocks.EmptyBlock;
import se.mickelus.customgen.blocks.InterfaceBlock;
import se.mickelus.customgen.items.PlaceholderItem;
import se.mickelus.customgen.models.Gen;
import se.mickelus.customgen.models.GenManager;
import se.mickelus.customgen.models.Segment;
import se.mickelus.customgen.models.SegmentPlaceholder;

public class ForgeGenerator implements IWorldGenerator  {
	
	private static ForgeGenerator instance;
	
	
	public ForgeGenerator() {

		GameRegistry.registerWorldGenerator(this, Constants.GENERATION_WEIGHT);
		
		instance = this;
	}
	
	public static ForgeGenerator getInstance() {
		return instance;
	}

	public void generateSegment(int chunkX, int chunkZ, int y,
			Segment segment, World world, boolean generatePlaceholders, Random random) {
		int x = chunkX * 16;
		int z = chunkZ * 16;
		Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
		
		// TODO : we should not have to handle this
		if(segment == null) {
			MLogger.logf("Failed to generate a segment at %d %d %d.", x, y, z);
			return;
		}

		// generate blocks
		for(int sy = 0; sy < 16; sy++) {
			for(int sz = 0; sz < 16; sz++) {
				for(int sx = 0; sx < 16; sx++) {
					Block block = segment.getBlock(sx, sy, sz);
					if(!generatePlaceholders && (block.equals(EmptyBlock.getInstance()) || block.equals(InterfaceBlock.getInstance()) )) {
						continue;
					}
					world.setBlockState(
							new BlockPos(x+sx, y+sy, z+sz), 
							block.getStateFromMeta(segment.getBlockMetadata(sx, sy, sz)), 
							2);
				}
			}
		}
		
		for(int sy = 0; sy < 16; sy++) {
			for(int sz = 0; sz < 16; sz++) {
				for(int sx = 0; sx < 16; sx++) {
					BlockPos pos = new BlockPos(x+sx, y+sy, z+sz);
					Block block =  world.getBlockState(pos).getBlock();
					if(block instanceof IFluidBlock || block instanceof BlockLiquid) {
						world.notifyBlockOfStateChange(pos, block);
					}
					
				}
			}
		}

		// spawn tile entities
		for (int i = 0; i < segment.getNumTileEntities(); i++) {
			
			NBTTagCompound tag = segment.getTileEntityNBT(i);
			tag = updateTileEntityNBT(tag, chunkX*16, y, chunkZ*16);
			BlockPos pos = new BlockPos(
					tag.getInteger("x"),
					tag.getInteger("y"),
					tag.getInteger("z"));
			
			TileEntity tileEntity = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
			tileEntity.readFromNBT(tag);
			if (tileEntity != null) {
				
				// this is dangerous!
				try {
				
					if(tileEntity instanceof IInventory) {
						IInventory inventory = (IInventory) tileEntity;
						
						for (int j = 0; j < inventory.getSizeInventory(); j++) {
							if(!generatePlaceholders && inventory.getStackInSlot(j) != null
									&& inventory.getStackInSlot(j).getItem().equals(PlaceholderItem.getInstance())) {
								ItemStack itemStack = ChestGenHooks.getOneItem(ChestGenHooks.DUNGEON_CHEST, random);
								if(inventory.getInventoryStackLimit() < itemStack.stackSize) {
									itemStack.stackSize = inventory.getInventoryStackLimit();
								}
								if(inventory.isItemValidForSlot(j, itemStack)) {
									inventory.setInventorySlotContents(j, itemStack);
								}
							}
						}
					}
				} catch(Exception e) {
					MLogger.log("Failed when generating loot in a tile entity .");
					e.printStackTrace();
				}
            }
		}
		
		// spawn entities 
		for (int i = 0; i < segment.getNumEntities(); i++) {
			
			NBTTagCompound tag = segment.getEntityNBT(i);
			tag = updateEntityNBT(tag, world, chunkX*16, y, chunkZ*16);
			Entity entity = EntityList.createEntityFromNBT(tag, world);
			if(entity != null) {
				world.spawnEntityInWorld(entity);
				
				// spawn (potentially recursively) ridden entities
                for (NBTTagCompound tempTag = tag; entity != null && tempTag.hasKey("Riding", 10); tempTag = tempTag.getCompoundTag("Riding")) {
                    Entity riddenEntity = EntityList.createEntityFromNBT(tempTag.getCompoundTag("Riding"), world);

                    if (riddenEntity != null) {
                    	riddenEntity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
                        world.spawnEntityInWorld(riddenEntity);
                        entity.mountEntity(riddenEntity);
                    }
                    
                    entity = riddenEntity;
                }
			}
		}
	}
	
	private NBTTagCompound updateTileEntityNBT(NBTTagCompound tileEntityNBT, int x, int y, int z) {
        tileEntityNBT.setInteger("x", tileEntityNBT.getInteger("x") + x);
        tileEntityNBT.setInteger("y", tileEntityNBT.getInteger("y") + y);
        tileEntityNBT.setInteger("z", tileEntityNBT.getInteger("z") + z);
        
        return tileEntityNBT;
	}
	
	private NBTTagCompound updateEntityNBT(NBTTagCompound entityNBT, World world, int x, int y, int z) {
		
		NBTTagList oldPosition = entityNBT.getTagList(Segment.NBT_POSITION_KEY, 6);
		NBTTagList newPosition = new NBTTagList();
		newPosition.appendTag(new NBTTagDouble(oldPosition.getDouble(0) + x));
		newPosition.appendTag(new NBTTagDouble(oldPosition.getDouble(1) + y));
		newPosition.appendTag(new NBTTagDouble(oldPosition.getDouble(2) + z));
		entityNBT.setTag(Segment.NBT_POSITION_KEY, newPosition);
		
		entityNBT.setInteger(Segment.NBT_DIMENSION_KEY, world.provider.getDimensionId());
        
        return entityNBT;
	}
	
	private void createPlaceholders(int chunkX, int chunkZ, int y, Segment segment, 
			List<SegmentPlaceholder> placeholderList) {
		
		// if a ph exists for the given coordinates
		boolean exists = false;
		
		// if a ph exists for each side
		boolean [] addSide = new boolean[6];
		
		// make sure we wont add ph:s for non-interfacing sides
		for (int i = 0; i < addSide.length; i++) {
			/*if(segment.getInterface(i) != 0) {
				MLogger.logf("side exists: %d", i);
			}*/
			addSide[i] = segment.getInterface(i) != 0;
		}
		/*	   0 - top
		 *     1 - bottom
		 *     2 - north
		 *     3 - east
		 *     4 - south
		 *     5 - west*/
		// iterate over all placeholders, update those that are next to the given coordinates
		for (SegmentPlaceholder ph : placeholderList) {
			if(ph.getX() == chunkX + 1 
					&& ph.getZ() == chunkZ
					&& ph.getY() == y) {
				addSide[3] = false;
				ph.setInterface(5, segment.getInterface(3));
				continue;
			} else if(ph.getX() == chunkX - 1
					&& ph.getZ() == chunkZ
					&& ph.getY() == y) {
				addSide[5] = false;
				ph.setInterface(3, segment.getInterface(5));
				continue;
			} else if(ph.getX() == chunkX
					&& ph.getZ() == chunkZ + 1
					&& ph.getY() == y) {
				addSide[4] = false;
				ph.setInterface(2, segment.getInterface(4));
				continue;
			} else if(ph.getX() == chunkX
					&& ph.getZ() == chunkZ - 1
					&& ph.getY() == y) {
				addSide[2] = false;
				ph.setInterface(4, segment.getInterface(2));
				continue;
			} else if(ph.getX() == chunkX
					&& ph.getZ() == chunkZ
					&& ph.getY() == y + 16) {
				addSide[0] = false;
				ph.setInterface(1, segment.getInterface(0));
				continue;
			} else if(ph.getX() == chunkX
					&& ph.getZ() == chunkZ
					&& ph.getY() == y - 16) {
				addSide[1] = false;
				ph.setInterface(0, segment.getInterface(1));
				continue;
			} else if(ph.getX() == chunkX
					&& ph.getZ() == chunkZ
					&& ph.getY() == y) {
				exists = true;
				ph.setOccupied(true);
				continue;
			} 
		}
		
		/*for (int i = 0; i < addSide.length; i++) {
			if(addSide[i]) {
				MLogger.logf("add side: %d", i);
			}
		}*/
		
		/*	   0 - top		+y
		 *     1 - bottom	-y
		 *     2 - north	-z
		 *     3 - east 	+x
		 *     4 - south	+z
		 *     5 - west 	-x
		 */
		
		// add placeholders to list for unoccupied, interfaced sides
		if(addSide[3]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX + 1, y, chunkZ);
			ph.setInterface(5, segment.getInterface(3));
			placeholderList.add(ph);
		}
		if(addSide[5]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX - 1, y, chunkZ);
			ph.setInterface(3, segment.getInterface(5));
			placeholderList.add(ph);
		}
		if(addSide[2]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX, y, chunkZ - 1);
			ph.setInterface(4, segment.getInterface(2));
			placeholderList.add(ph);
		}
		if(addSide[4]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX, y, chunkZ + 1);
			ph.setInterface(2, segment.getInterface(4));
			placeholderList.add(ph);
		}
		if(addSide[0]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX, y + 16, chunkZ);
			ph.setInterface(1, segment.getInterface(0));
			placeholderList.add(ph);
		}
		if(addSide[1]) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX, y - 16, chunkZ);
			ph.setInterface(0, segment.getInterface(1));
			placeholderList.add(ph);
		}
		if(!exists) {
			SegmentPlaceholder ph = new SegmentPlaceholder(chunkX, y, chunkZ);
			for (int i = 0; i < 6; i++) {
				ph.setInterface(i, segment.getInterface(i));
			}
			ph.setOccupied(true);
			placeholderList.add(ph);
		}
		
	}
	
	public void generateGen(int chunkX, int chunkZ, World world, Gen gen, Random random) {
		int startY = 0;
		int count = 0;
		Segment startingSegment = null;
		List<SegmentPlaceholder> placeholderList = new ArrayList<SegmentPlaceholder>();
		
		// calculate generation height
		switch(gen.getLevel()) {
			case Gen.UNDERGROUND_LEVEL:
				startY = 4 + random.nextInt(3)*16;
				break;
			
			case Gen.SURFACE_LEVEL:
				Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
				int average = 0;
				for (int i = 0; i < 16; i++) {
					for (int j = 0; j < 16; j++) {
						average += chunk.getHeight(i, j);
					}
				}
				
				// we generate surface gens 3 levels below, it's already one below based on world.getHeightValue
				startY = average / 256 - 2;
				break;
				
			case Gen.SEA_FLOOR_LEVEL:
				for (startY = 255; startY > 0; startY--) {
					if(world.getBlockState(new BlockPos(chunkX*16, startY, chunkZ*16)).getBlock().getMaterial().isSolid()) {
						startY-= 3;
						break;
					}
				}
				break;
			
			default:
				MLogger.log("Attempt to generate gen with invalid level.");
				return;
		}
		
		// get starting segment
		startingSegment = gen.getStartingSegment(random);
		

		if(startingSegment != null) {
			
			// add placeholder for starting segment to placeholder list
			createPlaceholders(chunkX, chunkZ, startY, startingSegment, placeholderList);
			
			// generate starting segment
			generateSegment(chunkX, chunkZ, startY, startingSegment, world, false, random);
			
			//MLogger.log("   [+y,-y,-z,+x,+z,-x]");
			
			/*for (SegmentPlaceholder ph : placeholderList) {
				MLogger.log(ph);
			}*/
		} else {
			MLogger.logf("Failed to find starting segment when generating gen: %s", gen.getName());
		}
		
		
		// while there are placeholders
		while(hasUsablePlaceholders(placeholderList) && count < Constants.SEGMENT_LIMIT) {
			
			// get first placeholder
			SegmentPlaceholder ph = getFirstUsablePlaceholder(placeholderList);
			
			// get segment matching placeholder
			Segment segment = gen.getMatchingSegment(ph.getInterfaces(), random);
			
			if(segment != null) {
				// generate segment
				generateSegment(ph.getX(), ph.getZ(), ph.getY(), segment, world, false, random);
				
				// update placeholder list based on segment
				createPlaceholders(ph.getX(), ph.getZ(), ph.getY(), segment, placeholderList);
			} else {
				MLogger.logf("Failed to find matching segment when generating gen: %s", gen.getName());
				break;
			}
			
			count++;
		}
	}
	
	private boolean hasUsablePlaceholders(List<SegmentPlaceholder> placeholderList) {
		for (SegmentPlaceholder placeholder : placeholderList) {
			if(!placeholder.isOccupied() && placeholder.hasProperInterfaces()) {
				return true;
			}
		}
		return false;
	}
	
	private SegmentPlaceholder getFirstUsablePlaceholder(List<SegmentPlaceholder> placeholderList) {
		for (SegmentPlaceholder placeholder : placeholderList) {
			if(!placeholder.isOccupied() && placeholder.hasProperInterfaces()) {
				return placeholder;
			}
		}
		return null;
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		
		int level = -1;
		
		if(random.nextInt(Constants.DUNGEON_CHANCE_SURFACE) == 0) {
			generateAtLevel(random, chunkX, chunkZ, world, Gen.SURFACE_LEVEL);
		}
		
		if(random.nextInt(Constants.DUNGEON_CHANCE_SEA) == 0) {
			generateAtLevel(random, chunkX, chunkZ, world, Gen.SEA_FLOOR_LEVEL);
		}
		
		if(random.nextInt(Constants.DUNGEON_CHANCE_UNDERGROUND) == 0) {
			generateAtLevel(random, chunkX, chunkZ, world, Gen.UNDERGROUND_LEVEL);
		}
	}

	private void generateAtLevel(Random random, int chunkX, int chunkZ,
			World world, int level) {
		List<Gen> matchingGens = new ArrayList<Gen>();
		GenManager genManager = GenManager.getInstance();
		BiomeGenBase biome = world.getBiomeGenForCoords(new BlockPos(chunkX*16, 0, chunkZ*16));
		Type[] types = BiomeDictionary.getTypesForBiome(biome);
		
		for (int i = 0; i < genManager.getNumGens(); i++) {
			Gen gen = genManager.getGenByIndex(i);
			for (int j = 0; j < types.length; j++) {
				if(gen.generatesInBiome(types[j]) && gen.getLevel() == level) {
					matchingGens.add(gen);

					/* add the gen additional times based on its weight */
					for (int k = 0; k < gen.getWeight(); k++) {
						matchingGens.add(gen);
					}

					break;
				}
			}
		}
		
		if(matchingGens.size() > 0) {
			generateGen(chunkX, chunkZ, world, matchingGens.get(random.nextInt(matchingGens.size())), random);
		}
	}
}
