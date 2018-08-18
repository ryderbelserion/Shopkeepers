package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	protected final SignShops signShops;
	private BlockFace signFacing; // can be null, if not yet loaded or unknown
	// update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;

	protected SKSignShopObject(SignShops signShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.signShops = signShops;
		if (creationData != null) {
			this.signFacing = creationData.getTargetedBlockFace();
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return signShops.getSignShopObjectType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		if (configSection.isString("signFacing")) {
			String signFacingName = configSection.getString("signFacing");
			if (signFacingName != null) {
				try {
					signFacing = BlockFace.valueOf(signFacingName);
				} catch (IllegalArgumentException e) {
				}
			}
		}

		// in case no sign facing is stored: try getting the current sign facing from the sign in the world
		// if this is not possible (for ex. because the world isn't loaded yet), we will re-attempt this
		// during the periodic checks
		this.updateSignFacingFromWorld();
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		if (signFacing != null) {
			configSection.set("signFacing", signFacing.name());
		}
	}

	@Override
	public void setup() {
		super.setup();
		this.spawn();
	}

	// LIFE CYCLE

	@Override
	public void delete() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world != null) {
			// this should load the chunk if necessary, making sure that the block gets removed (though, might not work
			// on server stops..):
			Block signBlock = world.getBlockAt(shopkeeper.getX(), shopkeeper.getY(), shopkeeper.getZ());
			if (ItemUtils.isSign(signBlock.getType())) {
				// remove sign:
				signBlock.setType(Material.AIR);
			}
			// TODO trigger an unloadChunkRequest if the chunk had to be loaded? (for now let's assume that the server
			// handles that kind of thing automatically)
		} else {
			// well: world unloaded and we didn't get an event.. not our fault
			// TODO actually, we are not removing the sign on world unloads..
		}
	}

	// ACTIVATION

	@Override
	public Block getBlock() {
		Location signLocation = this.getLocation();
		if (signLocation == null) return null; // world not loaded
		Block signBlock = signLocation.getBlock();
		if (!ItemUtils.isSign(signBlock.getType())) return null;
		return signBlock;
	}

	public Sign getSign() {
		Block signBlock = this.getBlock();
		if (signBlock == null) return null;
		assert ItemUtils.isSign(signBlock.getType());
		return (Sign) signBlock.getState();
	}

	private BlockFace getSignFacingFromWorld() {
		// try getting the current sign facing from the sign in the world:
		Block signBlock = this.getBlock();
		if (signBlock != null) {
			if (signBlock.getType() == Material.WALL_SIGN) {
				return ((WallSign) signBlock.getBlockData()).getFacing();
			} else if (signBlock.getType() == Material.SIGN) {
				return ((org.bukkit.block.data.type.Sign) signBlock.getBlockData()).getRotation();
			}
		}
		return null;
	}

	private void updateSignFacingFromWorld() {
		if (signFacing == null) {
			signFacing = this.getSignFacingFromWorld();
			if (signFacing != null) {
				shopkeeper.markDirty();
			}
		}
	}

	@Override
	public void onChunkLoad() {
		super.onChunkLoad();
		// get the sign facing, in case we weren't able yet, for example because the world wasn't loaded earlier:
		this.updateSignFacingFromWorld();

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}
	}

	@Override
	public boolean isActive() {
		return (this.getBlock() != null);
	}

	@Override
	public String getId() {
		return this.getType().createObjectId(this.getBlock());
	}

	@Override
	public boolean spawn() {
		Location signLocation = this.getLocation();
		if (signLocation == null) return false;

		Block signBlock = signLocation.getBlock();
		if (signBlock.getType() != Material.AIR) {
			return false;
		}

		// place sign: // TODO maybe also allow non-wall signs?
		WallSign wallSignData = (WallSign) Bukkit.createBlockData(Material.WALL_SIGN);
		if (signFacing != null) {
			// set sign facing:
			wallSignData.setFacing(signFacing);
		}
		// cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(wallSignData, false);
		// cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// in case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			return false;
		}

		// init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	@Override
	public void despawn() {
	}

	@Override
	public Location getLocation() {
		return shopkeeper.getLocation();
	}

	public void updateSign() {
		Sign sign = this.getSign();
		if (sign == null) {
			updateSign = true; // request update, once the sign is available again
			return;
		}

		// line 0: header
		sign.setLine(0, Settings.signShopFirstLine);

		// line 1: shop name
		String name = shopkeeper.getName();
		String line1 = "";
		if (name != null) {
			name = this.prepareName(name);
			line1 = name;
		}
		sign.setLine(1, line1);

		// line 2: owner name
		String line2 = "";
		if (shopkeeper instanceof PlayerShopkeeper) {
			line2 = ((PlayerShopkeeper) shopkeeper).getOwnerName();
		}
		sign.setLine(2, line2);

		// line 3: empty
		sign.setLine(3, "");

		// apply sign changes:
		sign.update();
	}

	@Override
	public boolean check() {
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) {
			// only verify sign, if the chunk is currently loaded:
			return false;
		}

		Sign sign = this.getSign();
		if (sign == null) {
			// removing the shopkeeper, because re-spawning might fail (ex. attached block missing) or could be abused
			// (sign drop farming):
			Log.debug("Shopkeeper sign at " + shopkeeper.getPositionString() + " is no longer existing! Attempting respawn now.");
			if (!this.spawn()) {
				Log.warning("Shopkeeper sign at " + shopkeeper.getPositionString() + " could not be replaced! Removing shopkeeper now!");
				// delayed removal:
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> shopkeeper.delete());
			}
			return true;
		}

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}

		return false;
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		// TODO this is outdated
		return 15;
	}

	@Override
	public void setName(String name) {
		// always uses the name of the shopkeeper:
		// TODO really? why?
		this.updateSign();
	}

	@Override
	public String getName() {
		Sign sign = this.getSign();
		if (sign == null) return null;
		return sign.getLine(1);
	}

	// SUB TYPES
	// not supported

	// OTHER PROPERTIES
	// not supported
}
