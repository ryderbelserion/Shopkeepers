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

import com.nisovin.shopkeepers.SKShopkeeper;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.TradingRecipe;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.shoptypes.offers.TradingOffer;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Represents a shopkeeper that is managed by an admin. This shopkeeper will have unlimited supply
 * and will not store earnings anywhere.
 */
public class AdminShopkeeper extends SKShopkeeper {

	protected static class AdminShopEditorHandler extends EditorHandler {

		protected AdminShopEditorHandler(UIType uiType, AdminShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
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

		protected AdminShopTradingHandler(UIType uiType, AdminShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
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
	private final List<TradingOffer> offers = new ArrayList<TradingOffer>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	// kept in sync with offers:
	private final List<TradingRecipe> recipes = new ArrayList<TradingRecipe>();
	private final List<TradingRecipe> recipesView = Collections.unmodifiableList(recipes);

	// null indicates that no additional permission is required:
	protected String tradePermission = null;

	/**
	 * For use in extending classes.
	 */
	protected AdminShopkeeper() {
	}

	protected AdminShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	protected AdminShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new AdminShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new AdminShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		// load trade permission:
		tradePermission = config.getString("tradePerm", null);
		// load offers:
		this.clearOffers();
		// TODO remove legacy: load offers from old format
		this.addOffers(this.loadFromConfigOld(config, "recipes"));
		this.addOffers(TradingOffer.loadFromConfig(config, "recipes"));
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		// save trade permission:
		config.set("tradePerm", tradePermission);
		// save offers:
		TradingOffer.saveToConfig(config, "recipes", this.getOffers());
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.ADMIN();
	}

	public String getTradePremission() {
		return tradePermission;
	}

	public void setTradePermission(String tradePermission) {
		if (tradePermission == null || tradePermission.isEmpty()) {
			tradePermission = null;
		}
		this.tradePermission = tradePermission;
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
		this.addOffer(newOffer);
		return newOffer;
	}

	private void addOffer(TradingOffer offer) {
		assert offer != null;
		offers.add(offer);
		recipes.add(offer); // TradingOffer extends TradingRecipe
	}

	private void addOffers(Collection<TradingOffer> offers) {
		assert offers != null;
		for (TradingOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer:
			this.addOffer(offer);
		}
	}

	public void clearOffers() {
		offers.clear();
		recipes.clear();
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
		List<TradingOffer> offers = new ArrayList<TradingOffer>();
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
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
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
