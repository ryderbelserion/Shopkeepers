package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKPriceOffer implements PriceOffer {

	private final UnmodifiableItemStack item; // Not null or empty, assumed immutable
	private final int price; // > 0

	/**
	 * Creates a new {@link SKPriceOffer}.
	 * <p>
	 * If the given item stack is an {@link UnmodifiableItemStack}, it is assumed to be immutable and therefore not
	 * copied before it is stored by the price offer. Otherwise, it is first copied.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 */
	public SKPriceOffer(ItemStack item, int price) {
		this(ItemUtils.unmodifiableCloneIfModifiable(item), price);
	}

	/**
	 * Creates a new {@link SKPriceOffer}.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 */
	public SKPriceOffer(UnmodifiableItemStack item, int price) {
		Validate.isTrue(!ItemUtils.isEmpty(item), "item is empty");
		Validate.isTrue(price > 0, "price has to be positive");
		this.item = item;
		this.price = price;
	}

	@Override
	public UnmodifiableItemStack getItem() {
		return item;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKPriceOffer [item=");
		builder.append(item);
		builder.append(", price=");
		builder.append(price);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + item.hashCode();
		result = prime * result + price;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKPriceOffer)) return false;
		SKPriceOffer other = (SKPriceOffer) obj;
		if (price != other.price) return false;
		if (!item.equals(other.item)) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, @ReadOnly Collection<? extends PriceOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 1;
		for (PriceOffer offer : offers) {
			UnmodifiableItemStack item = offer.getItem(); // Assumed immutable
			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			ConfigUtils.saveItemStack(offerSection, "item", item);
			offerSection.set("price", offer.getPrice());
			id++;
		}
	}

	// Elements inside the config section are assumed to be immutable and can be reused without having to be copied.
	public static List<? extends PriceOffer> loadFromConfig(ConfigurationSection config, String node, String errorPrefix) {
		if (errorPrefix == null) errorPrefix = "";
		List<PriceOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String id : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(id);
				if (offerSection == null) continue; // Invalid offer: Not a section.

				// The item stack is assumed to be immutable and therefore does not need to be copied.
				UnmodifiableItemStack item = ConfigUtils.loadUnmodifiableItemStack(offerSection, "item");
				if (ItemUtils.isEmpty(item)) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid price offer for " + id + ": Item is empty.");
					continue;
				}
				int price = offerSection.getInt("price");
				if (price <= 0) {
					// Invalid offer.
					Log.warning(errorPrefix + "Invalid price offer for " + id
							+ ": Price has to be positive, but is " + price + ".");
					continue;
				}
				offers.add(new SKPriceOffer(item, price));
			}
		}
		return offers;
	}

	// Note: Returns the same list instance if no items were migrated.
	public static List<? extends PriceOffer> migrateItems(@ReadOnly List<? extends PriceOffer> offers, String errorPrefix) {
		if (errorPrefix == null) errorPrefix = "";
		if (offers == null) return null;
		List<PriceOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			PriceOffer offer = offers.get(i);
			if (offer == null) continue; // Skip invalid entries

			boolean itemsMigrated = false;
			boolean migrationFailed = false;

			UnmodifiableItemStack item = offer.getItem();
			UnmodifiableItemStack migratedItem = ItemMigration.migrateItemStack(item);
			if (!ItemUtils.isSimilar(item, migratedItem)) {
				if (ItemUtils.isEmpty(migratedItem) && !ItemUtils.isEmpty(item)) {
					migrationFailed = true;
				}
				item = migratedItem;
				itemsMigrated = true;
			}

			if (itemsMigrated) {
				// Lazily setup the list of migrated offers, and add the trades that were already processed but did not
				// require migrations:
				if (migratedOffers == null) {
					migratedOffers = new ArrayList<>(size);
					for (int j = 0; j < i; ++j) {
						PriceOffer oldOffer = offers.get(j);
						if (oldOffer == null) continue; // Skip invalid entries
						migratedOffers.add(oldOffer);
					}
				}

				if (migrationFailed) {
					Log.warning(errorPrefix + "Item migration failed for offer " + (i + 1) + ": " + offer);
					continue; // Skip this offer
				}

				// Add the migrated offer to the list of migrated offers:
				assert !ItemUtils.isEmpty(item);
				migratedOffers.add(new SKPriceOffer(item, offer.getPrice()));
			} else if (migratedOffers != null) {
				// Add the previous offer, which did not require any migrations, to the list of already migrated offers:
				migratedOffers.add(offer);
			}
		}
		return (migratedOffers == null) ? offers : migratedOffers;
	}
}
