package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.sign.SignShopObject;
import com.nisovin.shopkeepers.compat.MC_1_17_Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObject;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKSignShopObject extends AbstractBlockShopObject implements SignShopObject {

	private static final String DATA_KEY_SIGN_TYPE = "signType";
	public static final Property<SignType> SIGN_TYPE = new BasicProperty<SignType>()
			.dataKeyAccessor(DATA_KEY_SIGN_TYPE, EnumSerializers.lenient(SignType.class))
			.validator((property, value) -> {
				Validate.isTrue(value.isSupported(), () -> "Unsupported sign type: '" + value.name() + "'.");
			})
			.defaultValue(SignType.OAK)
			.build();

	public static final Property<Boolean> WALL_SIGN = new BasicProperty<Boolean>()
			.dataKeyAccessor("wallSign", BooleanSerializers.LENIENT)
			.defaultValue(true)
			.build();

	public static final Property<Boolean> GLOWING_TEXT = new BasicProperty<Boolean>()
			.dataKeyAccessor("glowingText", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private static final int CHECK_PERIOD_SECONDS = 10;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_PERIOD_SECONDS + 1);
	private static final long RESPAWN_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(3);

	protected final SignShops signShops;

	private final PropertyValue<SignType> signTypeProperty = new PropertyValue<>(SIGN_TYPE)
			.onValueChanged(this::applySignType)
			.build(properties);
	private final PropertyValue<Boolean> wallSignProperty = new PropertyValue<>(WALL_SIGN)
			.onValueChanged(this::respawn)
			.build(properties);
	private final PropertyValue<Boolean> glowingTextProperty = new PropertyValue<>(GLOWING_TEXT)
			.onValueChanged(this::applyGlowingText)
			.build(properties);

	private Block block = null;
	private long lastFailedRespawnAttemptMillis = 0;

	// Initial threshold between [1, CHECK_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkLimiter = new RateLimiter(CHECK_PERIOD_SECONDS, nextCheckingOffset.getAndIncrement());

	protected SKSignShopObject(SignShops signShops, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.signShops = signShops;
		if (creationData != null) {
			BlockFace targetedBlockFace = creationData.getTargetedBlockFace();
			if (targetedBlockFace == BlockFace.UP) {
				// Sign post:
				wallSignProperty.setValue(false, Collections.emptySet()); // Not marking dirty
			} // Else: Wall sign (default).
		}
	}

	@Override
	public SKSignShopObjectType getType() {
		return signShops.getSignShopObjectType();
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		signTypeProperty.load(shopObjectData);
		wallSignProperty.load(shopObjectData);
		glowingTextProperty.load(shopObjectData);

		this.migrateSignFacingToYaw(shopObjectData);
	}

	private void migrateSignFacingToYaw(DataContainer shopObjectData) {
		assert shopObjectData != null;
		// Migration from sign facing to shopkeeper yaw (pre v2.13.4):
		// TODO Remove this migration again at some point.
		String signFacingName = shopObjectData.getString("signFacing");
		if (signFacingName != null) {
			BlockFace signFacing = BlockFace.SOUTH;
			try {
				signFacing = BlockFace.valueOf(signFacingName);
			} catch (IllegalArgumentException e) {
				Log.warning(shopkeeper.getLogPrefix() + "Could not parse sign facing '"
						+ signFacingName + "'. Falling back to SOUTH.");
			}
			if (!this.isValidSignFacing(signFacing)) {
				Log.warning(shopkeeper.getLogPrefix() + "Invalid sign facing '" + signFacingName
						+ "'. Falling back to SOUTH.");
				signFacing = BlockFace.SOUTH;
			}

			float yaw = BlockFaceUtils.getYaw(signFacing);
			Log.warning(shopkeeper.getLogPrefix() + "Migrating sign facing '" + signFacing
					+ "' to yaw " + TextUtils.DECIMAL_FORMAT.format(yaw));
			shopkeeper.setYaw(yaw); // This also marks the shopkeeper as dirty
		}
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		signTypeProperty.save(shopObjectData);
		wallSignProperty.save(shopObjectData);
		glowingTextProperty.save(shopObjectData);
		// Note: The sign facing is not saved, but instead derived from the shopkeeper's yaw.
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
		if (System.currentTimeMillis() - lastFailedRespawnAttemptMillis < RESPAWN_TIMEOUT_MILLIS) {
			Log.debug(() -> shopkeeper.getLocatedLogPrefix() + "Sign is on spawn cooldown.");
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
			lastFailedRespawnAttemptMillis = System.currentTimeMillis();
			this.cleanUpBlock(signBlock);
			return false;
		}

		// Remember the block (indicates that this shop object has been spawned):
		this.block = signBlock;
		// Assign metadata for easy identification by other plugins:
		ShopkeeperMetadata.apply(block);

		// Setup sign:
		this.updateSign();

		// Inform about the object id change:
		this.onIdChanged();

		return true;
	}

	private BlockData createBlockData() {
		SignType signType = this.getSignType();
		boolean wallSign = this.isWallSign();
		Material signMaterial = getSignMaterial(signType, wallSign);
		assert ItemUtils.isSign(signMaterial);
		BlockData signData = null;
		if (wallSign) {
			// Wall sign:
			WallSign wallSignData = (WallSign) Bukkit.createBlockData(signMaterial);
			wallSignData.setFacing(this.getSignFacing());
			signData = wallSignData;
		} else {
			// Sign post:
			org.bukkit.block.data.type.Sign signPostData = (org.bukkit.block.data.type.Sign) Bukkit.createBlockData(signMaterial);
			signPostData.setRotation(this.getSignFacing());
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

		// Inform about the object id change:
		this.onIdChanged();
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
		if (sign == null) return; // Not spawned or no longer a sign

		// Setup sign contents:
		if (shopkeeper instanceof PlayerShopkeeper) {
			this.setupPlayerShopSign(sign, (PlayerShopkeeper) shopkeeper);
		} else {
			assert shopkeeper instanceof AdminShopkeeper;
			this.setupAdminShopSign(sign, (AdminShopkeeper) shopkeeper);
		}

		// Glowing text:
		NMSManager.getProvider().setGlowingText(sign, this.isGlowingText());

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
			Log.debug(() -> shopkeeper.getLocatedLogPrefix() + "Sign is missing! Attempting respawn.");
			// Cleanup any previously spawned block, and then respawn:
			this.despawn();
			boolean success = this.spawn();
			if (!success) {
				Log.warning(shopkeeper.getLocatedLogPrefix() + "Sign could not be spawned!");
			}
			return;
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
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSignTypeEditorButton());
		if (MC_1_17_Utils.isAvailable()) {
			editorButtons.add(this.getGlowingTextEditorButton());
		}
		return editorButtons;
	}

	// WALL SIGN (vs sign post)

	public boolean isWallSign() {
		return wallSignProperty.getValue();
	}

	// SIGN FACING

	private boolean isValidSignFacing(BlockFace signFacing) {
		if (this.isWallSign()) {
			return BlockFaceUtils.isWallSignFacing(signFacing);
		} else {
			return BlockFaceUtils.isSignPostFacing(signFacing);
		}
	}

	public BlockFace getSignFacing() {
		if (this.isWallSign()) {
			return BlockFaceUtils.getWallSignFacings().fromYaw(shopkeeper.getYaw());
		} else {
			return BlockFaceUtils.getSignPostFacings().fromYaw(shopkeeper.getYaw());
		}
	}

	// SIGN TYPE

	public SignType getSignType() {
		return signTypeProperty.getValue();
	}

	public void setSignType(SignType signType) {
		signTypeProperty.setValue(signType);
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
		this.setSignType(EnumUtils.cycleEnumConstant(SignType.class, this.getSignType(), backwards, SignType.IS_SUPPORTED));
	}

	private ItemStack getSignTypeEditorItem() {
		ItemStack iconItem = new ItemStack(this.getSignType().getSignMaterial());
		return ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSignVariant, Messages.buttonSignVariantLore);
	}

	private Button getSignTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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

	// GLOWING TEXT

	public boolean isGlowingText() {
		return glowingTextProperty.getValue();
	}

	public void setGlowingText(boolean glowing) {
		glowingTextProperty.setValue(glowing);
	}

	protected void applyGlowingText() {
		Sign sign = this.getSign();
		if (sign == null) return; // Not spawned or no longer a sign

		NMSManager.getProvider().setGlowingText(sign, this.isGlowingText());
		// Sign block type is still the same (no force required), and we want to skip physics:
		sign.update(false, false);
	}

	public void cycleGlowingText(boolean backwards) {
		this.setGlowingText(!this.isGlowingText());
	}

	private ItemStack getGlowingTextEditorItem() {
		ItemStack iconItem;
		if (this.isGlowingText()) {
			iconItem = new ItemStack(MC_1_17_Utils.MATERIAL_GLOW_INK_SAC);
		} else {
			iconItem = new ItemStack(Material.INK_SAC);
		}
		return ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSignGlowingText, Messages.buttonSignGlowingTextLore);
	}

	private Button getGlowingTextEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getGlowingTextEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleGlowingText(backwards);
				return true;
			}
		};
	}
}
