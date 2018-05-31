package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SKItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public class SignShop extends ShopObject {

	public static String getId(Block block) {
		if (block == null) return null;
		return "block" + Utils.getLocationString(block);
	}

	private BlockFace signFacing; // can be null, if not yet loaded or unknown

	// update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;

	protected SignShop(Shopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		if (creationData != null) {
			this.signFacing = creationData.getTargetedBlockFace();
		}
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		if (config.isString("signFacing")) {
			String signFacingName = config.getString("signFacing");
			if (signFacingName != null) {
				try {
					signFacing = BlockFace.valueOf(signFacingName);
				} catch (IllegalArgumentException e) {
				}
			}
		}

		// in case no sign facing is stored: try getting the current sign facing from sign in the world
		// if it is not possible (for ex. because the world isn't loaded yet), we will re-attempt this
		// during the periodic checks
		if (signFacing == null) {
			signFacing = this.getSignFacingFromWorld();
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		if (signFacing != null) {
			config.set("signFacing", signFacing.name());
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.spawn();
	}

	@Override
	public SignShopObjectType getObjectType() {
		return SKDefaultShopObjectTypes.SIGN();
	}

	public Sign getSign() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return null;
		Block signBlock = signLocation.getBlock();
		if (!SKItemUtils.isSign(signBlock.getType())) return null;
		return (Sign) signBlock.getState();
	}

	private BlockFace getSignFacingFromWorld() {
		// try getting the current sign facing from the sign in the world:
		Sign sign = this.getSign();
		if (sign != null) {
			return ((Attachable) sign.getData()).getFacing();
		}
		return null;
	}

	@Override
	protected void onChunkLoad() {
		super.onChunkLoad();
		// get the sign facing, in case we weren't able yet, for example because the world wasn't loaded earlier:
		if (signFacing == null) {
			signFacing = this.getSignFacingFromWorld();
		}

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}
	}

	@Override
	public boolean spawn() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return false;

		Block signBlock = signLocation.getBlock();
		if (signBlock.getType() != Material.AIR) {
			return false;
		}

		// place sign: // TODO maybe also allow non-wall signs?
		// cancel block physics for this placed sign if needed:
		SKShopkeepersPlugin.getInstance().cancelNextBlockPhysics(signLocation);
		signBlock.setType(Material.WALL_SIGN);
		// cleanup state if no block physics were triggered:
		SKShopkeepersPlugin.getInstance().cancelNextBlockPhysics(null);

		// in case sign placement has failed for some reason:
		if (!SKItemUtils.isSign(signBlock.getType())) {
			return false;
		}

		// set sign facing:
		if (signFacing != null) {
			Sign signState = (Sign) signBlock.getState();
			((Attachable) signState.getData()).setFacingDirection(signFacing);
			// apply facing:
			signState.update();
		}

		// init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	@Override
	public boolean isActive() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return false;
		Block signBlock = signLocation.getBlock();
		return SKItemUtils.isSign(signBlock.getType());
	}

	@Override
	public String getId() {
		Location location = shopkeeper.getLocation();
		if (location == null) return null;
		return getId(location.getBlock());
	}

	@Override
	public Location getActualLocation() {
		return shopkeeper.getLocation();
	}

	@Override
	public void setName(String name) {
		// always uses the name of the shopkeeper:
		this.updateSign();
	}

	@Override
	public int getNameLengthLimit() {
		return 15;
	}

	@Override
	public void setItem(ItemStack item) {
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
			name = this.trimToNameLength(name);
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
				Bukkit.getScheduler().runTask(SKShopkeepersPlugin.getInstance(), () -> shopkeeper.delete());
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

	@Override
	public void despawn() {
	}

	@Override
	public void delete() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world != null) {
			// this should load the chunk if necessary, making sure that the block gets removed (though, might not work
			// on server stops..):
			Block signBlock = world.getBlockAt(shopkeeper.getX(), shopkeeper.getY(), shopkeeper.getZ());
			if (SKItemUtils.isSign(signBlock.getType())) {
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

	@Override
	public ItemStack getSubTypeItem() {
		return null;
	}

	@Override
	public void cycleSubType() {
	}
}
