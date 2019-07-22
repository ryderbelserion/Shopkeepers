package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
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
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TestPlayerInteractEntityEvent;
import com.nisovin.shopkeepers.util.Utils;

class LivingEntityShopListener implements Listener {

	// the radius around lightning strikes in which villagers turn into witches
	private static final int VILLAGER_ZAP_RADIUS = 7; // minecraft wiki says 3-4, we use 7 to be safe

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
		// ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEntityEvent) return;

		if (!(event.getRightClicked() instanceof LivingEntity)) return;
		LivingEntity shopEntity = (LivingEntity) event.getRightClicked();
		Player player = event.getPlayer();
		Log.debug("Player " + player.getName() + " is interacting (" + (event.getHand()) + ") with entity at " + shopEntity.getLocation());

		// also checks for citizens npc shopkeepers:
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

		// if citizens npc: don't cancel the event, let Citizens perform other actions as appropriate
		if (shopkeeper.getShopObject().getType() != DefaultShopObjectTypes.CITIZEN()) {
			// always cancel interactions with shopkeepers, to prevent any default behavior:
			Log.debug("  Cancelling entity interaction");
			event.setCancelled(true);
			// update inventory in case the interaction would trigger an item action normally (such as animal feeding):
			player.updateInventory();
		}

		// The PlayerInteractAtEntityEvent gets sometimes called additionally to the PlayerInteractEntityEvent.
		// We cancel the event but don't process it any further.
		if (event instanceof PlayerInteractAtEntityEvent) {
			Log.debug("  Ignoring InteractAtEntity event");
			return;
		}

		// only trigger shopkeeper interaction for main-hand events:
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("  Ignoring off-hand interaction");
			return;
		}

		// Check the entity interaction result by calling another interact event:
		if (Settings.checkShopInteractionResult) {
			if (!Utils.checkEntityInteract(player, shopEntity)) {
				Log.debug("  Cancelled by another plugin");
				return;
			}
		}

		// handle interaction:
		shopkeeper.onPlayerInteraction(player);
	}

	// This event gets sometimes called additionally to the PlayerInteractEntityEvent
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
		this.onEntityInteract(event);
	}

	// TODO many of those behaviors might no longer be active, once all entities use noAI (once legacy mob behavior is
	// no longer supported)

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

		// block damaging of shopkeepers
		event.setCancelled(true);
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
			if (evt.getDamager() instanceof Monster) {
				Monster monster = (Monster) evt.getDamager();
				// reset target, future targeting should get prevented somewhere else:
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

	// ex: creepers

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
			Log.debug("Cancelled event for living shop: " + event.getEventName());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onCreeperCharged(CreeperPowerEvent event) {
		if (event.getCause() == PowerCause.LIGHTNING && shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// ex: enderman

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

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onLightningStrike(LightningStrikeEvent event) {
		// workaround: preventing lightning strikes near villager shopkeepers
		// because they would turn into witches
		Location loc = event.getLightning().getLocation();
		for (Entity entity : Utils.getNearbyEntities(loc, VILLAGER_ZAP_RADIUS, EntityType.VILLAGER)) {
			if (shopkeeperRegistry.isShopkeeper(entity)) {
				event.setCancelled(true);
			}
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

	// allow sleeping if the only nearby monsters are shopkeepers:
	// note: cancellation state also reflects default behavior
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerEnterBed(PlayerBedEnterEvent event) {
		// bed entering prevented due to nearby monsters?
		if (event.getBedEnterResult() != BedEnterResult.NOT_SAFE) return;

		// find nearby monsters that prevent bed entering (see MC EntityHuman):
		Block bedBlock = event.getBed();
		Collection<Entity> monsters = bedBlock.getWorld().getNearbyEntities(bedBlock.getLocation(), 8.0D, 5.0D, 8.0D, (entity) -> {
			// TODO bukkit API to check if monster prevents sleeping? ie. pigzombies only prevent sleeping if angered
			return (entity instanceof Monster) && (!(entity instanceof PigZombie) || ((PigZombie) entity).isAngry());
		});

		for (Entity entity : monsters) {
			if (!shopkeeperRegistry.isShopkeeper(entity)) {
				// found non-shopkeeper entity: do nothing (keep bed entering prevented)
				return;
			}
		}
		// sleeping is only prevented due to nearby shopkeepers -> bypass and allow sleeping:
		Log.debug("Allowing sleeping of player '" + event.getPlayer().getName() + "': The only nearby monsters are shopkeepers.");
		event.setUseBed(Result.ALLOW);
	}

	// ex: blazes or skeletons

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityLaunchProjectile(ProjectileLaunchEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof LivingEntity && shopkeeperRegistry.isShopkeeper((LivingEntity) source)) {
			event.setCancelled(true);
		}
	}

	// ex: snowmans

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityBlockForm(EntityBlockFormEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// ex: chicken laying eggs

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityDropItem(EntityDropItemEvent event) {
		if (shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// prevent shopkeeper entities from being affected by potion effects
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityPotionEffectEvent(EntityPotionEffectEvent event) {
		if (event.getAction() == Action.ADDED && shopkeeperRegistry.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}
