package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.MathUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class EditorHandler extends UIHandler {

	protected static final int COLUMNS_PER_ROW = 9;
	// 9 columns, column = [0,8]
	protected static final int TRADES_COLUMNS = 9;

	protected static final int TRADES_ROW_1_START = 0;
	protected static final int TRADES_ROW_1_END = TRADES_ROW_1_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_ROW_2_START = TRADES_ROW_1_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_ROW_2_END = TRADES_ROW_2_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_ROW_3_START = TRADES_ROW_2_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_ROW_3_END = TRADES_ROW_3_START + TRADES_COLUMNS - 1;

	// TODO: config setting?
	protected static final int TRADES_MAX_PAGES = 5; // 45 trades, double chest can hold 54 different items
	protected static final int TRADES_PAGE_BAR_START = TRADES_ROW_3_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_PAGE_BAR_END = TRADES_PAGE_BAR_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_PAGE_ICON = TRADES_PAGE_BAR_START + (TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START) / 2;
	protected static final int TRADES_SETUP_ICON = TRADES_PAGE_ICON - 1;

	protected static final int BUTTONS_START = TRADES_PAGE_BAR_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int BUTTON_MAX_ROWS = 2;

	// slot = column + offset:
	protected static final int RESULT_ITEM_OFFSET = TRADES_ROW_1_START;
	protected static final int ITEM_1_OFFSET = TRADES_ROW_3_START;
	protected static final int ITEM_2_OFFSET = TRADES_ROW_2_START;

	protected EditorHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);

		this.setupTradesPageBarButtons();
		this.setupButtons();
	}

	// INVENTORY LAYOUT

	protected int getInventorySize() {
		return COLUMNS_PER_ROW * (4 + this.getButtonRows());
	}

	protected boolean isResultRow(int rawSlot) {
		return rawSlot >= TRADES_ROW_1_START && rawSlot <= TRADES_ROW_1_END;
	}

	protected boolean isItem1Row(int rawSlot) {
		return rawSlot >= TRADES_ROW_3_START && rawSlot <= TRADES_ROW_3_END;
	}

	protected boolean isItem2Row(int rawSlot) {
		return rawSlot >= TRADES_ROW_2_START && rawSlot <= TRADES_ROW_2_END;
	}

	protected boolean isTradesArea(int rawSlot) {
		return this.isResultRow(rawSlot) || this.isItem1Row(rawSlot) || this.isItem2Row(rawSlot);
	}

	protected boolean isTradesPageBar(int rawSlot) {
		return rawSlot >= TRADES_PAGE_BAR_START && rawSlot <= TRADES_PAGE_BAR_END;
	}

	protected boolean isButtonArea(int rawSlot) {
		return rawSlot >= BUTTONS_START && rawSlot <= this.getButtonsEnd();
	}

	// depends on the number of buttons rows currently used
	protected int getButtonsEnd() {
		return BUTTONS_START + (this.getButtonRows() * COLUMNS_PER_ROW) - 1;
	}

	protected boolean isPlayerInventory(InventoryView view, SlotType slotType, int rawSlot) {
		return rawSlot >= view.getTopInventory().getSize() && (slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR);
	}

	// TRADES AREA

	protected void setTradeColumn(Inventory inventory, int column, TradingRecipeDraft recipe) {
		if (inventory == null) return;
		inventory.setItem(column + RESULT_ITEM_OFFSET, recipe.getResultItem());
		inventory.setItem(column + ITEM_1_OFFSET, recipe.getItem1());
		inventory.setItem(column + ITEM_2_OFFSET, recipe.getItem2());
	}

	protected TradingRecipeDraft getTradingRecipe(Inventory inventory, int column) {
		ItemStack resultItem = inventory.getItem(column + RESULT_ITEM_OFFSET);
		ItemStack item1 = inventory.getItem(column + ITEM_1_OFFSET);
		ItemStack item2 = inventory.getItem(column + ITEM_2_OFFSET);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	// EDITOR BUTTONS

	public static abstract class Button {

		private static final int NO_SLOT = -1;

		protected final Shopkeeper shopkeeper;
		private final boolean placeAtEnd;

		private EditorHandler editorHandler;
		private int slot = NO_SLOT;

		public Button(Shopkeeper shopkeeper) {
			this(shopkeeper, false);
		}

		public Button(Shopkeeper shopkeeper, boolean placeAtEnd) {
			Validate.notNull(shopkeeper);
			this.shopkeeper = shopkeeper;
			this.placeAtEnd = placeAtEnd;
		}

		private void setEditorHandler(EditorHandler editorHandler) {
			if (this.editorHandler != null) {
				throw new IllegalStateException("Button already registered!");
			}
			this.editorHandler = editorHandler;
		}

		public abstract ItemStack getIcon(Session session);

		// updates the icon in all sessions
		// note: cannot deal with changes to the registered buttons (the button's slot) while the inventory is open
		protected final void updateIcon() {
			if (slot != NO_SLOT && editorHandler != null) {
				for (Session session : editorHandler.sessions.values()) {
					session.inventory.setItem(slot, this.getIcon(session));
					session.player.updateInventory();
				}
			}
		}

		// updates all icons in all sessions
		protected final void updateAllIcons() {
			if (editorHandler != null) {
				for (Session session : editorHandler.sessions.values()) {
					editorHandler.updateButtons(session);
					session.player.updateInventory();
				}
			}
		}

		protected abstract void onClick(InventoryClickEvent clickEvent, Player player);
	}

	// for simple one-click actions
	public static abstract class ActionButton extends Button {

		public ActionButton(Shopkeeper shopkeeper) {
			super(shopkeeper);
		}

		public ActionButton(Shopkeeper shopkeeper, boolean placeAtEnd) {
			super(shopkeeper, placeAtEnd);
		}

		@Override
		protected final void onClick(InventoryClickEvent clickEvent, Player player) {
			if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // ignore double clicks

			// run action:
			boolean success = this.runAction(clickEvent, player);
			if (!success) return;

			// icon might have changed:
			this.updateIcon();

			// call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// save:
			shopkeeper.save();
		}

		// returns true on success
		protected abstract boolean runAction(InventoryClickEvent clickEvent, Player player);
	}

	private final Button[] tradesPageBarButtons = new Button[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START + 1];

	private Button[] getTradesPageBarButtons() {
		this.setupTradesPageBarButtons();
		return tradesPageBarButtons;
	}

	private Button getTradesPageBarButton(int rawSlot) {
		if (!this.isTradesPageBar(rawSlot)) return null;
		return this._getTradesPageBarButton(rawSlot);
	}

	private Button _getTradesPageBarButton(int rawSlot) {
		assert this.isTradesPageBar(rawSlot);
		return tradesPageBarButtons[rawSlot - TRADES_PAGE_BAR_START];
	}

	protected void setupTradesPageBarButtons() {
		Shopkeeper shopkeeper = this.getShopkeeper();

		Button prevPageButton = this.createPrevPageButton(shopkeeper);
		prevPageButton.slot = TRADES_PAGE_BAR_START;
		tradesPageBarButtons[0] = prevPageButton;

		Button tradeSetupButton = this.createTradeSetupButton(shopkeeper);
		tradeSetupButton.slot = TRADES_SETUP_ICON;
		tradesPageBarButtons[TRADES_SETUP_ICON - TRADES_PAGE_BAR_START] = tradeSetupButton;

		Button currentPageButton = this.createCurrentPageButton(shopkeeper);
		currentPageButton.slot = TRADES_PAGE_ICON;
		tradesPageBarButtons[TRADES_PAGE_ICON - TRADES_PAGE_BAR_START] = currentPageButton;

		Button nextPageButton = this.createNextPageButton(shopkeeper);
		nextPageButton.slot = TRADES_PAGE_BAR_END;
		tradesPageBarButtons[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START] = nextPageButton;
	}

	protected Button createPrevPageButton(Shopkeeper shopkeeper) {
		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
				if (page <= 1) return null;
				return createPrevPageIcon(page);
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// previous page button:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // ignore double clicks
				Session session = getSession(player);
				if (session == null) return;

				// save current page:
				saveEditorPage(session);

				// update page:
				session.setPage(Math.max(1, session.currentPage - 1));
				setupPage(player, session.currentPage);
				player.updateInventory();
			}
		};
	}

	protected Button createNextPageButton(Shopkeeper shopkeeper) {
		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
				if (page >= TRADES_MAX_PAGES) return null;
				return createNextPageIcon(page);
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// next page button:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // ignore double clicks
				Session session = getSession(player);
				if (session == null) return;

				// save current page:
				saveEditorPage(session);

				// update page:
				session.setPage(Math.min(TRADES_MAX_PAGES, session.currentPage + 1));
				setupPage(player, session.currentPage);
				player.updateInventory();
			}
		};
	}

	protected Button createCurrentPageButton(Shopkeeper shopkeeper) {
		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
				return createCurrentPageIcon(page);
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// current page button: doing nothing
			}
		};
	}

	protected Button createTradeSetupButton(Shopkeeper shopkeeper) {
		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				return createTradeSetupIcon();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// trade setup button: doing nothing
			}
		};
	}

	protected ItemStack createPrevPageIcon(int page) {
		int prevPage = 1;
		String prevPageText = "-";
		if (page > 1) {
			prevPage = (page - 1);
			prevPageText = String.valueOf(prevPage);
		}
		String itemName = TextUtils.replaceArgs(Settings.msgButtonPreviousPage,
				"{prev_page}", prevPageText,
				"{page}", String.valueOf(page),
				"{max_page}", String.valueOf(TRADES_MAX_PAGES));
		ItemStack item = Settings.previousPageItem.createItemStack();
		// note: can exceed the item's natural max stack size
		item.setAmount(MathUtils.trim(prevPage, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setItemStackNameAndLore(item, itemName, Settings.msgButtonPreviousPageLore);
	}

	protected ItemStack createNextPageIcon(int page) {
		int nextPage = 1;
		String nextPageText = "-";
		if (page < TRADES_MAX_PAGES) {
			nextPage = (page + 1);
			nextPageText = String.valueOf(nextPage);
		}
		String itemName = TextUtils.replaceArgs(Settings.msgButtonNextPage,
				"{next_page}", nextPageText,
				"{page}", String.valueOf(page),
				"{max_page}", String.valueOf(TRADES_MAX_PAGES));
		ItemStack item = Settings.nextPageItem.createItemStack();
		// note: can exceed the item's natural max stack size
		item.setAmount(MathUtils.trim(nextPage, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setItemStackNameAndLore(item, itemName, Settings.msgButtonNextPageLore);
	}

	protected ItemStack createCurrentPageIcon(int page) {
		String itemName = TextUtils.replaceArgs(Settings.msgButtonCurrentPage,
				"{page}", String.valueOf(page),
				"{max_page}", String.valueOf(TRADES_MAX_PAGES));
		ItemStack item = Settings.currentPageItem.createItemStack();
		// note: can exceed the item's natural max stack size
		item.setAmount(MathUtils.trim(page, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setItemStackNameAndLore(item, itemName, Settings.msgButtonCurrentPageLore);
	}

	protected ItemStack createTradeSetupIcon() {
		ShopType<?> shopType = this.getShopkeeper().getType();
		String itemName = TextUtils.replaceArgs(Settings.msgTradeSetupDescHeader,
				"{shopType}", shopType.getDisplayName());
		return ItemUtils.setItemStackNameAndLore(Settings.tradeSetupItem.createItemStack(), itemName, shopType.getTradeSetupDescription());
	}

	private final List<Button> buttons = new ArrayList<>();
	private int buttonRows = 1;
	private final Button[] bakedButtons = new Button[BUTTON_MAX_ROWS * COLUMNS_PER_ROW];
	private boolean dirtyButtons = false;

	private void bakeButtons() {
		if (!dirtyButtons) return;

		// reset buttons:
		for (Button button : buttons) {
			button.slot = Button.NO_SLOT;
		}

		// clear array:
		for (int i = 0; i < bakedButtons.length; ++i) {
			bakedButtons[i] = null;
		}

		// insert buttons:
		int frontIndex = 0;
		this.buttonRows = Math.min(BUTTON_MAX_ROWS, ((buttons.size() - 1) / COLUMNS_PER_ROW) + 1);
		int endIndex = buttonRows * COLUMNS_PER_ROW - 1;
		for (int i = 0; i < buttons.size(); ++i) {
			Button button = buttons.get(i);
			int buttonIndex;
			if (button.placeAtEnd) {
				buttonIndex = endIndex;
				endIndex--;
			} else {
				buttonIndex = frontIndex;
				frontIndex++;
			}
			if (bakedButtons[buttonIndex] != null) {
				// there is not enough space for the remaining buttons
				break;
			}
			bakedButtons[buttonIndex] = button;
			button.slot = BUTTONS_START + buttonIndex;
		}
	}

	private int getButtonRows() {
		this.bakeButtons();
		return buttonRows;
	}

	private Button[] getBakedButtons() {
		this.bakeButtons();
		return bakedButtons;
	}

	private Button getButton(int rawSlot) {
		if (!this.isButtonArea(rawSlot)) return null;
		this.bakeButtons();
		return bakedButtons[rawSlot - BUTTONS_START];
	}

	private Button _getButton(int rawSlot) {
		assert this.isButtonArea(rawSlot);
		this.bakeButtons();
		return bakedButtons[rawSlot - BUTTONS_START];
	}

	public void addButton(Button button) {
		Validate.notNull(button, "Button is null");
		button.setEditorHandler(this); // validates that the button isn't used elsewhere yet
		buttons.add(button);
		dirtyButtons = true;
	}

	protected void addButtonOrIgnore(Button button) {
		if (button == null) return; // ignore
		this.addButton(button);
	}

	protected void addButtonsOrIgnore(Iterable<Button> buttons) {
		if (buttons == null) return;
		for (Button button : buttons) {
			this.addButtonOrIgnore(button);
		}
	}

	protected void setupButtons() {
		AbstractShopkeeper shopkeeper = this.getShopkeeper();
		this.addButtonOrIgnore(this.createDeleteButton(shopkeeper));
		this.addButtonOrIgnore(this.createNamingButton(shopkeeper));
		this.addButtonOrIgnore(this.createChestButton(shopkeeper));
		this.addButtonsOrIgnore(shopkeeper.getShopObject().getEditorButtons());
	}

	protected Button createDeleteButton(Shopkeeper shopkeeper) {
		return new Button(shopkeeper, true) {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createDeleteButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// delete button - delete shopkeeper:
				// call event:
				PlayerDeleteShopkeeperEvent deleteEvent = new PlayerDeleteShopkeeperEvent(shopkeeper, player);
				Bukkit.getPluginManager().callEvent(deleteEvent);
				if (deleteEvent.isCancelled()) {
					Log.debug("ShopkeeperDeleteEvent was cancelled!");
				} else {
					// return shop creation item for player shopkeepers:
					if (Settings.deletingPlayerShopReturnsCreationItem && shopkeeper.getType() instanceof PlayerShopType) {
						ItemStack shopCreationItem = Settings.createShopCreationItem();
						Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
						if (!remaining.isEmpty()) {
							player.getWorld().dropItem(shopkeeper.getObjectLocation(), shopCreationItem);
						}
					}

					// delete shopkeeper:
					// this also deactivates the ui and closes all open windows for this shopkeeper after a delay
					shopkeeper.delete();

					// save:
					shopkeeper.save();
				}
			}
		};
	}

	protected Button createNamingButton(Shopkeeper shopkeeper) {
		boolean useNamingButton = true;
		if (shopkeeper.getType() instanceof PlayerShopType) {
			// naming via button enabled?
			if (Settings.namingOfPlayerShopsViaItem) {
				useNamingButton = false;
			} else {
				// no naming button for citizens player shops if renaming is disabled for those
				// TODO restructure this to allow for dynamic editor buttons depending on shop (object) types and
				// settings
				if (!Settings.allowRenamingOfPlayerNpcShops && shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
					useNamingButton = false;
				}
			}
		}
		if (!useNamingButton) return null;

		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createNameButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// naming button:
				closeEditorAndRunTask(player, null);

				// start naming:
				SKShopkeepersPlugin.getInstance().getShopkeeperNaming().startNaming(player, shopkeeper);
				TextUtils.sendMessage(player, Settings.msgTypeNewName);
			}
		};
	}

	protected Button createChestButton(Shopkeeper shopkeeper) {
		if (!Settings.enableChestOptionOnPlayerShop || !(shopkeeper.getType() instanceof PlayerShopType)) {
			return null;
		}
		return new Button(shopkeeper) {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createChestButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// chest inventory button:
				closeEditorAndRunTask(player, () -> {
					// open chest inventory:
					if (!player.isValid()) return;
					((PlayerShopkeeper) shopkeeper).openChestWindow(player);
				});
			}
		};
	}

	protected void closeEditorAndRunTask(Player player, Runnable task) {
		Shopkeeper shopkeeper = this.getShopkeeper();

		// ignore other click events for this shopkeeper in the same tick:
		shopkeeper.deactivateUI();

		// close editor window delayed and run task afterwards:
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			// this triggers closing and saving of the editor state (if this inventory didn't get closed for other
			// reasons in the meantime already)
			player.closeInventory();

			// reactivate ui for this shopkeeper:
			shopkeeper.activateUI();

			// run task:
			if (task != null) {
				task.run();
			}
		});
	}

	// PLAYER SESSIONS

	public static final class Session {

		private final Player player;
		private final List<TradingRecipeDraft> recipes;
		private final Inventory inventory;
		private int currentPage = 1;

		private Session(Player player, List<TradingRecipeDraft> recipes, Inventory inventory) {
			this.player = player;
			this.recipes = recipes;
			this.inventory = inventory;
		}

		public final Player getPlayer() {
			return player;
		}

		// starts at 1
		public final int getCurrentPage() {
			return currentPage;
		}

		private void setPage(int newPage) {
			this.currentPage = newPage;
		}

		public final List<TradingRecipeDraft> getRecipes() {
			return recipes;
		}
	}

	private final Map<UUID, Session> sessions = new HashMap<>();

	protected Session getSession(Player player) {
		if (player == null) return null;
		return sessions.get(player.getUniqueId());
	}

	protected abstract List<TradingRecipeDraft> getTradingRecipes();

	@Override
	public boolean openWindow(Player player) {
		// setup session:
		List<TradingRecipeDraft> recipes = this.getTradingRecipes();
		// create inventory:
		Inventory inventory = Bukkit.createInventory(player, this.getInventorySize(), Settings.editorTitle);
		Session session = new Session(player, recipes, inventory);
		sessions.put(player.getUniqueId(), session);

		// setup and open first page:
		this.setupPage(player, 1);
		player.openInventory(session.inventory);
		return true;
	}

	protected void setupPage(Player player, int page) {
		Session session = this.getSession(player);
		if (session == null) return; // expecting a valid session

		// setup inventory:
		this.setupTradeColumns(session);
		this.setupTradesPageBar(session);
		this.setupButtons(session);
	}

	protected void setupTradeColumns(Session session) {
		assert session != null;
		Inventory inventory = session.inventory;

		// clear trades area:
		for (int i = TRADES_ROW_1_START; i <= TRADES_ROW_1_END; ++i) {
			inventory.setItem(i, null);
		}
		for (int i = TRADES_ROW_2_START; i <= TRADES_ROW_2_END; ++i) {
			inventory.setItem(i, null);
		}
		for (int i = TRADES_ROW_3_START; i <= TRADES_ROW_3_END; ++i) {
			inventory.setItem(i, null);
		}

		// insert trades:
		int page = session.currentPage;
		assert page >= 1;
		List<TradingRecipeDraft> recipes = session.recipes;
		int recipesPerPage = COLUMNS_PER_ROW;
		int startIndex = (page - 1) * recipesPerPage;
		for (int column = 0, i = startIndex; column < COLUMNS_PER_ROW && i < recipes.size(); ++column, ++i) {
			TradingRecipeDraft recipe = recipes.get(i);
			this.setTradeColumn(inventory, column, recipe);
		}
	}

	protected void setupTradesPageBar(Session session) {
		assert session != null;
		Inventory inventory = session.inventory;
		// clear page bar area:
		for (int i = TRADES_PAGE_BAR_START; i <= TRADES_PAGE_BAR_END; ++i) {
			inventory.setItem(i, null);
		}

		// insert buttons:
		Button[] buttons = this.getTradesPageBarButtons();
		for (int i = 0; i < buttons.length; ++i) {
			Button button = buttons[i];
			if (button == null) continue;
			ItemStack icon = button.getIcon(session);
			if (icon == null) continue;
			inventory.setItem(button.slot, icon);
		}
	}

	// note: this cannot deal with new button rows being required due to newly added buttons (which would require
	// creating and freshly open a new inventory, resulting in flicker)
	protected void updateButtons(Session session) {
		this.setupButtons(session);
	}

	// also used to refresh all button icons in an already open inventory
	protected void setupButtons(Session session) {
		Inventory inventory = session.inventory;
		final int inventorySize = inventory.getSize();
		Button[] buttons = this.getBakedButtons();
		for (int buttonIndex = 0; buttonIndex < buttons.length; ++buttonIndex) {
			int slot = BUTTONS_START + buttonIndex;
			if (slot >= inventorySize) break; // this can be reached if called on a previously setup inventory
			ItemStack icon = null;
			Button button = buttons[buttonIndex];
			if (button != null) {
				icon = button.getIcon(session);
			}
			// null will clear the slot (required if this is called to refresh the buttons in an already setup
			// inventory):
			inventory.setItem(slot, icon);
		}
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		// permission for the type of shopkeeper is checked in the AdminShopkeeper specific EditorHandler
		// owner is checked in the PlayerShopkeeper specific EditorHandler
		return true;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		return view != null && view.getTitle().equals(Settings.editorTitle);
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
		// dragging is allowed by default only inside the player inventory and the trades area:
		if (event.isCancelled()) return; // already cancelled
		InventoryView view = event.getView();
		Set<Integer> slots = event.getRawSlots();
		for (Integer slot : slots) {
			if (!this.isTradesArea(slot) && !this.isPlayerInventory(view, view.getSlotType(slot), slot)) {
				event.setCancelled(true);
				break;
			}
		}
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		Session session = this.getSession(player);
		assert session != null;

		int rawSlot = event.getRawSlot();
		if (this.isTradesArea(rawSlot)) {
			// trades area:
			this.handleTradesClick(session, event);
		} else if (this.isTradesPageBar(rawSlot)) {
			// trades page bar:
			this.handleTradesPageBarClick(session, event);
		} else if (this.isButtonArea(rawSlot)) {
			// editor buttons:
			this.handleButtonClick(session, event);
		} else if (this.isPlayerInventory(event.getView(), event.getSlotType(), rawSlot)) {
			// player inventory:
			this.handlePlayerInventoryClick(session, event);
		}
	}

	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		assert this.isTradesArea(event.getRawSlot());
	}

	protected void handleTradesPageBarClick(Session session, InventoryClickEvent event) {
		assert this.isTradesPageBar(event.getRawSlot());
		event.setCancelled(true);
		Player player = session.player;
		int rawSlot = event.getRawSlot();
		Button button = this._getTradesPageBarButton(rawSlot);
		if (button != null) {
			button.onClick(event, player);
		}
	}

	protected void handleButtonClick(Session session, InventoryClickEvent event) {
		assert this.isButtonArea(event.getRawSlot());
		event.setCancelled(true);
		Player player = session.player;
		int rawSlot = event.getRawSlot();
		Button button = this._getButton(rawSlot);
		if (button != null) {
			button.onClick(event, player);
		}
	}

	protected void handlePlayerInventoryClick(Session session, InventoryClickEvent event) {
		assert this.isPlayerInventory(event.getView(), event.getSlotType(), event.getRawSlot());
	}

	protected int getNewAmountAfterEditorClick(InventoryClickEvent event, int currentAmount, int minAmount, int maxAmount) {
		// validate bounds:
		if (minAmount > maxAmount) return currentAmount; // no valid value possible
		if (minAmount == maxAmount) return minAmount; // only one valid value possible

		int newAmount = currentAmount;
		ClickType clickType = event.getClick();
		switch (clickType) {
		case LEFT:
			newAmount += 1;
			break;
		case SHIFT_LEFT:
			newAmount += 10;
			break;
		case RIGHT:
			newAmount -= 1;
			break;
		case SHIFT_RIGHT:
			newAmount -= 10;
			break;
		case MIDDLE:
			newAmount = minAmount;
			break;
		case NUMBER_KEY:
			assert event.getHotbarButton() >= 0;
			newAmount = event.getHotbarButton() + 1;
			break;
		default:
			break;
		}
		// bounds:
		if (newAmount < minAmount) newAmount = minAmount;
		if (newAmount > maxAmount) newAmount = maxAmount;
		return newAmount;
	}

	@Override
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		// cleanup session:
		Session session = sessions.remove(player.getUniqueId());

		if (closeEvent != null) {
			// only saving if caused by an inventory close event:
			this.saveEditor(session);

			// call event:
			Shopkeeper shopkeeper = this.getShopkeeper();
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			shopkeeper.closeAllOpenWindows();
			shopkeeper.save();
		}
	}

	/**
	 * Saves the current page of the editor interface.
	 * 
	 * @param session
	 *            the session
	 */
	protected void saveEditorPage(Session session) {
		assert session != null;
		// if (!session.dirtyPage) return; // skip if not dirty
		Inventory inventory = session.inventory;
		int page = session.currentPage;
		assert page >= 1;
		List<TradingRecipeDraft> recipes = session.recipes;

		int recipesPerPage = COLUMNS_PER_ROW;
		int startIndex = (page - 1) * recipesPerPage;
		int endIndex = startIndex + TRADES_COLUMNS - 1;
		// add empty recipes to support the recipes of the current page:
		for (int i = recipes.size(); i <= endIndex; ++i) {
			recipes.add(TradingRecipeDraft.EMPTY);
		}

		// replace recipes:
		for (int column = 0; column < TRADES_COLUMNS; column++) {
			TradingRecipeDraft recipeDraft = this.getTradingRecipe(inventory, column);
			int recipeIndex = startIndex + column;
			recipes.set(recipeIndex, recipeDraft);
		}
	}

	/**
	 * Saves the current state of the editor interface.
	 * 
	 * @param session
	 *            the session
	 */
	protected void saveEditor(Session session) {
		assert session != null;
		// save current page:
		this.saveEditorPage(session);

		// save recipes:
		Player player = session.player;
		this.clearRecipes();
		for (TradingRecipeDraft recipe : session.recipes) {
			if (!recipe.isValid()) {
				this.handleInvalidRecipeDraft(player, recipe);
				continue;
			}
			this.addRecipe(player, recipe);
		}
	}

	// called for every recipe draft that is not valid:
	protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
	}

	protected abstract void clearRecipes();

	protected abstract void addRecipe(Player player, TradingRecipeDraft recipe);
}
