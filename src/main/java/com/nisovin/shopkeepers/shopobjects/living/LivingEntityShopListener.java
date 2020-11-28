package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.CreeperPowerEvent.PowerCause;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.projectiles.ProjectileSource;

import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TestPlayerInteractEntityEvent;
import com.nisovin.shopkeepers.util.Utils;

class LivingEntityShopListener implements Listener {

	private final SKShopkeeperRegistry shopkeeperRegistry;

	LivingEntityShopListener(SKShopkeeperRegistry shopkeeperRegistry) {
		this.shopkeeperRegistry = shopkeeperRegistry;
	}

	// We want to bypass other plugins by default. To allow other plugins (eg. protection plugins) to ignore the event
	// if we have cancelled it, we handle the event as early as possible (LOWEST priority).
	// In order to resolve conflicts with other event handlers potentially running at LOWEST priority, we ignore the
	// event if it already got cancelled.
	// In some usecases it may make sense to take into account if some other plugin wants to cancel the interaction. For
	// those situations the setting check-shop-interaction-result can be used to call an additional interact event
	// that other plugins can react to and which determines whether we handle the interaction. Since this might cause
	// side-effects in general due to other plugins handling the event, this is disabled by default.
	// Using a higher event priority with a setting to ignore whether the event got already cancelled by other
	// plugins is not an option, because then other plugins will already have handled the event and we have no chance to
	// avoid their side-effects (eg. protection plugins will already have sent the player a message, that interaction
	// with the entity is denied).
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onEntityInteract(PlayerInteractEntityEvent event) {
		// Ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEntityEvent) return;
		if (!(event.getRightClicked() instanceof LivingEntity)) return;

		LivingEntity shopEntity = (LivingEntity) event.getRightClicked();
		Player player = event.getPlayer();
		boolean isInteractAtEvent = (event instanceof PlayerInteractAtEntityEvent);
		Log.debug(() -> "Player " + player.getName() + " is interacting (" + (event.getHand()) + ") "
				+ (isInteractAtEvent ? "at" : "with") + " entity at " + shopEntity.getLocation());

		// Also checks for Citizens NPC shopkeepers:
		AbstractShopkeeper shopkeeper = shopkeeperRegistry.getShopkeeperByEntity(shopEntity);
		if (shopkeeper == null) {
			Log.debug("  Non-shopkeeper");
			return;
		}

		// Ignore if already cancelled. Resolves conflicts with other event handlers running at LOWEST priority as well.
		if (event.isCancelled()) {
			Log.debug("  Ignoring already cancelled event");
			return;
		}

		// If Citizens NPC: Don't cancel the event, let Citizens perform other actions as appropriate.
		if (shopkeeper.getShopObject().getType() != DefaultShopObjectTypes.CITIZEN()) {
			// Always cancel interactions with shopkeepers, to prevent any default behavior:
			Log.debug("  Cancelling entity interaction");
			event.setCancelled(true);
			// Update inventory in case the interaction would trigger an item action normally (such as animal feeding):
			player.updateInventory();
		}

		// The PlayerInteractAtEntityEvent gets sometimes called additionally to the PlayerInteractEntityEvent.
		// We cancel the event but don't process it any further.
		if (isInteractAtEvent) {
			Log.debug("  Ignoring InteractAtEntity event");
			return;
		}

		// Only trigger shopkeeper interaction for main-hand events:
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("  Ignoring off-hand interaction");
			return;
		}

		// TODO Minecraft bug: https://bugs.mojang.com/browse/MC-141494
		// Interacting with a villager while holding a written book in the main or off hand results in weird glitches
		// and tricks the plugin into thinking that the editor or trading UI got opened even though the book got opened
		// instead. We therefore ignore any interactions with shopkeeper mobs for now when the interacting player is
		// holding a written book.
		// TODO This has been fixed in MC 1.16. Remove this check once we only support MC 1.16 and above.
		if (hasWrittenBookInHand(player)) {
			Log.debug("  Ignoring interaction due to holding a written book in main or off hand. See Minecraft issue MC-141494.");
			return;
		}

		// Check the entity interaction result by calling another interact event:
		if (Settings.checkShopInteractionResult) {
			if (!Utils.checkEntityInteract(player, shopEntity)) {
				Log.debug("  Cancelled by another plugin");
				return;
			}
		}

		// Handle interaction:
		shopkeeper.onPlayerInteraction(player);
	}

	private static boolean hasWrittenBookInHand(Player player) {
		assert player != null;
		PlayerInventory inventory = player.getInventory();
		return (isWrittenBook(inventory.getItemInMainHand()) || isWrittenBook(inventory.getItemInOffHand()));
	}

	private static boolean isWrittenBook(ItemStack itemStack) {
		return (itemStack != null && itemStack.getType() == Material.WRITTEN_BOOK);
	}

	// This event gets sometimes called additionally to the PlayerInteractEntityEvent
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
		this.onEntityInteract(event);
	}

	// TODO Many of those behaviors might no longer be active, once all entities use NoAI (once legacy mob behavior is
	// no longer supported).

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityTarget(EntityTargetEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity()) || shopkeeperRegistry.isShopkeeper(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!shopkeeperRegistry.isShopkeeper(entity)) return;

		// Block damaging of shopkeepers
		event.setCancelled(true);
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
			if (evt.getDamager() instanceof Monster) {
				Monster monster = (Monster) evt.getDamager();
				// Reset target, future targeting should get prevented somewhere else:
				if (entity.equals(monster.getTarget())) {
					monster.setTarget(null);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityEnterVehicle(VehicleEnterEvent event) {
		Entity entity = event.getEntered();
		if (shopkeeperRegistry.isShopkeeper(entity)) {
			event.setCancelled(true);
		}
	}

	// Example: Creepers.

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onExplodePrime(ExplosionPrimeEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onExplode(EntityExplodeEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
			Log.debug(() -> "Cancelled event for living shop: " + event.getEventName());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onCreeperCharged(CreeperPowerEvent event) {
		if (event.getCause() == PowerCause.LIGHTNING && shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Example: Enderman.

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityTeleport(EntityTeleportEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityPortalTeleport(EntityPortalEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPigZap(PigZapEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onSheepDyed(SheepDyeWoolEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Handles all kinds of events, such as for example villagers struck by lightning turning into witches.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityTransform(EntityTransformEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPotionSplash(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities()) {
			if (shopkeeperRegistry.isShopkeeper(entity)) {
				event.setIntensity(entity, 0.0D);
			}
		}
	}

	// Allow sleeping if the only nearby monsters are shopkeepers:
	// Note: Cancellation state also reflects default behavior.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerEnterBed(PlayerBedEnterEvent event) {
		// Bed entering prevented due to nearby monsters?
		if (event.getBedEnterResult() != BedEnterResult.NOT_SAFE) return;

		// Find nearby monsters that prevent bed entering (see MC EntityHuman):
		Block bedBlock = event.getBed();
		Collection<Entity> monsters = bedBlock.getWorld().getNearbyEntities(bedBlock.getLocation(), 8.0D, 5.0D, 8.0D, (entity) -> {
			// TODO Bukkit API to check if monster prevents sleeping? ie. pigzombies only prevent sleeping if angered
			return (entity instanceof Monster) && (!(entity instanceof PigZombie) || ((PigZombie) entity).isAngry());
		});

		for (Entity entity : monsters) {
			if (!shopkeeperRegistry.isShopkeeper(entity)) {
				// Found non-shopkeeper entity. Do nothing (keep bed entering prevented):
				return;
			}
		}
		// Sleeping is only prevented due to nearby shopkeepers. -> Bypass and allow sleeping:
		Log.debug(() -> "Allowing sleeping of player '" + event.getPlayer().getName() + "': The only nearby monsters are shopkeepers.");
		event.setUseBed(Result.ALLOW);
	}

	// Example: Blazes or skeletons.

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityLaunchProjectile(ProjectileLaunchEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof LivingEntity && shopkeeperRegistry.isShopkeeper((LivingEntity) source)) {
			event.setCancelled(true);
		}
	}

	// Example: Snowmans.

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityBlockForm(EntityBlockFormEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Example: Chicken laying eggs.

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityDropItem(EntityDropItemEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Prevent shopkeeper entities from being affected by potion effects:
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == Action.ADDED && shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// Prevent shopkeeper entities from getting set on fire (eg. monsters in daylight).
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityCombustEvent(EntityCombustEvent event) {
		// If the entity is standing in lava, fire, etc., we ignore the event.
		if (event instanceof EntityCombustByBlockEvent) return;

		Entity entity = event.getEntity();
		if (shopkeeperRegistry.isShopkeeper(entity)) {
			event.setCancelled(true);
		}
	}
}
