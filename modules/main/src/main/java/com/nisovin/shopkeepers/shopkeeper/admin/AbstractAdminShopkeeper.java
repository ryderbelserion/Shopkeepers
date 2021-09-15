package com.nisovin.shopkeepers.shopkeeper.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.trading.TradingHandler;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public abstract class AbstractAdminShopkeeper extends AbstractShopkeeper implements AdminShopkeeper {

	public static class AdminShopTradingHandler extends TradingHandler {

		protected AdminShopTradingHandler(AbstractAdminShopkeeper shopkeeper) {
			super(SKDefaultUITypes.TRADING(), shopkeeper);
		}

		@Override
		public AbstractAdminShopkeeper getShopkeeper() {
			return (AbstractAdminShopkeeper) super.getShopkeeper();
		}

		@Override
		public boolean canOpen(Player player, boolean silent) {
			if (!super.canOpen(player, silent)) return false;

			// Check trading permission:
			String tradePermission = this.getShopkeeper().getTradePermission();
			if (tradePermission != null && !PermissionUtils.hasPermission(player, tradePermission)) {
				if (!silent) {
					this.debugNotOpeningUI(player, "Player is missing the custom trade permission '" + tradePermission + "'.");
					TextUtils.sendMessage(player, Messages.missingCustomTradePerm);
				}
				return false;
			}
			return true;
		}
	}

	// Null if no additional trading permission is required. Not empty.
	protected String tradePermission = null;

	/**
	 * Creates a not yet initialized {@link AbstractAdminShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected AbstractAdminShopkeeper(int id) {
		super(id);
	}

	/**
	 * Expects an {@link AdminShopCreationData}.
	 */
	@Override
	protected void loadFromCreationData(ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super.loadFromCreationData(shopCreationData);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new AdminShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	public void loadDynamicState(ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		super.loadDynamicState(shopkeeperData);
		this.loadTradePermission(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ConfigurationSection shopkeeperData) {
		super.saveDynamicState(shopkeeperData);
		this.saveTradePermission(shopkeeperData);
	}

	// TRADE PERMISSION

	private void loadTradePermission(ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		assert shopkeeperData != null;
		this._setTradePermission(shopkeeperData.getString("tradePerm"));
	}

	private void saveTradePermission(ConfigurationSection shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set("tradePerm", tradePermission);
	}

	@Override
	public String getTradePermission() {
		return tradePermission;
	}

	@Override
	public void setTradePermission(String tradePermission) {
		this._setTradePermission(tradePermission);
		this.markDirty();
	}

	private void _setTradePermission(String tradePermission) {
		this.tradePermission = StringUtils.getNotEmpty(tradePermission);
	}
}
