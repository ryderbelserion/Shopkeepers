package com.nisovin.shopkeepers.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.villagerEditor.VillagerEditorHandler;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for editor UIs which support editing multiple pages of trades and offer additional editor buttons.
 * <p>
 * For example used by {@link EditorHandler} and {@link VillagerEditorHandler}.
 */
public abstract class AbstractEditorHandler extends UIHandler {

	private static final SoundEffect PAGE_TURN_SOUND = new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN);

	protected static final int COLUMNS_PER_ROW = 9;
	// 9 columns, column = [0,8]
	protected static final int TRADES_COLUMNS = 9;

	protected static final int TRADES_ROW_1_START = 0;
	protected static final int TRADES_ROW_1_END = TRADES_ROW_1_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_ROW_2_START = TRADES_ROW_1_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_ROW_2_END = TRADES_ROW_2_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_ROW_3_START = TRADES_ROW_2_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_ROW_3_END = TRADES_ROW_3_START + TRADES_COLUMNS - 1;

	protected static final int TRADES_PAGE_BAR_START = TRADES_ROW_3_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	protected static final int TRADES_PAGE_BAR_END = TRADES_PAGE_BAR_START + TRADES_COLUMNS - 1;
	protected static final int TRADES_PAGE_ICON = TRADES_PAGE_BAR_START + (TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START) / 2;
	protected static final int TRADES_SETUP_ICON = TRADES_PAGE_ICON - 1;

	protected static final int BUTTONS_START = TRADES_PAGE_BAR_END + (COLUMNS_PER_ROW - TRADES_COLUMNS) + 1;
	// TODO If there are more buttons than we can fit into two rows, move the excess buttons into a separate (paged)
	// inventory view and add an editor button that opens it.
	protected static final int BUTTON_MAX_ROWS = 2;

	// slot = column + offset:
	protected static final int RESULT_ITEM_OFFSET = TRADES_ROW_1_START;
	protected static final int ITEM_1_OFFSET = TRADES_ROW_3_START;
	protected static final int ITEM_2_OFFSET = TRADES_ROW_2_START;

	private final Button[] tradesPageBarButtons = new Button[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START + 1];
	private final List<Button> buttons = new ArrayList<>();
	private int buttonRows = 1;
	private final Button[] bakedButtons = new Button[BUTTON_MAX_ROWS * COLUMNS_PER_ROW];
	private boolean dirtyButtons = false;
	private boolean setup = false; // lazy setup

	protected final TradingRecipesAdapter tradingRecipesAdapter;

	private final Map<UUID, Session> sessions = new HashMap<>();

	protected AbstractEditorHandler(AbstractUIType uiType, TradingRecipesAdapter tradingRecipesAdapter) {
		super(uiType);
		Validate.notNull(tradingRecipesAdapter, "tradingRecipesAdapter is null");
		this.tradingRecipesAdapter = tradingRecipesAdapter;
	}

	/**
	 * Performs any setup that has to happen once for this UI.
	 * <p>
	 * This may for example setup the UI contents, such as registering buttons.
	 * <p>
	 * Setup is performed lazily, before the first time the UI is opened.
	 */
	private void setup() {
		if (setup) return;
		setup = true;
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

	// Depends on the number of buttons rows currently used:
	protected int getButtonsEnd() {
		return BUTTONS_START + (this.getButtonRows() * COLUMNS_PER_ROW) - 1;
	}

	protected boolean isPlayerInventory(InventoryView view, SlotType slotType, int rawSlot) {
		return rawSlot >= view.getTopInventory().getSize() && (slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR);
	}

	// TRADES AREA

	// assert: [1, 10].
	protected int getMaxTradesPages() {
		return Settings.maxTradesPages;
	}

	protected void setTradeColumn(Inventory inventory, int column, TradingRecipeDraft recipe) {
		if (inventory == null) return;
		// The inventory implementations create NMS copies of the items, so we do not need to copy them ourselves here:
		inventory.setItem(column + RESULT_ITEM_OFFSET, ItemUtils.asItemStackOrNull(recipe.getResultItem()));
		inventory.setItem(column + ITEM_1_OFFSET, ItemUtils.asItemStackOrNull(recipe.getItem1()));
		inventory.setItem(column + ITEM_2_OFFSET, ItemUtils.asItemStackOrNull(recipe.getItem2()));
	}

	// TODO Avoid creating new TradingRecipeDraft objects here and instead update the drafts of the session?
	protected TradingRecipeDraft getTradingRecipe(Inventory inventory, int column) {
		ItemStack resultItem = inventory.getItem(column + RESULT_ITEM_OFFSET);
		ItemStack item1 = inventory.getItem(column + ITEM_1_OFFSET);
		ItemStack item2 = inventory.getItem(column + ITEM_2_OFFSET);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	// EDITOR BUTTONS

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
		Button prevPageButton = this.createPrevPageButton();
		prevPageButton.setSlot(TRADES_PAGE_BAR_START);
		tradesPageBarButtons[0] = prevPageButton;

		Button tradeSetupButton = this.createTradeSetupButton();
		tradeSetupButton.setSlot(TRADES_SETUP_ICON);
		tradesPageBarButtons[TRADES_SETUP_ICON - TRADES_PAGE_BAR_START] = tradeSetupButton;

		Button currentPageButton = this.createCurrentPageButton();
		currentPageButton.setSlot(TRADES_PAGE_ICON);
		tradesPageBarButtons[TRADES_PAGE_ICON - TRADES_PAGE_BAR_START] = currentPageButton;

		Button nextPageButton = this.createNextPageButton();
		nextPageButton.setSlot(TRADES_PAGE_BAR_END);
		tradesPageBarButtons[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START] = nextPageButton;
	}

	protected Button createPrevPageButton() {
		return new ActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.getCurrentPage();
				if (page <= 1) return null;
				return createPrevPageIcon(page);
			}

			@Override
			protected void playButtonClickSound(Player player, boolean actionSuccess) {
				if (actionSuccess) {
					PAGE_TURN_SOUND.play(player);
				}
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				// Ignore double clicks:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return false;

				Session session = getSession(player);
				if (session == null) return false;

				// Save the current page:
				saveEditorPage(session);

				// Switch to previous page:
				int currentPage = session.getCurrentPage();
				int newPage = Math.max(1, currentPage - 1);
				if (newPage == currentPage) return false; // Page has not changed

				// Update page:
				session.setPage(newPage);
				setupPage(player, newPage);
				player.updateInventory();
				return true;
			}
		};
	}

	protected Button createNextPageButton() {
		return new ActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.getCurrentPage();
				if (page >= getMaxTradesPages()) return null;
				return createNextPageIcon(page);
			}

			@Override
			protected void playButtonClickSound(Player player, boolean actionSuccess) {
				if (actionSuccess) {
					PAGE_TURN_SOUND.play(player);
				}
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				// Ignore double clicks:
				if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return false;

				Session session = getSession(player);
				if (session == null) return false;

				// Save the current page:
				saveEditorPage(session);

				// Switch to next page:
				int currentPage = session.getCurrentPage();
				int newPage = Math.min(getMaxTradesPages(), currentPage + 1);
				if (newPage == currentPage) return false; // Page has not changed

				// Update page:
				session.setPage(newPage);
				setupPage(player, newPage);
				player.updateInventory();
				return true;
			}
		};
	}

	protected Button createCurrentPageButton() {
		return new Button() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.getCurrentPage();
				return createCurrentPageIcon(page);
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Current page button: Does nothing.
			}
		};
	}

	protected Button createTradeSetupButton() {
		return new Button() {
			@Override
			public ItemStack getIcon(Session session) {
				return createTradeSetupIcon();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Trade setup button: Does nothing.
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
		String itemName = StringUtils.replaceArguments(Messages.buttonPreviousPage,
				"prev_page", prevPageText,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.previousPageItem.createItemStack();
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.trim(prevPage, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setDisplayNameAndLore(item, itemName, Messages.buttonPreviousPageLore);
	}

	protected ItemStack createNextPageIcon(int page) {
		int nextPage = 1;
		String nextPageText = "-";
		if (page < getMaxTradesPages()) {
			nextPage = (page + 1);
			nextPageText = String.valueOf(nextPage);
		}
		String itemName = StringUtils.replaceArguments(Messages.buttonNextPage,
				"next_page", nextPageText,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.nextPageItem.createItemStack();
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.trim(nextPage, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setDisplayNameAndLore(item, itemName, Messages.buttonNextPageLore);
	}

	protected ItemStack createCurrentPageIcon(int page) {
		String itemName = StringUtils.replaceArguments(Messages.buttonCurrentPage,
				"page", page,
				"max_page", getMaxTradesPages()
		);
		ItemStack item = Settings.currentPageItem.createItemStack();
		// Note: Can exceed the item's natural max stack size.
		item.setAmount(MathUtils.trim(page, 1, ItemUtils.MAX_STACK_SIZE));
		return ItemUtils.setDisplayNameAndLore(item, itemName, Messages.buttonCurrentPageLore);
	}

	protected abstract ItemStack createTradeSetupIcon();

	private void bakeButtons() {
		if (!dirtyButtons) return;

		// Reset buttons:
		for (Button button : buttons) {
			button.setSlot(Button.NO_SLOT);
		}

		// Clear array:
		for (int i = 0; i < bakedButtons.length; ++i) {
			bakedButtons[i] = null;
		}

		// Insert buttons:
		int frontIndex = 0;
		this.buttonRows = Math.min(BUTTON_MAX_ROWS, ((buttons.size() - 1) / COLUMNS_PER_ROW) + 1);
		int endIndex = buttonRows * COLUMNS_PER_ROW - 1;
		for (int i = 0; i < buttons.size(); ++i) {
			Button button = buttons.get(i);
			int buttonIndex;
			if (button.isPlaceAtEnd()) {
				buttonIndex = endIndex;
				endIndex--;
			} else {
				buttonIndex = frontIndex;
				frontIndex++;
			}
			if (bakedButtons[buttonIndex] != null) {
				// There is not enough space for the remaining buttons.
				break;
			}
			bakedButtons[buttonIndex] = button;
			button.setSlot(BUTTONS_START + buttonIndex);
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
		Validate.isTrue(button.isApplicable(this), "The button is incompatible with this type of editor handler!");
		button.setEditorHandler(this); // Validates that the button isn't used elsewhere yet
		buttons.add(button);
		dirtyButtons = true;
	}

	protected void addButtonOrIgnore(Button button) {
		if (button == null) return; // Ignore
		this.addButton(button);
	}

	protected void addButtonsOrIgnore(Iterable<Button> buttons) {
		if (buttons == null) return;
		for (Button button : buttons) {
			this.addButtonOrIgnore(button);
		}
	}

	protected void setupButtons() {
		// None by default.
	}

	// PLAYER SESSIONS

	/**
	 * Creates a new {@link Session}.
	 * <p>
	 * The given list of recipes is not copied, but edited directly.
	 * 
	 * @param player
	 *            the editing player, not <code>null</code>
	 * @param recipes
	 *            the trading recipe drafts, not <code>null</code> and expected to be modifiable
	 * @param inventory
	 *            the editor inventory, not <code>null</code>
	 * @return the session, not <code>null</code>
	 */
	protected Session createSession(Player player, List<TradingRecipeDraft> recipes, Inventory inventory) {
		return new Session(player, recipes, inventory);
	}

	protected Session getSession(Player player) {
		if (player == null) return null;
		return sessions.get(player.getUniqueId());
	}

	@Override
	public boolean openWindow(Player player) {
		// Lazy setup:
		this.setup();

		// Setup session:
		List<TradingRecipeDraft> recipes = tradingRecipesAdapter.getTradingRecipes();

		// Create inventory:
		Inventory inventory = Bukkit.createInventory(player, this.getInventorySize(), this.getEditorTitle());
		Session session = this.createSession(player, recipes, inventory);
		sessions.put(player.getUniqueId(), session);

		// Setup and open first page:
		this.setupPage(player, 1);
		player.openInventory(inventory);
		return true;
	}

	protected abstract String getEditorTitle();

	protected void setupPage(Player player, int page) {
		Session session = this.getSession(player);
		if (session == null) return; // Expecting a valid session

		// Setup inventory:
		this.setupTradeColumns(session);
		this.setupTradesPageBar(session);
		this.setupButtons(session);
	}

	protected void setupTradeColumns(Session session) {
		assert session != null;
		Inventory inventory = session.getInventory();

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

		// Insert trades:
		int page = session.getCurrentPage();
		assert page >= 1;
		List<TradingRecipeDraft> recipes = session.getRecipes();
		int recipesPerPage = COLUMNS_PER_ROW;
		int startIndex = (page - 1) * recipesPerPage;
		for (int column = 0, i = startIndex; column < COLUMNS_PER_ROW && i < recipes.size(); ++column, ++i) {
			TradingRecipeDraft recipe = recipes.get(i);
			this.setTradeColumn(inventory, column, recipe);
		}
	}

	protected void setupTradesPageBar(Session session) {
		assert session != null;
		Inventory inventory = session.getInventory();
		// Clear page bar area:
		for (int i = TRADES_PAGE_BAR_START; i <= TRADES_PAGE_BAR_END; ++i) {
			inventory.setItem(i, null);
		}

		// Insert buttons:
		Button[] buttons = this.getTradesPageBarButtons();
		for (int i = 0; i < buttons.length; ++i) {
			Button button = buttons[i];
			if (button == null) continue;
			ItemStack icon = button.getIcon(session);
			if (icon == null) continue;
			inventory.setItem(button.getSlot(), icon);
		}
	}

	// Note: This cannot deal with new button rows being required due to newly added buttons (which would require
	// creating and freshly open a new inventory, resulting in flicker).
	protected void updateButtons(Session session) {
		this.setupButtons(session);
	}

	void updateButtonsInAllSessions() {
		for (Session session : sessions.values()) {
			this.updateButtons(session);
			session.getPlayer().updateInventory();
		}
	}

	// Also used to refresh all button icons in an already open inventory.
	protected void setupButtons(Session session) {
		Inventory inventory = session.getInventory();
		final int inventorySize = inventory.getSize();
		Button[] buttons = this.getBakedButtons();
		for (int buttonIndex = 0; buttonIndex < buttons.length; ++buttonIndex) {
			int slot = BUTTONS_START + buttonIndex;
			if (slot >= inventorySize) break; // This can be reached if called on a previously setup inventory

			ItemStack icon = null;
			Button button = buttons[buttonIndex];
			if (button != null) {
				icon = button.getIcon(session);
			}
			// Null will clear the slot (required if this is called to refresh the buttons in an already setup
			// inventory):
			inventory.setItem(slot, icon);
		}
	}

	void updateButtonInAllSessions(Button button) {
		int slot = button.getSlot();
		for (Session session : sessions.values()) {
			ItemStack icon = button.getIcon(session);
			session.getInventory().setItem(slot, icon);
			session.getPlayer().updateInventory();
		}
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		assert player != null;
		// Permission for the type of shopkeeper is checked in the AdminShopkeeper specific EditorHandler.
		// Owner is checked in the PlayerShopkeeper specific EditorHandler.
		return true;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		return view != null && view.getTitle().equals(this.getEditorTitle());
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
		// Dragging is allowed by default only inside the player inventory and the trades area:
		if (event.isCancelled()) return; // Already cancelled
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
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		Session session = this.getSession(player);
		assert session != null;

		int rawSlot = event.getRawSlot();
		if (this.isTradesArea(rawSlot)) {
			// Trades area:
			this.handleTradesClick(session, event);
		} else if (this.isTradesPageBar(rawSlot)) {
			// Trades page bar:
			this.handleTradesPageBarClick(session, event);
		} else if (this.isButtonArea(rawSlot)) {
			// Editor buttons:
			this.handleButtonClick(session, event);
		} else if (this.isPlayerInventory(event.getView(), event.getSlotType(), rawSlot)) {
			// Player inventory:
			this.handlePlayerInventoryClick(session, event);
		}
	}

	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		assert this.isTradesArea(event.getRawSlot());
	}

	protected void handleTradesPageBarClick(Session session, InventoryClickEvent event) {
		assert this.isTradesPageBar(event.getRawSlot());
		event.setCancelled(true);
		Player player = session.getPlayer();
		int rawSlot = event.getRawSlot();
		Button button = this._getTradesPageBarButton(rawSlot);
		if (button != null) {
			button.onClick(event, player);
		}
	}

	protected void handleButtonClick(Session session, InventoryClickEvent event) {
		assert this.isButtonArea(event.getRawSlot());
		event.setCancelled(true);
		Player player = session.getPlayer();
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
		// Validate bounds:
		if (minAmount > maxAmount) return currentAmount; // No valid value possible
		if (minAmount == maxAmount) return minAmount; // Only one valid value possible

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
		// Bounds:
		if (newAmount < minAmount) newAmount = minAmount;
		if (newAmount > maxAmount) newAmount = maxAmount;
		return newAmount;
	}

	@Override
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		// Cleanup session:
		Session session = sessions.remove(player.getUniqueId());

		if (closeEvent != null) {
			// Only save if caused by an inventory close event (i.e. if the session has not been 'aborted'):
			this.saveEditor(session);
		}
	}

	/**
	 * Saves the current page of the editor interface to the session.
	 * 
	 * @param session
	 *            the session
	 */
	protected void saveEditorPage(Session session) {
		assert session != null;
		Inventory inventory = session.getInventory();
		int page = session.getCurrentPage();
		assert page >= 1;
		List<TradingRecipeDraft> recipes = session.getRecipes();

		int recipesPerPage = COLUMNS_PER_ROW;
		int startIndex = (page - 1) * recipesPerPage;
		int endIndex = startIndex + TRADES_COLUMNS - 1;
		// Add empty recipes to support the recipes of the current page:
		for (int i = recipes.size(); i <= endIndex; ++i) {
			recipes.add(TradingRecipeDraft.EMPTY);
		}

		// Replace recipes:
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
		// Save the current editor page from the UI to the session:
		this.saveEditorPage(session);

		// Save the recipes from the session:
		this.saveRecipes(session);
	}

	/**
	 * Saves (i.e. applies) the trading recipes of the given session.
	 * 
	 * @param session
	 *            the session
	 */
	protected abstract void saveRecipes(Session session);
}
