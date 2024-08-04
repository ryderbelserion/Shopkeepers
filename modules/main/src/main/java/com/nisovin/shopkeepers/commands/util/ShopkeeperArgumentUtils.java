package com.nisovin.shopkeepers.commands.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.util.ObjectMatcher;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Command utility functions related to shopkeepers.
 */
public final class ShopkeeperArgumentUtils {

	private static final int SHOPKEEPER_TARGET_RANGE = 10;

	public static final class TargetShopkeepersResult {

		private final List<? extends Shopkeeper> shopkeepers;
		private final @Nullable Text errorMessage;
		// assert: !shopkeepers.isEmpty || errorMessage != null

		private TargetShopkeepersResult(List<? extends Shopkeeper> shopkeepers) {
			Validate.notNull(shopkeepers, "shopkeepers is null");
			Validate.isTrue(!shopkeepers.isEmpty(), "shopkeepers is empty");
			Validate.noNullElements(shopkeepers, "shopkeepers contains null");
			this.shopkeepers = shopkeepers;
			this.errorMessage = null;
		}

		private TargetShopkeepersResult(Text errorMessage) {
			Validate.notNull(errorMessage, "errorMessage is null");
			Validate.isTrue(!errorMessage.isPlainTextEmpty());
			this.errorMessage = errorMessage;
			this.shopkeepers = Collections.emptyList();
		}

		public boolean isSuccess() {
			return (errorMessage == null);
		}

		public List<? extends Shopkeeper> getShopkeepers() {
			return shopkeepers;
		}

		public @Nullable Text getErrorMessage() {
			return errorMessage;
		}
	}

	public interface TargetShopkeeperFilter extends Predicate<Shopkeeper> {

		public static final TargetShopkeeperFilter ANY = new TargetShopkeeperFilter() {
			@Override
			public boolean test(@Nullable Shopkeeper shopkeeper) {
				return true;
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Messages.mustTargetShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Text.EMPTY; // Not used
			}
		};

		public static final TargetShopkeeperFilter ADMIN = new TargetShopkeeperFilter() {
			@Override
			public boolean test(@Nullable Shopkeeper shopkeeper) {
				return (shopkeeper instanceof AdminShopkeeper);
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Messages.mustTargetAdminShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Messages.targetShopIsNoAdminShop;
			}
		};

		public static final TargetShopkeeperFilter PLAYER = new TargetShopkeeperFilter() {
			@Override
			public boolean test(@Nullable Shopkeeper shopkeeper) {
				return (shopkeeper instanceof PlayerShopkeeper);
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Messages.mustTargetPlayerShop;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper) {
				return Messages.targetShopIsNoPlayerShop;
			}
		};

		public abstract Text getNoTargetErrorMsg();

		public abstract Text getInvalidTargetErrorMsg(Shopkeeper shopkeeper);
	}

	// If the filter is null, any shopkeeper type can be returned.
	public static TargetShopkeepersResult findTargetedShopkeepers(
			Player player,
			TargetShopkeeperFilter filter
	) {
		Validate.notNull(filter, "filter is null");
		Location playerLoc = player.getEyeLocation();
		World world = Unsafe.assertNonNull(playerLoc.getWorld());
		Vector viewDirection = playerLoc.getDirection();

		// Ray trace to check for the closest block and entity collision:
		// Not ignoring passable blocks, in case some type of shopkeeper object makes use of them.
		RayTraceResult rayTraceResult = world.rayTrace(
				playerLoc,
				viewDirection,
				SHOPKEEPER_TARGET_RANGE,
				FluidCollisionMode.NEVER,
				false,
				0.0D,
				(entity) -> {
					// TODO SPIGOT-5228: Filtering dead entities.
					return !entity.isDead() && !entity.equals(player);
				}
		);

		// Determine the targeted shopkeeper(s), or return a context dependent error messages:
		if (rayTraceResult != null) {
			Block targetBlock = rayTraceResult.getHitBlock();
			if (targetBlock != null) {
				// Get the shopkeeper by the targeted block:
				TargetShopkeepersResult result = getTargetedShopkeeperByBlock(targetBlock, filter);
				if (result != null) {
					return result;
				}

				// Get the player shopkeepers by the targeted container:
				result = getTargetedShopkeepersByContainer(targetBlock, filter);
				if (result != null) {
					return result;
				}
			} else {
				// Get the shopkeeper by the targeted entity:
				Entity targetEntity = Unsafe.assertNonNull(rayTraceResult.getHitEntity());
				TargetShopkeepersResult result = getTargetedShopkeeperByEntity(
						targetEntity,
						filter
				);
				if (result != null) {
					return result;
				}
			}
		}

		// No targeted shopkeeper found:
		return new TargetShopkeepersResult(filter.getNoTargetErrorMsg());
	}

	private static @Nullable TargetShopkeepersResult getTargetedShopkeeperByBlock(
			Block targetBlock,
			TargetShopkeeperFilter filter
	) {
		Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByBlock(targetBlock);
		if (shopkeeper == null) return null;
		if (!filter.test(shopkeeper)) {
			return new TargetShopkeepersResult(filter.getInvalidTargetErrorMsg(shopkeeper));
		}
		return new TargetShopkeepersResult(Collections.singletonList(shopkeeper));
	}

	private static @Nullable TargetShopkeepersResult getTargetedShopkeepersByContainer(
			Block targetBlock,
			TargetShopkeeperFilter filter
	) {
		if (!ShopContainers.isSupportedContainer(targetBlock.getType())) return null;

		ProtectedContainers containerProtections = SKShopkeepersPlugin.getInstance().getProtectedContainers();
		List<? extends PlayerShopkeeper> shopsUsingContainer = containerProtections.getShopkeepersUsingContainer(targetBlock);
		if (shopsUsingContainer.isEmpty()) {
			return new TargetShopkeepersResult(Messages.unusedContainer);
		}

		// Filter shops:
		List<Shopkeeper> acceptedShops = new ArrayList<>();
		for (Shopkeeper shopUsingContainer : shopsUsingContainer) {
			if (filter.test(shopUsingContainer)) {
				acceptedShops.add(shopUsingContainer);
			}
		}
		if (acceptedShops.isEmpty()) {
			// Use the first shopkeeper using the container for the error message:
			return new TargetShopkeepersResult(filter.getInvalidTargetErrorMsg(shopsUsingContainer.get(0)));
		}
		return new TargetShopkeepersResult(acceptedShops);
	}

	private static @Nullable TargetShopkeepersResult getTargetedShopkeeperByEntity(
			Entity targetEntity,
			TargetShopkeeperFilter filter
	) {
		Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByEntity(targetEntity);
		if (shopkeeper == null) {
			return new TargetShopkeepersResult(Messages.targetEntityIsNoShop);
		}
		if (!filter.test(shopkeeper)) {
			return new TargetShopkeepersResult(filter.getInvalidTargetErrorMsg(shopkeeper));
		}
		return new TargetShopkeepersResult(Collections.singletonList(shopkeeper));
	}

	public static List<? extends Shopkeeper> getTargetedShopkeepers(
			Player player,
			TargetShopkeeperFilter shopkeeperFilter
	) {
		TargetShopkeepersResult result = findTargetedShopkeepers(player, shopkeeperFilter);
		if (result.isSuccess()) {
			assert !result.getShopkeepers().isEmpty();
			return result.getShopkeepers();
		} else {
			return Collections.emptyList();
		}
	}

	// Returns an empty list if the command sender is not a player.
	public static List<? extends Shopkeeper> getTargetedShopkeepers(
			CommandSender sender,
			TargetShopkeeperFilter shopkeeperFilter
	) {
		if (sender instanceof Player) {
			return getTargetedShopkeepers((Player) sender, shopkeeperFilter);
		} else {
			return Collections.emptyList();
		}
	}

	public static class OwnedPlayerShopsResult {

		private final @Nullable UUID playerUUID; // Can be null
		private final @Nullable String playerName; // Can be null
		// Stores the player uuids and names of all shop owners found that match the given target
		// player name. If this contains more than one entry then the player name is ambiguous.
		// Not null, can be empty:
		private final Map<? extends UUID, ? extends String> matchingShopOwners;
		private final List<? extends PlayerShopkeeper> shops; // Not null, can be empty

		public OwnedPlayerShopsResult(
				@Nullable UUID playerUUID,
				@Nullable String playerName,
				Map<? extends UUID, ? extends String> matchingShopOwners,
				List<? extends PlayerShopkeeper> shops
		) {
			Validate.isTrue(playerUUID != null || playerName != null,
					"playerUUID and playerName are both null");
			Validate.notNull(matchingShopOwners, "matchingShopOwners is null");
			Validate.notNull(shops, "shops is null");
			this.playerUUID = playerUUID;
			this.playerName = playerName;
			this.matchingShopOwners = matchingShopOwners;
			this.shops = shops;
		}

		public @Nullable UUID getPlayerUUID() {
			return playerUUID;
		}

		public @Nullable String getPlayerName() {
			return playerName;
		}

		public Map<? extends UUID, ? extends String> getMatchingShopOwners() {
			return matchingShopOwners;
		}

		public List<? extends PlayerShopkeeper> getShops() {
			return shops;
		}
	}

	// Searches for shops owned by the player specified by either uuid or name.
	// If at least one matching shop is found, it is used to complete the available information
	// about the target player (e.g. missing uuid or name). The found player name may also differ in
	// case.
	// If shops are searched via target player name, a map of matching shop owners is returned,
	// which stores the player uuids and names of all shop owners found that match the given target
	// player name. If this contains more than one entry then the target player name is ambiguous.
	// The result contains the shops of all those matching players then.
	public static OwnedPlayerShopsResult getOwnedPlayerShops(
			@Nullable UUID targetPlayerUUID,
			@Nullable String targetPlayerName
	) {
		Validate.isTrue(targetPlayerUUID != null || targetPlayerName != null,
				"targetPlayerUUID and targetPlayerName are both null");

		String actualTargetPlayerName = targetPlayerName;

		// Keep track if there are multiple shop owners with matching name:
		Map<UUID, String> matchingShopOwners = new LinkedHashMap<>();

		// Search for shops owned by the specified player:
		List<PlayerShopkeeper> shops = new ArrayList<>();
		for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
			if (!(shopkeeper instanceof PlayerShopkeeper)) {
				continue;
			}

			PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
			UUID shopOwnerUUID = playerShop.getOwnerUUID(); // Not null
			String shopOwnerName = playerShop.getOwnerName(); // Not null
			if (targetPlayerUUID != null) {
				// We search for shops with matching owner uuid:
				if (targetPlayerUUID.equals(shopOwnerUUID)) {
					shops.add(playerShop);

					// The input target player name may be missing or differ in case.
					// Keep track of the owner's actual name:
					actualTargetPlayerName = shopOwnerName;
				}
			} else {
				assert targetPlayerName != null;
				// Check for matching name:
				if (shopOwnerName.equalsIgnoreCase(targetPlayerName)) {
					// Note: If there exist multiple players which match the given name, the result
					// will include the shops of all of them.
					shops.add(playerShop);

					// The input target player name may differ in case.
					// Keep track of the owner's actual name:
					actualTargetPlayerName = shopOwnerName;

					// Keep track of players with matching name:
					matchingShopOwners.putIfAbsent(shopOwnerUUID, shopOwnerName);
				}
			}
		}
		return new OwnedPlayerShopsResult(
				targetPlayerUUID,
				actualTargetPlayerName,
				matchingShopOwners,
				shops
		);
	}

	public static final class ShopkeeperNameMatchers {

		public static final ObjectMatcher<Shopkeeper> DEFAULT = new ObjectMatcher<Shopkeeper>() {
			@Override
			public Stream<? extends Shopkeeper> match(String input) {
				if (StringUtils.isEmpty(input)) return Stream.empty();
				return ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersByName(input);
			}
		};

		private ShopkeeperNameMatchers() {
		}
	}

	private ShopkeeperArgumentUtils() {
	}
}
