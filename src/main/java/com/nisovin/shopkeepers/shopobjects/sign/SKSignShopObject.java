package com.nisovin.shopkeepers.shopobjects.sign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	protected final SignShops signShops;
	private boolean wallSign = true;
	private BlockFace signFacing = BlockFace.SOUTH; // not null
	// update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;
	private long lastFailedRespawnAttempt = 0;

	protected SKSignShopObject(SignShops signShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.signShops = signShops;
		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.UP) {
				this.wallSign = false;
				this.signFacing = Utils.getSignPostFacing(creationData.getSpawnLocation().getYaw());
			} else if (Utils.isWallSignFace(targetedBlockFace)) {
				this.signFacing = targetedBlockFace;
			}
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return signShops.getSignShopObjectType();
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		// wall sign vs sign post:
		if (!configSection.isBoolean("wallSign")) {
			// missing value:
			shopkeeper.markDirty();
		}
		wallSign = configSection.getBoolean("wallSign", true);

		// sign facing:
		signFacing = BlockFace.SOUTH; // default
		String signFacingName = configSection.getString("signFacing");
		if (signFacingName == null) {
			Log.warning("Missing sign facing for shopkeeper " + shopkeeper.getId());
			shopkeeper.markDirty();
		} else {
			try {
				signFacing = BlockFace.valueOf(signFacingName);
			} catch (IllegalArgumentException e) {
				Log.warning("Could not parse sign facing for shopkeeper " + shopkeeper.getId() + ": " + signFacingName);
				shopkeeper.markDirty();
			}
			if (wallSign ? !Utils.isWallSignFace(signFacing) : !Utils.isSignPostFacing(signFacing)) {
				Log.warning("Invalid sign facing for shopkeeper " + shopkeeper.getId() + ": " + signFacingName);
				signFacing = BlockFace.SOUTH; // fallback to default
				shopkeeper.markDirty();
			}
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);

		// wall sign vs sign post:
		configSection.set("wallSign", wallSign);

		// sign facing:
		configSection.set("signFacing", signFacing.name());
	}

	public boolean isWallSign() {
		return wallSign;
	}

	public BlockFace getSignFacing() {
		return signFacing;
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
		if (signLocation == null) {
			return false;
		}

		// if re-spawning fails due to the sign dropping for some reason (ex. attached block missing) this could be
		// abused (sign drop farming), therefore we limit the number of spawn attempts:
		if (System.currentTimeMillis() - lastFailedRespawnAttempt < 3 * 60 * 1000L) {
			Log.debug("Shopkeeper sign at " + shopkeeper.getPositionString() + " is on spawn cooldown.");
			return false;
		}

		// place sign: // TODO maybe also allow non-wall signs?
		Block signBlock = signLocation.getBlock();
		BlockData signData = null;
		if (wallSign) {
			// place wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(Material.WALL_SIGN);
			wallSignData.setFacing(signFacing);
			signData = wallSignData;
		} else {
			// place sign post:
			org.bukkit.block.data.type.Sign signPostData = (org.bukkit.block.data.type.Sign) Bukkit.createBlockData(Material.SIGN);
			signPostData.setRotation(signFacing);
			signData = signPostData;
		}
		assert signData != null;

		// cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(signData, false);
		// cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// in case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			lastFailedRespawnAttempt = System.currentTimeMillis();
			return false;
		}

		// init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	@Override
	public void despawn() {
		Block signBlock = this.getBlock();
		if (signBlock != null) {
			assert ItemUtils.isSign(signBlock.getType());
			// remove sign:
			signBlock.setType(Material.AIR, false);
		}
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
		sign.update(false, false);
	}

	@Override
	public boolean check() {
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) {
			// only verify sign, if the chunk is currently loaded:
			return false;
		}

		Block signBlock = this.getBlock();
		if (signBlock == null) {
			Log.debug("Shopkeeper sign at " + shopkeeper.getPositionString() + " is no longer existing! Attempting respawn now.");
			if (!this.spawn()) {
				Log.warning("Shopkeeper sign at " + shopkeeper.getPositionString() + " could not be spawned!");
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
	public void setName(String name) {
		// the name gets set during sign update, which always uses the name of the shopkeeper:
		// TODO allow changing only the name? Currently this restriction allows to not have to store
		// custom names inside this sign shop object
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
