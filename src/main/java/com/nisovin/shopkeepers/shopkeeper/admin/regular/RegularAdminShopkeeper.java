package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Represents a shopkeeper that is managed by the server. This shopkeeper will have unlimited supply and will not store
 * earnings anywhere.
 */
public class RegularAdminShopkeeper extends AbstractAdminShopkeeper {

	protected static class AdminShopEditorHandler extends EditorHandler {

		protected AdminShopEditorHandler(RegularAdminShopkeeper shopkeeper) {
			super(SKDefaultUITypes.EDITOR(), shopkeeper);
		}

		@Override
		public RegularAdminShopkeeper getShopkeeper() {
			return (RegularAdminShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			assert player != null;
			return super.canOpen(player) && this.getShopkeeper().getType().hasPermission(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			RegularAdminShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's trade offers:
			List<TradingOffer> offers = shopkeeper.getOffers();
			for (int column = 0; column < offers.size() && column < TRADE_COLUMNS; column++) {
				TradingOffer offer = offers.get(column);
				inventory.setItem(column, offer.getItem1());
				inventory.setItem(column + 9, offer.getItem2());
				inventory.setItem(column + 18, offer.getResultItem());
			}
			// add the special buttons:
			this.setActionButtons(inventory);
			// show editing inventory:
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			RegularAdminShopkeeper shopkeeper = this.getShopkeeper();
			shopkeeper.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack cost1 = ItemUtils.getNullIfEmpty(inventory.getItem(column));
				ItemStack cost2 = ItemUtils.getNullIfEmpty(inventory.getItem(column + 9));
				ItemStack resultItem = ItemUtils.getNullIfEmpty(inventory.getItem(column + 18));

				// handle cost2 item as cost1 item if there is no cost1 item:
				if (cost1 == null) {
					cost1 = cost2;
					cost2 = null;
				}

				if (cost1 != null && resultItem != null) {
					// add trading recipe:
					shopkeeper.addOffer(resultItem, cost1, cost2);
				} else if (player != null) {
					// return unused items to inventory:
					if (cost1 != null) {
						player.getInventory().addItem(cost1);
					}
					if (cost2 != null) {
						player.getInventory().addItem(cost2);
					}
					if (resultItem != null) {
						player.getInventory().addItem(resultItem);
					}
				}
			}
		}
	}

	// can contain multiple offers for a specific type of item:
	private final List<TradingOffer> offers = new ArrayList<>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	// kept in sync with offers:
	private final List<TradingRecipe> recipes = new ArrayList<>();
	private final List<TradingRecipe> recipesView = Collections.unmodifiableList(recipes);

	/**
	 * Creates a not yet initialized {@link RegularAdminShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected RegularAdminShopkeeper(int id) {
		super(id);
	}

	protected RegularAdminShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected RegularAdminShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new AdminShopEditorHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// load offers:
		this._clearOffers();
		this._addOffers(TradingOffer.loadFromConfig(configSection, "recipes"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		TradingOffer.saveToConfig(configSection, "recipes", this.getOffers());
	}

	@Override
	public RegularAdminShopType getType() {
		return SKDefaultShopTypes.ADMIN();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		return recipesView;
	}

	// OFFERS:

	public List<TradingOffer> getOffers() {
		return offersView;
	}

	public TradingOffer addOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		// create offer (also handles validation):
		TradingOffer newOffer = new TradingOffer(resultItem, item1, item2);

		// add new offer:
		this._addOffer(newOffer);
		this.markDirty();
		return newOffer;
	}

	private void _addOffer(TradingOffer offer) {
		assert offer != null;
		offers.add(offer);
		recipes.add(offer); // TradingOffer extends TradingRecipe
	}

	private void _addOffers(Collection<TradingOffer> offers) {
		assert offers != null;
		for (TradingOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer:
			this._addOffer(offer);
		}
	}

	private void _clearOffers() {
		offers.clear();
		recipes.clear();
	}

	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}
}
