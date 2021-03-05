package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.BlockFaceUtils;
import com.nisovin.shopkeepers.util.CyclicCounter;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.RateLimiter;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_PERIOD_SECONDS + 1);

	protected final SignShops signShops;
	private SignType signType = SignType.OAK; // Not null, not unsupported, default is OAK.
	private boolean wallSign = true;
	private BlockFace signFacing = BlockFace.SOUTH; // Not null
	// Update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;
	private Block block = null;
	private long lastFailedRespawnAttempt = 0;

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkLimiter = new RateLimiter(CHECK_PERIOD_SECONDS, nextCheckingOffset.getAndIncrement());

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
		return block;
	}

	public Sign getSign() {
		if (!this.isActive()) return null;
		assert block != null && ItemUtils.isSign(block.getType());
		return (Sign) block.getState();
	}

	@Override
	public boolean isActive() {
		Block block = this.getBlock();
		if (block == null) return false; // Not spawned
		assert shopkeeper.getChunkCoords().isChunkLoaded(); // The shopkeeper is despawned on chunk unload
		if (!ItemUtils.isSign(block.getType())) return false; // No longer a sign
		return true;
	}

	@Override
	public boolean spawn() {
		if (block != null) {
			return true; // Already spawned
		}

		Location signLocation = shopkeeper.getLocation();
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
		// This replaces any currently existing block at that location.
		Block signBlock = signLocation.getBlock();
		BlockData blockData = this.createBlockData();
		assert blockData != null;

		// Cancel block physics for this placed sign if needed:
		signShops.cancelNextBlockPhysics(signBlock);
		signBlock.setBlockData(blockData, false); // Skip physics update
		// Cleanup state if no block physics were triggered:
		signShops.cancelNextBlockPhysics(null);

		// In case sign placement has failed for some reason:
		if (!ItemUtils.isSign(signBlock.getType())) {
			lastFailedRespawnAttempt = System.currentTimeMillis();
			this.cleanUpBlock(signBlock);
			return false;
		}

		// Remember the block (indicates that this shop object has been spawned):
		this.block = signBlock;
		// Assign metadata for easy identification by other plugins:
		ShopkeeperMetadata.apply(block);

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
		if (block == null) return;

		// Cleanup:
		this.cleanUpBlock(block);

		// Remove the sign:
		block.setType(Material.AIR, false);
		this.block = null;
	}

	// Any clean up that needs to happen for the block.
	protected void cleanUpBlock(Block block) {
		assert block != null;
		// Remove the metadata again:
		ShopkeeperMetadata.remove(block);
	}

	@Override
	public Location getLocation() {
		if (block == null) return null;
		return block.getLocation();
	}

	@Override
	public Location getTickVisualizationParticleLocation() {
		Location location = this.getLocation();
		if (location == null) return null;
		if (this.isWallSign()) {
			// Return location at the block center:
			return location.add(0.5D, 0.5D, 0.5D);
		} else {
			// Return location above the sign post:
			return location.add(0.5D, 1.3D, 0.5D);
		}
	}

	private void updateSign() {
		Sign sign = this.getSign();
		if (sign == null) {
			updateSign = true; // Request update, once the sign is available again
			return;
		}

		// Setup sign contents:
		if (shopkeeper instanceof PlayerShopkeeper) {
			this.setupPlayerShopSign(sign, (PlayerShopkeeper) shopkeeper);
		} else {
			assert shopkeeper instanceof AdminShopkeeper;
			this.setupAdminShopSign(sign, (AdminShopkeeper) shopkeeper);
		}

		// Apply sign changes:
		sign.update(false, false);
	}

	protected void setupPlayerShopSign(Sign sign, PlayerShopkeeper playerShop) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("shopName", this.prepareName(playerShop.getName())); // Not null, can be empty
		arguments.put("owner", playerShop.getOwnerName());  // Not null, can be empty

		sign.setLine(0, StringUtils.replaceArguments(Messages.playerSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.playerSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.playerSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.playerSignShopLine4, arguments));
	}

	protected void setupAdminShopSign(Sign sign, AdminShopkeeper adminShop) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("shopName", this.prepareName(adminShop.getName())); // Not null, can be empty

		sign.setLine(0, StringUtils.replaceArguments(Messages.adminSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.adminSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.adminSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.adminSignShopLine4, arguments));
	}

	// TICKING

	@Override
	public void tick() {
		super.tick();
		if (!checkLimiter.request()) {
			return;
		}

		// Indicate ticking activity for visualization:
		this.indicateTickActivity();

		// This is only called for shopkeepers in active (i.e. loaded) chunks, and shopkeepers are despawned on chunk
		// unload:
		assert shopkeeper.getChunkCoords().isChunkLoaded();

		if (!this.isActive()) {
			Log.debug(() -> "Shopkeeper sign at " + shopkeeper.getPositionString() + " is missing! Attempting respawn.");

			// Cleanup any previously spawned block first:
			if (this.isSpawned()) {
				this.despawn();
			}

			boolean success = this.spawn();
			if (!success) {
				Log.warning("Shopkeeper sign at " + shopkeeper.getPositionString() + " could not be spawned!");
			}
			return;
		}

		// Update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}
	}

	// NAMING

	@Override
	public void setName(String name) {
		// Sign blocks don't have a name (the sign contents are language file specific). However, this method is usually
		// called when the shopkeeper is renamed, which may require an update of the sign contents.
		this.updateSign();
	}

	@Override
	public String getName() {
		// Sign blocks don't have a name (the sign contents are language file specific):
		return null;
	}

	// PLAYER SHOP OWNER

	@Override
	public void onShopOwnerChanged() {
		// Update the sign:
		this.updateSign();
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSignTypeEditorButton());
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
		if (sign == null) return; // Not spawned or no longer a sign

		// Note: The different sign types are different materials. We need to capture the sign state (eg. sign
		// contents), because they would otherwise be removed when changing the block's type.
		BlockData blockData = this.createBlockData();
		sign.setBlockData(blockData); // Keeps sign data (eg. text) the same
		sign.update(true, false); // Force: Material has changed, skip physics update.
	}

	public void cycleSignType(boolean backwards) {
		this.setSignType(EnumUtils.cycleEnumConstant(SignType.class, signType, backwards, SignType.IS_SUPPORTED));
	}

	private ItemStack getSignTypeEditorItem() {
		ItemStack iconItem = new ItemStack(signType.getSignMaterial());
		return ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSignVariant, Messages.buttonSignVariantLore);
	}

	private EditorHandler.Button getSignTypeEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
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
		};
	}
}
