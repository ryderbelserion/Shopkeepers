package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

/**
 * Utility functions related to shopkeepers.
 */
public class ShopkeeperUtils {

	private ShopkeeperUtils() {
	}

	public static final String SHOPKEEPER_METADATA_KEY = "shopkeeper";

	public static boolean isShopkeeper(Entity entity) {
		return entity.hasMetadata(SHOPKEEPER_METADATA_KEY);
	}

	private static final int SHOPKEEPER_TARGET_RANGE = 10;

	public static final class TargetShopkeepersResult {

		private final List<Shopkeeper> shopkeepers;
		private final Text errorMessage;
		// assert: !shopkeepers.isEmpty || errorMessage != null

		private TargetShopkeepersResult(List<Shopkeeper> shopkeepers) {
			Validate.isTrue(shopkeepers != null && !shopkeepers.isEmpty());
			this.shopkeepers = shopkeepers;
			this.errorMessage = null;
		}

		private TargetShopkeepersResult(Text errorMessage) {
			Validate.notNull(errorMessage);
			Validate.isTrue(!errorMessage.isPlainTextEmpty());
			this.errorMessage = errorMessage;
			this.shopkeepers = Collections.emptyList();
		}

		public boolean isSuccess() {
			return (errorMessage == null);
		}

		public List<Shopkeeper> getShopkeepers() {
			return shopkeepers;
		}

		public Text getErrorMessage() {
			return errorMessage;
		}
	}

	public interface TargetShopkeeperFilter extends Predicate<Shopkeeper> {

		public static final TargetShopkeeperFilter ANY = new TargetShopkeeperFilter() {
			@Override
			public boolean test(Shopkeeper shopkeeper) {
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
			public boolean test(Shopkeeper shopkeeper) {
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
			public boolean test(Shopkeeper shopkeeper) {
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
	public static TargetShopkeepersResult getTargetedShopkeepers(Player player, TargetShopkeeperFilter shopkeeperFilter) {
		if (shopkeeperFilter == null) shopkeeperFilter = TargetShopkeeperFilter.ANY;
		Location playerLoc = player.getEyeLocation();
		World world = playerLoc.getWorld();
		Vector viewDirection = playerLoc.getDirection();

		// Ray trace to check for the closest block and entity collision:
		// Not ignoring passable blocks, in case some type of shopkeeper object makes use of them.
		RayTraceResult rayTraceResult = world.rayTrace(playerLoc, viewDirection, SHOPKEEPER_TARGET_RANGE, FluidCollisionMode.NEVER, false, 0.0D, (entity) -> {
			return !entity.isDead() && !entity.equals(player); // TODO SPIGOT-5228: Filtering dead entities.
		});

		// Determine targeted shopkeeper, and return context dependent failure messages:
		if (rayTraceResult != null) {
			Shopkeeper shopkeeper = null;
			Block targetBlock = rayTraceResult.getHitBlock();
			if (targetBlock != null) {
				// Get shopkeeper by targeted block:
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByBlock(targetBlock);
				if (shopkeeper == null) {
					// Get player shopkeepers by targeted container:
					if (ShopContainers.isSupportedContainer(targetBlock.getType())) {
						List<PlayerShopkeeper> shopsUsingContainer = SKShopkeepersPlugin.getInstance().getProtectedContainers().getShopkeepersUsingContainer(targetBlock);
						if (shopsUsingContainer.isEmpty()) {
							return new TargetShopkeepersResult(Messages.unusedContainer);
						} else {
							// Filter shops:
							List<Shopkeeper> acceptedShops = new ArrayList<>();
							for (Shopkeeper shopUsingContainer : shopsUsingContainer) {
								if (shopkeeperFilter.test(shopUsingContainer)) {
									acceptedShops.add(shopUsingContainer);
								}
							}
							if (acceptedShops.isEmpty()) {
								// Use the first shopkeeper using the container for the error message:
								return new TargetShopkeepersResult(shopkeeperFilter.getInvalidTargetErrorMsg(shopsUsingContainer.get(0)));
							} else {
								return new TargetShopkeepersResult(acceptedShops);
							}
						}
					}
				}
			} else {
				Entity targetEntity = rayTraceResult.getHitEntity();
				assert targetEntity != null;
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByEntity(targetEntity);
				if (shopkeeper == null) {
					return new TargetShopkeepersResult(Messages.targetEntityIsNoShop);
				}
			}

			// Check if found shopkeeper is accepted:
			if (shopkeeper != null) {
				if (shopkeeperFilter.test(shopkeeper)) {
					return new TargetShopkeepersResult(Arrays.asList(shopkeeper)); // accepted
				} else {
					return new TargetShopkeepersResult(shopkeeperFilter.getInvalidTargetErrorMsg(shopkeeper));
				}
			}
		}

		// No targeted shopkeeper found:
		return new TargetShopkeepersResult(shopkeeperFilter.getNoTargetErrorMsg());
	}

	public static class OwnedPlayerShopsResult {

		private final UUID playerUUID; // Can be null
		private final String playerName; // Can be null
		// Stores the player uuids and names of all shop owners found that match the given target player name. If this
		// contains more than one entry then the player name is ambiguous.
		private final Map<UUID, String> matchingShopOwners; // Not null, can be empty
		private final List<? extends PlayerShopkeeper> shops; // Not null, can be empty

		public OwnedPlayerShopsResult(UUID playerUUID, String playerName, Map<UUID, String> matchingShopOwners, List<? extends PlayerShopkeeper> shops) {
			Validate.isTrue(playerUUID != null || playerName != null, "The player uuid and name are both null!");
			Validate.notNull(matchingShopOwners, "Matching shop owners map is null!");
			this.playerUUID = playerUUID;
			this.playerName = playerName;
			this.matchingShopOwners = matchingShopOwners;
			this.shops = shops;
		}

		public UUID getPlayerUUID() {
			return playerUUID;
		}

		public String getPlayerName() {
			return playerName;
		}

		public Map<UUID, String> getMatchingShopOwners() {
			return matchingShopOwners;
		}

		public List<? extends PlayerShopkeeper> getShops() {
			return shops;
		}
	}

	// Searches for shops owned by the player specified by either uuid or name.
	// If at least one matching shop is found, it is used to complete the available information about the target player
	// (eg. missing uuid or name). The found player name may also differ in case.
	// If shops are searched via target player name, a map of matching shop owners is returned, which stores the player
	// uuids and names of all shop owners found that match the given target player name. If this contains more than one
	// entry then the target player name is ambiguous. The result contains the shops of all those matching players then.
	public static OwnedPlayerShopsResult getOwnedPlayerShops(UUID targetPlayerUUID, String targetPlayerName) {
		Validate.isTrue(targetPlayerUUID != null || targetPlayerName != null, "The target player uuid and name are both null!");

		// Keep track if there are multiple shop owners with matching name:
		Map<UUID, String> matchingShopOwners = new LinkedHashMap<>();

		// Search for shops owned by the specified player:
		List<PlayerShopkeeper> shops = new ArrayList<>();
		for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				UUID shopOwnerUUID = playerShop.getOwnerUUID(); // Not null
				String shopOwnerName = playerShop.getOwnerName(); // Not null
				if (targetPlayerUUID != null) {
					// We search for shops with matching owner uuid:
					if (targetPlayerUUID.equals(shopOwnerUUID)) {
						shops.add(playerShop);

						// The input target player name may be missing or differ in case.
						// Keep track of the owner's actual name:
						targetPlayerName = shopOwnerName;
					}
				} else {
					assert targetPlayerName != null;
					// Check for matching name:
					if (shopOwnerName.equalsIgnoreCase(targetPlayerName)) {
						// Note: If there exist multiple players which match the given name, the result will include the
						// shops of all of them.
						shops.add(playerShop);

						// The input target player name may differ in case.
						// Keep track of the owner's actual name:
						targetPlayerName = shopOwnerName;

						// Keep track of players with matching name:
						matchingShopOwners.putIfAbsent(shopOwnerUUID, shopOwnerName);
					}
				}
			}
		}
		return new OwnedPlayerShopsResult(targetPlayerUUID, targetPlayerName, matchingShopOwners, shops);
	}

	public static class ShopkeeperNameMatchers {

		private ShopkeeperNameMatchers() {
		}

		public static final ObjectMatcher<Shopkeeper> DEFAULT = new ObjectMatcher<Shopkeeper>() {
			@Override
			public Stream<? extends Shopkeeper> match(String input) {
				if (StringUtils.isEmpty(input)) return Stream.empty();
				return ShopkeepersAPI.getShopkeeperRegistry().getShopkeepersByName(input);
			}
		};
	}
}
