package com.nisovin.shopkeepers.tradenotifications;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.text.ClickEventText.Action;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.Lazy;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.text.MessageArguments;
import com.nisovin.shopkeepers.util.trading.MergedTrades;
import com.nisovin.shopkeepers.util.trading.TradeMerger;
import com.nisovin.shopkeepers.util.trading.TradeMerger.MergeMode;

/**
 * Informs certain players and/or shop owners about trades that take place.
 */
public class TradeNotifications implements Listener {

	private static class TradeContext {

		private final MergedTrades mergedTrades;
		private final Lazy<MessageArguments> shopMessageArguments;
		private final Lazy<Map<String, Object>> tradeMessageArguments;
		private final Lazy<Boolean> isResultItemCurrency;

		TradeContext(MergedTrades mergedTrades) {
			this.mergedTrades = mergedTrades;
			shopMessageArguments = new Lazy<>(() -> ((AbstractShopkeeper) this.getShopkeeper()).getMessageArguments("shop_"));
			tradeMessageArguments = new Lazy<>(() -> createTradeMessageArguments(this));
			isResultItemCurrency = new Lazy<>(() -> Settings.isCurrencyItem(this.getResultItem()));
		}

		public Player getTradingPlayer() {
			return mergedTrades.getInitialTrade().getPlayer();
		}

		public Shopkeeper getShopkeeper() {
			return mergedTrades.getInitialTrade().getShopkeeper();
		}

		/**
		 * Gets the result item of the trades. See {@link ShopkeeperTradeEvent#getTradingRecipe()} and
		 * {@link TradingRecipe#getResultItem()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the result item
		 */
		public ItemStack getResultItem() {
			return mergedTrades.getResultItem();
		}

		public boolean isResultItemCurrency() {
			return isResultItemCurrency.get();
		}

		public int getResultItemAmount() {
			return this.getResultItem().getAmount();
		}

		/**
		 * Gets the first offered item of the trades. See {@link ShopkeeperTradeEvent#getOfferedItem1()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the first offered item
		 */
		public ItemStack getOfferedItem1() {
			return mergedTrades.getOfferedItem1();
		}

		public int getItem1Amount() {
			return this.getOfferedItem1().getAmount();
		}

		private boolean hasOfferedItem2() {
			return mergedTrades.getInitialTrade().hasOfferedItem2();
		}

		/**
		 * Gets the second offered item of the trades. See {@link ShopkeeperTradeEvent#getOfferedItem2()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the second offered item, or <code>null</code>
		 */
		public ItemStack getOfferedItem2() {
			return mergedTrades.getOfferedItem2();
		}

		public int getItem2Amount() {
			if (this.getOfferedItem2() == null) return 0;
			return this.getOfferedItem2().getAmount();
		}

		public int getTradeCount() {
			return mergedTrades.getTradeCount();
		}

		public MessageArguments getShopMessageArguments() {
			return shopMessageArguments.get();
		}

		public Map<String, Object> getTradeMessageArguments() {
			return tradeMessageArguments.get();
		}
	}

	private static Map<String, Object> createTradeMessageArguments(TradeContext tradeContext) {
		Player player = tradeContext.getTradingPlayer();
		Map<String, Object> msgArgs = new HashMap<>();
		msgArgs.put("player", player.getName());
		msgArgs.put("playerId", (Supplier<Object>) () -> player.getUniqueId().toString());
		msgArgs.put("resultItem", (Supplier<Object>) () -> TextUtils.getItemText(tradeContext.getResultItem()));
		msgArgs.put("resultItemAmount", (Supplier<Object>) () -> tradeContext.getResultItemAmount());
		msgArgs.put("item1", (Supplier<Object>) () -> TextUtils.getItemText(tradeContext.getOfferedItem1()));
		msgArgs.put("item1Amount", (Supplier<Object>) () -> tradeContext.getItem1Amount());
		msgArgs.put("item2", (Supplier<Object>) () -> TextUtils.getItemText(tradeContext.getOfferedItem2()));
		msgArgs.put("item2Amount", (Supplier<Object>) () -> tradeContext.getItem2Amount());
		return msgArgs;
	}

	private static final long TRADE_MERGE_DURATION_TICKS = 100L; // 5 seconds

	private final Plugin plugin;
	private final NotificationUserPreferences userPreferences;
	private final TradeMerger tradeMerger;

	private boolean enabled;

	public TradeNotifications(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.userPreferences = new NotificationUserPreferences(plugin);
		this.tradeMerger = new TradeMerger(plugin, MergeMode.DURATION, this::onTradesCompleted)
				.withMergeDuration(TRADE_MERGE_DURATION_TICKS);
	}

	public void onEnable() {
		this.enabled = (Settings.notifyPlayersAboutTrades || Settings.notifyShopOwnersAboutTrades);
		if (!enabled) return;

		Bukkit.getPluginManager().registerEvents(this, plugin);
		userPreferences.onEnable();
		tradeMerger.onEnable();
	}

	public void onDisable() {
		if (!enabled) return;
		enabled = false;

		tradeMerger.onDisable();
		userPreferences.onDisable();
		HandlerList.unregisterAll(this);
	}

	public NotificationUserPreferences getUserPreferences() {
		return userPreferences;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTradeCompleted(ShopkeeperTradeEvent event) {
		tradeMerger.mergeTrade(event);
	}

	private void onTradesCompleted(MergedTrades mergedTrades) {
		TradeContext tradeContext = new TradeContext(mergedTrades);
		this.sendTradeNotifications(tradeContext);
		this.sendOwnerTradeNotifications(tradeContext);
	}

	private void sendTradeNotifications(TradeContext tradeContext) {
		assert tradeContext != null;
		if (!Settings.notifyPlayersAboutTrades) return;

		Player shopOwner = null;
		String tradeNotificationPermission = ShopkeepersPlugin.TRADE_NOTIFICATIONS_ADMIN;
		if (tradeContext.getShopkeeper() instanceof PlayerShopkeeper) {
			tradeNotificationPermission = ShopkeepersPlugin.TRADE_NOTIFICATIONS_PLAYER;
			shopOwner = ((PlayerShopkeeper) tradeContext.getShopkeeper()).getOwner();
		}

		Lazy<Text> tradeNotification = new Lazy<>(() -> this.getTradeNotificationMessage(tradeContext));
		for (Player player : Bukkit.getOnlinePlayers()) {
			// Avoid notifying the shop owner twice.
			// Note that the shop owner may have deactivated the trade notification for this particular shopkeeper. In
			// this case, they will not receive either type of trade notification.
			if (player == shopOwner && Settings.notifyShopOwnersAboutTrades) continue;
			if (!userPreferences.isNotifyOnTrades(player)) continue;
			if (!PermissionUtils.hasPermission(player, tradeNotificationPermission)) continue;

			// Note: We also send trade notifications for own trades (i.e. when the trading player matches the recipient
			// of the notification).
			TextUtils.sendMessage(player, tradeNotification.get());
			Settings.tradeNotificationSound.play(player);
			this.sendDisableTradeNotificationsHint(player);
		}
	}

	private Text getTradeNotificationMessage(TradeContext tradeContext) {
		assert tradeContext != null;
		Shopkeeper shopkeeper = tradeContext.getShopkeeper();

		Text message;
		// We avoid checking for specific shop types (eg. buying shop) and instead check if the result item is currency:
		if (tradeContext.isResultItemCurrency()) {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.buyNotificationTwoItems;
			} else {
				message = Messages.buyNotificationOneItem;
			}
		} else {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.tradeNotificationTwoItems;
			} else {
				message = Messages.tradeNotificationOneItem;
			}
		}

		Text shopText;
		if (shopkeeper.getName().isEmpty()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				shopText = Messages.tradeNotificationPlayerShop;
			} else {
				shopText = Messages.tradeNotificationAdminShop;
			}
		} else {
			if (shopkeeper instanceof PlayerShopkeeper) {
				shopText = Messages.tradeNotificationNamedPlayerShop;
			} else {
				shopText = Messages.tradeNotificationNamedAdminShop;
			}
		}

		Text tradeCountText = Text.EMPTY;
		if (tradeContext.getTradeCount() > 1) {
			tradeCountText = Messages.tradeNotificationTradeCount;
		}

		return this.getTradeNotificationMessage(tradeContext, message, shopText, tradeCountText);
	}

	private Text getTradeNotificationMessage(TradeContext tradeContext, Text message, Text shopText, Text tradeCountText) {
		assert tradeContext != null;
		MessageArguments shopMsgArgs = tradeContext.getShopMessageArguments();
		Map<String, Object> tradeMsgArgs = tradeContext.getTradeMessageArguments();

		shopText.setPlaceholderArguments(shopMsgArgs);
		// TODO Display more shop information as hover text? Add a click event or insertion text to automatically copy
		// the shop coordinates or id, or insert a teleport command to teleport to the shop?
		tradeMsgArgs.put("shop", shopText);

		tradeCountText.setPlaceholderArguments("count", tradeContext.getTradeCount());
		tradeMsgArgs.put("trade_count", tradeCountText);

		message.setPlaceholderArguments(tradeMsgArgs);
		message.setPlaceholderArguments(shopMsgArgs);

		return message;
	}

	private void sendOwnerTradeNotifications(TradeContext tradeContext) {
		assert tradeContext != null;
		if (!Settings.notifyShopOwnersAboutTrades) return;
		if (!(tradeContext.getShopkeeper() instanceof PlayerShopkeeper)) return;

		PlayerShopkeeper playerShop = (PlayerShopkeeper) tradeContext.getShopkeeper();
		if (!playerShop.isNotifyOnTrades()) return;
		Player owner = playerShop.getOwner();
		if (owner == null) return; // Owner is offline
		if (!userPreferences.isNotifyOnTrades(owner)) return;

		// Note: We also send trade notifications for own trades (i.e. when the trading player matches the recipient of
		// the notification).
		Text message = this.getOwnerTradeNotificationMessage(tradeContext);
		TextUtils.sendMessage(owner, message);
		Settings.shopOwnerTradeNotificationSound.play(owner);
		this.sendDisableTradeNotificationsHint(owner);
	}

	private Text getOwnerTradeNotificationMessage(TradeContext tradeContext) {
		assert tradeContext != null;
		Shopkeeper shopkeeper = tradeContext.getShopkeeper();

		// We avoid checking for specific shop types (eg. buying shop) and instead check if the result item is currency:
		boolean isBuy = tradeContext.isResultItemCurrency();
		Text message;
		if (isBuy) {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.ownerBuyNotificationTwoItems;
			} else {
				message = Messages.ownerBuyNotificationOneItem;
			}
		} else {
			if (tradeContext.hasOfferedItem2()) {
				message = Messages.ownerTradeNotificationTwoItems;
			} else {
				message = Messages.ownerTradeNotificationOneItem;
			}
		}

		Text shopText;
		if (isBuy) {
			if (shopkeeper.getName().isEmpty()) {
				shopText = Messages.ownerBuyNotificationShop;
			} else {
				shopText = Messages.ownerBuyNotificationNamedShop;
			}
		} else {
			if (shopkeeper.getName().isEmpty()) {
				shopText = Messages.ownerTradeNotificationShop;
			} else {
				shopText = Messages.ownerTradeNotificationNamedShop;
			}
		}

		Text tradeCountText = Text.EMPTY;
		if (tradeContext.getTradeCount() > 1) {
			tradeCountText = Messages.ownerTradeNotificationTradeCount;
		}

		return this.getTradeNotificationMessage(tradeContext, message, shopText, tradeCountText);
	}

	private void sendDisableTradeNotificationsHint(Player player) {
		if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.NOTIFY_TRADES_PERMISSION)) return;

		// We only send this once per session:
		if (userPreferences.hasReceivedDisableTradeNotificationsHint(player)) return;
		userPreferences.setReceivedDisableTradeNotificationsHint(player, true);

		Text command = Messages.disableTradeNotificationsHintCommand.copy(); // TODO Avoid this copy
		Text commandText = Text.clickEvent(Action.SUGGEST_COMMAND, command.toPlainText()).next(command).getRoot();
		TextUtils.sendMessage(player, Messages.disableTradeNotificationsHint, "command", commandText);
	}
}
