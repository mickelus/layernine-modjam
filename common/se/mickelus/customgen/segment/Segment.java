package se.mickelus.customgen.segment;

import java.util.ArrayList;
import java.util.List;

import se.mickelus.customgen.Constants;
import se.mickelus.customgen.MLogger;
import se.mickelus.customgen.newstuff.Gen;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary.Type;

public class Segment {
	
	public static final String NAME_KEY = "name";
	public static final String BLOCKS_KEY = "blocks";
	public static final String DATA_KEY = "data";
	public static final String TILE_ENTITY_KEY = "tileentity";
	public static final String ENTITY_KEY = "entity";
	public static final String INTERFACE_KEY = "interfaces";
	
	private String name;
	
	private int[] blocks;
	private int[] data;
	
	private ArrayList<NBTTagCompound> tileEntityNBTList;
	private ArrayList<NBTTagCompound> entityNBTList;
	
	private int[] interfaces;
	
	public Segment(String name) {
		
		blocks = new int[4096];
		data = new int[4096];
		
		tileEntityNBTList = new ArrayList<NBTTagCompound>();
		entityNBTList = new ArrayList<NBTTagCompound>();
		
		interfaces = new int[6];
		
		this.name = name;
				
	}
	
	public void setBlock(int x, int y, int z, int blockID, int blockData) {
		if(blockID == Constants.EMPTY_ID) {
			blockID = -1;
		} else if (blockID == Constants.INTERFACEBLOCK_ID) {
			blockID = -2;
		}
		blocks[x+z*16+y*256] = blockID;
		data[x+z*16+y*256] = blockData;
	}
	
	/**
	 * @param side The side of the segment. values and sides map like this:
	 *     0 - top
	 *     1 - bottom
	 *     2 - north
	 *     3 - east
	 *     4 - south
	 *     5 - west
	 * @return The interface value for the given side
	 */
	public int getInterface(int side) {
		return interfaces[side];
	}

	/**
	 * 
	 * @param side The side of the segment. values and sides map like this:
	 *     0 - top
	 *     1 - bottom
	 *     2 - north
	 *     3 - east
	 *     4 - south
	 *     5 - west
	 * @param value The value of this interface, should be >= 0
	 */
	public void setInterface(int side, int value) {
		interfaces[side] = value;
	}

	
	public int getBlockID(int x, int y, int z) {
		return blocks[x+z*16+y*256];
	}
	
	public int getBlockData(int x, int y, int z) {
		return data[x+z*16+y*256];
	}
	
	public int getNumTileEntities() {
		return tileEntityNBTList.size();
	}
	
	public NBTTagCompound getTileEntityNBT(int index) {
		return (NBTTagCompound)tileEntityNBTList.get(index).copy();
	}
	
	public void setTileEntityNBTs(NBTTagCompound[] tags) {
		tileEntityNBTList.clear();
		tileEntityNBTList.ensureCapacity(tags.length);
		for (NBTTagCompound nbtTagCompound : tags) {
			tileEntityNBTList.add(nbtTagCompound);
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public NBTTagCompound writeToNBT(boolean writeBlocks) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagList tileEntityTagList = new NBTTagList();
		NBTTagList entityTagList = new NBTTagList();
		
		// write name
		nbt.setString(NAME_KEY, name);
		
		// write interfaces
		nbt.setIntArray(INTERFACE_KEY, interfaces);

		// write block IDs and meta
		if(writeBlocks) {
			nbt.setIntArray(BLOCKS_KEY, blocks);
			nbt.setIntArray(DATA_KEY, data);
		}
		
		// write tile entities
		for (NBTTagCompound nbtTagCompound : tileEntityNBTList) {
			tileEntityTagList.appendTag(nbtTagCompound);
			if(nbtTagCompound.hasKey("Items")) {
				NBTTagList list = nbtTagCompound.getTagList("Items");
				for (int i = 0; i < list.tagCount(); i++) {
					NBTTagCompound item = (NBTTagCompound)list.tagAt(i);
					if(item.getShort("id") == Constants.PLACEHOLDERITEM_ID) {
						item.setShort("id", (short)-1);
					}
				}
			}
		}
		nbt.setTag(TILE_ENTITY_KEY, tileEntityTagList);
		
		// write entities
		for (NBTTagCompound nbtTagCompound : entityNBTList) {
			entityTagList.appendTag(nbtTagCompound);
		}
		nbt.setTag(ENTITY_KEY, entityTagList);
		
		return nbt;
	}
	
	public static Segment readFromNBT(NBTTagCompound nbt) {
		Segment segment = new Segment(nbt.getString("name"));
		
		NBTTagList tileEntityList = nbt.getTagList(TILE_ENTITY_KEY);
		NBTTagList entityList = nbt.getTagList(ENTITY_KEY);
		NBTTagCompound[] tileEntityArray = new NBTTagCompound[tileEntityList.tagCount()];
		NBTTagCompound[] entityArray = new NBTTagCompound[entityList.tagCount()];
		
		int[] interfaces = nbt.getIntArray(INTERFACE_KEY);
		
		int[] blocks = nbt.getIntArray(BLOCKS_KEY);
		int[] data = nbt.getIntArray(DATA_KEY);
		
		if(interfaces.length == 6) {
			for (int i = 0; i < interfaces.length; i++) {
				segment.setInterface(i, interfaces[i]);
			}
		} else {
			MLogger.logf("Failed to read interfaces for %s, %d", segment.getName(), interfaces.length);
		}
		
		if(blocks.length == 4096 && data.length == 4096) {
			//MLogger.log("reading blocks and data");
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						segment.setBlock(x, y, z, blocks[x+z*16+y*256], data[x+z*16+y*256]);
					}
				}
			}
		}
		
		for (int i = 0; i < tileEntityList.tagCount(); i++) {
			tileEntityArray[i] = (NBTTagCompound)tileEntityList.tagAt(i);
			if(tileEntityArray[i].hasKey("Items")) {
				NBTTagList list = tileEntityArray[i].getTagList("Items");
				for (int j = 0; j < list.tagCount(); j++) {
					NBTTagCompound item = (NBTTagCompound)list.tagAt(j);
					if(item.getShort("id") == -1) {
						item.setShort("id", (short)Constants.PLACEHOLDERITEM_ID);
					}
				}
			}
		}
		segment.setTileEntityNBTs(tileEntityArray);
		
		for (int i = 0; i < entityList.tagCount(); i++) {
			entityArray[i] = (NBTTagCompound)entityList.tagAt(i);
		}
		
		/* else {
			MLogger.logf("Failed to read blocks and data from segment %s, invalid lengths %d %d",
					segment.getName(), blocks.length, data.length);
		}*/
		

		return segment;
	}
	
	public void parseFromWorld(World world, int chunkX, int chunkY, int chunkZ) {
		int[] interfaces = new int[6];
		
		int xOffset = chunkX*16;
		int yOffset = chunkY;
		int zOffset = chunkZ*16;
		
		ArrayList<NBTTagCompound> tileEntityNBTList = new ArrayList<NBTTagCompound>();
		
		
		// set blocks
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					setBlock(x, y, z,
							world.getBlockId(xOffset+x, yOffset+y, zOffset+z),
							world.getBlockMetadata(xOffset+x, yOffset+y, zOffset+z));
				}
			}
		}
		
		// set interfaces
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				// top
				if(world.getBlockId(xOffset+i, yOffset+15, zOffset+j) == Constants.INTERFACEBLOCK_ID) {
					interfaces[0] += 1 + world.getBlockMetadata(xOffset+i, yOffset+15, zOffset+j);
				}
				// bottom
				if(world.getBlockId(xOffset+i, yOffset, zOffset+j) == Constants.INTERFACEBLOCK_ID) {
					interfaces[1] += 1 + world.getBlockMetadata(xOffset+i, yOffset, zOffset+j);
				}
				
				// south
				if(world.getBlockId(xOffset+i, yOffset+j, zOffset+15) == Constants.INTERFACEBLOCK_ID) {
					interfaces[2] += 1 + world.getBlockMetadata(xOffset+i, yOffset+j, zOffset+15);
				}
				// north
				if(world.getBlockId(xOffset+i, yOffset+j, zOffset) == Constants.INTERFACEBLOCK_ID) {
					interfaces[4] += 1 + world.getBlockMetadata(xOffset+i, yOffset+j, zOffset);
				}
				
				// east
				if(world.getBlockId(xOffset+15, yOffset+i, zOffset+j) == Constants.INTERFACEBLOCK_ID) {
					interfaces[3] += 1 + world.getBlockMetadata(xOffset+15, yOffset+i, zOffset+j);
				}
				// west
				if(world.getBlockId(xOffset, yOffset+i, zOffset+j) == Constants.INTERFACEBLOCK_ID) {
					interfaces[5] += 1 + world.getBlockMetadata(xOffset, yOffset+i, zOffset+j);
				}
			}
		}
		
		for (int i = 0; i < interfaces.length; i++) {
			setInterface(i, interfaces[i]);
		}
		
		// set tile entities
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					if(world.blockHasTileEntity(xOffset+x, yOffset+y, zOffset+z)) {
						NBTTagCompound tagCompound = new NBTTagCompound();
				        world.getBlockTileEntity(xOffset+x, yOffset+y, zOffset+z).writeToNBT(tagCompound);
				        tagCompound.setInteger("x", x);
				        tagCompound.setInteger("y", y);
				        tagCompound.setInteger("z", z);
				        tileEntityNBTList.add(tagCompound);
					}
					
				}
			}
		}
		setTileEntityNBTs(tileEntityNBTList.toArray(new NBTTagCompound[tileEntityNBTList.size()]));
		
	}
	
	@Override
	public String toString() {
		String string = "SEGM:";
		string += getName();
		string += "[";
		for (int i = 0; i < interfaces.length; i++) {
			string += String.format("%2d", interfaces[i]);
			if(i != interfaces.length-1) {
				string += ",";
			}
		}
		string += "]";

		return string;
	}

}