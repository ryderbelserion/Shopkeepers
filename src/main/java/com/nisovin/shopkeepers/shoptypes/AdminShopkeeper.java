package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.shoptypes.offers.TradingOffer;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Represents a shopkeeper that is managed by an admin. This shopkeeper will have unlimited supply
 * and will not store earnings anywhere.
 */
public class AdminShopkeeper extends AbstractShopkeeper {

	protected static class AdminShopEditorHandler extends EditorHandler {

		protected AdminShopEditorHandler(AdminShopkeeper shopkeeper) {
			super(SKDefaultUITypes.EDITOR(), shopkeeper);
		}

		@Override
		public AdminShopkeeper getShopkeeper() {
			return (AdminShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			assert player != null;
			return super.canOpen(player) && this.getShopkeeper().getType().hasPermission(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			AdminShopkeeper shopkeeper = this.getShopkeeper();
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
			AdminShopkeeper shopkeeper = this.getShopkeeper();
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

	protected static class AdminShopTradingHandler extends TradingHandler {

		protected AdminShopTradingHandler(AdminShopkeeper shopkeeper) {
			super(SKDefaultUITypes.TRADING(), shopkeeper);
		}

		@Override
		public AdminShopkeeper getShopkeeper() {
			return (AdminShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;
			String tradePermission = this.getShopkeeper().getTradePremission();
			if (tradePermission != null && !Utils.hasPermission(player, tradePermission)) {
				Log.debug("Blocked trade window opening from " + player.getName() + ": Missing custom trade permission.");
				Utils.sendMessage(player, Settings.msgMissingCustomTradePerm);
				return false;
			}
			return true;
		}
	}

	// can contain multiple offers for a specific type of item:
	private final List<TradingOffer> offers = new ArrayList<>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	// kept in sync with offers:
	private final List<TradingRecipe> recipes = new ArrayList<>();
	private final List<TradingRecipe> recipesView = Collections.unmodifiableList(recipes);

	// null indicates that no additional permission is required:
	protected String tradePermission = null;

	/**
	 * Creates a not yet initialized {@link AdminShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected AdminShopkeeper(int id) {
		super(id);
	}

	protected AdminShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected AdminShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new AdminShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new AdminShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// load trade permission:
		tradePermission = configSection.getString("tradePerm", null);
		// load offers:
		this._clearOffers();
		// TODO remove legacy: load offers from old format
		List<TradingOffer> legacyOffers = this.loadFromConfigOld(configSection, "recipes");
		if (!legacyOffers.isEmpty()) {
			this._addOffers(legacyOffers);
			this.markDirty();
		}
		this._addOffers(TradingOffer.loadFromConfig(configSection, "recipes"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save trade permission:
		configSection.set("tradePerm", tradePermission);
		// save offers:
		TradingOffer.saveToConfig(configSection, "recipes", this.getOffers());
	}

	@Override
	public AdminShopType getType() {
		return SKDefaultShopTypes.ADMIN();
	}

	public String getTradePremission() {
		return tradePermission;
	}

	public void setTradePermission(String tradePermission) {
		if (tradePermission == null || tradePermission.isEmpty()) {
			tradePermission = null;
		}
		this.tradePermission = tradePermission;
		this.markDirty();
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

	// legacy code:

	/*private void saveRecipesOld(ConfigurationSection config, String node, Collection<ItemStack[]> recipes) {
		ConfigurationSection recipesSection = config.createSection(node);
		int count = 0;
		for (ItemStack[] recipe : recipes) {
			ConfigurationSection recipeSection = recipesSection.createSection(String.valueOf(count));
			for (int slot = 0; slot < 3; slot++) {
				if (recipe[slot] != null) {
					this.saveItemStackOld(recipe[slot], recipeSection.createSection(String.valueOf(slot)));
				}
			}
			count++;
		}
	}*/

	private List<TradingOffer> loadFromConfigOld(ConfigurationSection config, String node) {
		List<TradingOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				if (offerSection == null) continue; // invalid offer: not a section

				ItemStack resultItem = this.loadItemStackOld(offerSection.getConfigurationSection("2"));
				if (ItemUtils.isEmpty(resultItem)) continue; // invalid offer
				ItemStack item1 = this.loadItemStackOld(offerSection.getConfigurationSection("0"));
				if (ItemUtils.isEmpty(item1)) continue; // invalid offer
				ItemStack item2 = this.loadItemStackOld(offerSection.getConfigurationSection("1"));
				offers.add(new TradingOffer(resultItem, item1, item2));
			}
		}
		return offers;
	}

	/*private List<ItemStack[]> loadRecipesOld(ConfigurationSection config, String node) {
		List<ItemStack[]> recipes = new ArrayList<>();
		ConfigurationSection recipesSection = config.getConfigurationSection(node);
		if (recipesSection != null) {
			for (String key : recipesSection.getKeys(false)) {
				ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
				ItemStack[] recipe = new ItemStack[3];
				for (int slot = 0; slot < 3; slot++) {
					if (recipeSection.isConfigurationSection(String.valueOf(slot))) {
						recipe[slot] = Utils.getNullIfEmpty(this.loadItemStackOld(recipeSection.getConfigurationSection(String.valueOf(slot))));
					}
				}
				if (recipe[0] == null || recipe[2] == null) continue; // invalid recipe
				recipes.add(recipe);
			}
		}
		return recipes;
	}*/

	/**
	 * Loads an ItemStack from a config section.
	 * 
	 * @param section
	 * @return
	 */
	private ItemStack loadItemStackOld(ConfigurationSection section) {
		if (section == null) return null;
		ItemStack item = section.getItemStack("item");
		if (item != null) {
			if (section.contains("attributes")) {
				String attributes = section.getString("attributes");
				if (attributes != null && !attributes.isEmpty()) {
					item = NMSManager.getProvider().loadItemAttributesFromString(item, attributes);
				}
			}
		}
		return item;
	}

	/**
	 * Saves an ItemStack to a config section.
	 * 
	 * @param item
	 * @param config
	 */
	/*private void saveItemStackOld(ItemStack item, ConfigurationSection config) {
		config.set("item", item);
		String attr = NMSManager.getProvider().saveItemAttributesToString(item);
		if (attr != null && !attr.isEmpty()) {
			config.set("attributes", attr);
		}
	}*/
}
