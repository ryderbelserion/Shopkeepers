package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.BlockFaceUtils;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	protected final SignShops signShops;
	private SignType signType = SignType.OAK; // Not null, not unsupported, default is OAK.
	private boolean wallSign = true;
	private BlockFace signFacing = BlockFace.SOUTH; // Not null
	// Update the sign content at least once after plugin start, in case some settings have changed which affect the
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
				this.signFacing = BlockFaceUtils.getSignPostFacing(creationData.getSpawnLocation().getYaw());
			} else if (BlockFaceUtils.isWallSignFace(targetedBlockFace)) {
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
		// Sign (wood) type:
		String signTypeName = configSection.getString("signType");
		// Migration from TreeSpecies to SignType:
		// TODO Remove this again at some point.
		if ("GENERIC".equals(signTypeName)) {
			Log.warning("Migrating sign type of shopkeeper '" + shopkeeper.getId() + "' from '" + signTypeName
					+ "' to '" + SignType.OAK + "'.");
			signTypeName = SignType.OAK.name();
			shopkeeper.markDirty();
		} else if ("REDWOOD".equals(signTypeName)) {
			Log.warning("Migrating sign type of shopkeeper '" + shopkeeper.getId() + "' from '" + signTypeName
					+ "' to '" + SignType.SPRUCE + "'.");
			signTypeName = SignType.SPRUCE.name();
			shopkeeper.markDirty();
		}

		try {
			signType = SignType.valueOf(signTypeName);
			assert signType != null;
			// Ensure that the loaded sign type is supported:
			if (!signType.isSupported()) {
				throw new RuntimeException("unsupported sign type");
			}
		} catch (Exception e) {
			// Fallback to default:
			Log.warning("Missing, invalid, or unsupported sign type '" + signTypeName + "' for shopkeeper "
					+ shopkeeper.getId() + "'. Using '" + SignType.OAK + "' now.");
			signType = SignType.OAK;
			shopkeeper.markDirty();
		}

		// Wall sign vs sign post:
		if (!configSection.isBoolean("wallSign")) {
			// Missing value:
			shopkeeper.markDirty();
		}
		wallSign = configSection.getBoolean("wallSign", true);

		// Sign facing:
		signFacing = BlockFace.SOUTH; // Default
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
			if (wallSign ? !BlockFaceUtils.isWallSignFace(signFacing) : !BlockFaceUtils.isSignPostFacing(signFacing)) {
				Log.warning("Invalid sign facing for shopkeeper " + shopkeeper.getId() + ": " + signFacingName);
				signFacing = BlockFace.SOUTH; // Fallback to default
				shopkeeper.markDirty();
			}
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);

		// Sign type:
		configSection.set("signType", signType.name());

		// Wall sign vs sign post:
		configSection.set("wallSign", wallSign);

		// Sign facing:
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
		if (signLocation == null) return null; // World not loaded
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) return null; // Chunk not loaded
		Block signBlock = signLocation.getBlock();
		if (!ItemUtils.isSign(signBlock.getType())) return null; // Not a sign
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
	public boolean needsSpawning() {
		return true; // Despawn signs on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean spawn() {
		Location signLocation = this.getLocation();
		if (signLocation == null) {
			return false;
		}

		// If re-spawning fails due to the sign dropping for some reason (ex. attached block missing) this could be
		// abused (sign drop farming), therefore we limit the number of spawn attempts:
		if (System.currentTimeMillis() - lastFailedRespawnAttempt < 3 * 60 * 1000L) {
			Log.debug(() -> "Shopkeeper sign at " + shopkeeper.getPositionString() + " is on spawn cooldown.");
			return false;
		}

		// Place sign:
		Block signBlock = signLocation.getBlock();
		BlockData signData = this.createBlockData();
		assert signData != null;

		// Cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(signData, false); // Skip physics update
		// Cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// In case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			lastFailedRespawnAttempt = System.currentTimeMillis();
			return false;
		}

		// Init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	private BlockData createBlockData() {
		Material signMaterial = getSignMaterial(signType, wallSign);
		assert ItemUtils.isSign(signMaterial);
		BlockData signData = null;
		if (wallSign) {
			// Wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(signMaterial);
			wallSignData.setFacing(signFacing);
			signData = wallSignData;
		} else {
			// Sign post:
			org.bukkit.block.data.type.Sign signPostData = (org.bukkit.block.data.type.Sign) Bukkit.createBlockData(signMaterial);
			signPostData.setRotation(signFacing);
			signData = signPostData;
		}
		return signData;
	}

	private static Material getSignMaterial(SignType signType, boolean wallSign) {
		assert signType != null && signType.isSupported();
		if (wallSign) return signType.getWallSignMaterial();
		else return signType.getSignMaterial();
	}

	@Override
	public void despawn() {
		Block signBlock = this.getBlock();
		if (signBlock != null) {
			assert ItemUtils.isSign(signBlock.getType());
			// Remove sign:
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
			updateSign = true; // Request update, once the sign is available again
			return;
		}

		// Line 0: Header
		sign.setLine(0, Settings.signShopFirstLine);

		// Line 1: Shop name
		String name = shopkeeper.getName(); // Can be empty
		name = this.prepareName(name);
		String line1 = name;
		sign.setLine(1, line1);

		// Line 2: Owner name
		String line2 = "";
		if (shopkeeper instanceof PlayerShopkeeper) {
			line2 = ((PlayerShopkeeper) shopkeeper).getOwnerName();
		}
		sign.setLine(2, line2);

		// Line 3: Empty
		sign.setLine(3, "");

		// Apply sign changes:
		sign.update(false, false);
	}

	@Override
	public boolean check() {
		if (!shopkeeper.getChunkCoords().isChunkLoaded()) {
			// Only verify sign, if the chunk is currently loaded:
			return false;
		}

		Block signBlock = this.getBlock();
		if (signBlock == null) {
			Log.debug(() -> "Shopkeeper sign at " + shopkeeper.getPositionString() + " is no longer existing! Attempting respawn now.");
			if (!this.spawn()) {
				Log.warning("Shopkeeper sign at " + shopkeeper.getPositionString() + " could not be spawned!");
			}
			return true;
		}

		// Update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}

		return false;
	}

	// NAMING

	@Override
	public void setName(String name) {
		// The name gets set during sign update, which always uses the name of the shopkeeper:
		// TODO Allow changing only the name? Currently this restriction allows to not have to store
		// custom names inside this sign shop object.
		this.updateSign();
	}

	@Override
	public String getName() {
		Sign sign = this.getSign();
		if (sign == null) return null;
		return sign.getLine(1);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<EditorHandler.Button>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSignTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleSignType(backwards);
				return true;
			}
		});
		return editorButtons;
	}

	// SIGN TYPE

	public void setSignType(SignType signType) {
		Validate.notNull(signType, "signType is null");
		Validate.isTrue(signType.isSupported(), "signType is not supported");
		this.signType = signType;
		shopkeeper.markDirty();
		this.applySignType();
	}

	protected void applySignType() {
		Sign sign = this.getSign();
		if (sign != null) {
			BlockData signData = this.createBlockData();
			sign.setBlockData(signData); // Keeps sign data (text) the same
			sign.update(true, false); // Force: Material has changed, skip physics update.
		}
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(EnumUtils.cycleEnumConstant(SignType.class, signType, backwards, SignType.IS_SUPPORTED));
	}

	protected ItemStack getSignTypeEditorItem() {
		ItemStack iconItem = new ItemStack(signType.getSignMaterial());
		return ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonSignVariant, Settings.msgButtonSignVariantLore);
	}
}
