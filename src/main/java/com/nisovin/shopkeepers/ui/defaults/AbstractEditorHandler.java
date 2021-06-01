package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.MathUtils;
import com.nisovin.shopkeepers.util.SoundEffect;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Base class for editor UIs which support editing multiple pages of trades and offer additional editor buttons.
 * <p>
 * For example used by {@link EditorHandler} and {@link VillagerEditorHandler}.
 */
public abstract class AbstractEditorHandler extends UIHandler {

	/**
	 * Different types of merchants differ in how they internally represent their offers. This
	 * {@link TradingRecipesAdapter} converts between the merchant's offers and the {@link TradingRecipeDraft}s that are
	 * visualized and edited in the editor.
	 * <p>
	 * Instead of mapping the merchant's offers one by one to {@link TradingRecipeDraft}s, {@link #getTradingRecipes()}
	 * returns the complete list of trading recipe drafts to initially show in the editor. This list may also contain
	 * partially setup but still invalid trading recipe drafts, for example to fill the editor with suggestions or
	 * examples for the editing player on possible trading recipes.
	 * <p>
	 * Once the player exists the editor, {@link #updateTradingRecipes(Player, List)} is invoked with the list of the
	 * potentially edited trading recipe drafts. This method is then responsible to update the merchant's offers based
	 * on these trading recipe drafts, and to report back whether and how many offers have changed.
	 */
	protected static interface TradingRecipesAdapter {

		/**
		 * Gets the list of {@link TradingRecipeDraft}s to show in the editor.
		 * <p>
		 * The returned list has to be modifiable: It is not copied, but edited directly.
		 * 
		 * @return the trading recipe drafts
		 */
		public List<TradingRecipeDraft> getTradingRecipes();

		/**
		 * Updates the merchant's offers based on the given {@link TradingRecipeDraft}s from the editor.
		 * <p>
		 * These trading recipe drafts may contain {@link TradingRecipeDraft#isValid() invalid} trading recipes.
		 * <p>
		 * Implementations of this method can assume that the given trading recipe drafts and their items are no longer
		 * used by the editor afterwards. Implementations are therefore allowed to reuse these objects.
		 * 
		 * @param player
		 *            the editing player, not <code>null</code>
		 * @param recipes
		 *            the trading recipe drafts from the editor, not <code>null</code>
		 * @return the (estimate) number of merchant offers that were changed, or <code>0</code> if no offers changed
		 */
		public int updateTradingRecipes(Player player, List<TradingRecipeDraft> recipes);
	}

	/**
	 * Default abstract skeleton implementation of a {@link TradingRecipesAdapter}.
	 * <p>
	 * By default, we reuse the merchant's previous offers and only replace those that changed. The previous offers and
	 * the new offers derived from the trading recipe drafts are matched by their index.
	 *
	 * @param <O>
	 *            the type of offer the merchant uses to represent its trading recipes
	 */
	protected static abstract class DefaultTradingRecipesAdapter<O> implements TradingRecipesAdapter {

		protected DefaultTradingRecipesAdapter() {
		}

		/**
		 * See {@link TradingRecipesAdapter#getTradingRecipes()}.
		 * <p>
		 * The order of these trading recipe drafts is expected to match the order of the corresponding offers returned
		 * by {@link #getOffers()}.
		 */
		@Override
		public abstract List<TradingRecipeDraft> getTradingRecipes();

		// TODO Maybe keep track of which TradingRecipeDrafts have actually been edited (i.e. clicked at in the editor
		// or marked as 'dirty' by some other mean), and then avoid creating offers and comparing items for them.
		// TODO When creating new offers, we can reuse the items of the trading recipe draft. Currently, these items are
		// copied most of the time.
		/**
		 * See {@link TradingRecipesAdapter#updateTradingRecipes(Player, List)}.
		 * <p>
		 * Any encountered {@link TradingRecipeDraft#isValid() invalid} trading recipe drafts are passed to
		 * {@link #handleInvalidTradingRecipe(Player, TradingRecipeDraft)} for further processing and are then ignored.
		 */
		@Override
		public int updateTradingRecipes(Player player, List<TradingRecipeDraft> recipes) {
			assert player != null && recipes != null && !recipes.contains(null);
			assert this.getOffers() != null && !this.getOffers().contains(null);
			List<O> newOffers = new ArrayList<>(this.getOffers());
			final int oldOffersSize = newOffers.size();
			int changedOffers = 0;
			boolean clearedAtLeastOneOffer = false;
			for (int index = 0; index < recipes.size(); index++) {
				TradingRecipeDraft recipe = recipes.get(index);
				// The recipe is also considered invalid if the created offer is null:
				O newOffer = recipe.isValid() ? this.createOffer(recipe) : null; // Null if invalid
				if (newOffer == null) {
					this.handleInvalidTradingRecipe(player, recipe);
					if (index < oldOffersSize) {
						newOffers.set(index, null); // Mark as cleared
						clearedAtLeastOneOffer = true;
						changedOffers += 1;
					}
				} else {
					if (index < oldOffersSize) {
						// Only replace the old offer if it has actually changed:
						O oldOffer = newOffers.get(index);
						if (!this.areOffersEqual(oldOffer, newOffer)) {
							newOffers.set(index, newOffer);
							changedOffers += 1;
						} // Else: Keep the old offer.
					} else {
						newOffers.add(newOffer);
						changedOffers += 1;
					}
				}
			}

			if (changedOffers > 0) {
				// Remove null markers:
				if (clearedAtLeastOneOffer) {
					newOffers.removeIf(Objects::isNull);
				}

				// Apply the new offers:
				this.setOffers(newOffers);
			}

			return changedOffers;
		}

		/**
		 * Gets the merchant's current offers.
		 * <p>
		 * The order of these offers is expected to match the order of the corresponding trading recipe drafts returned
		 * by {@link #getTradingRecipes()}.
		 * 
		 * @return the current offers
		 */
		protected abstract List<? extends O> getOffers();

		/**
		 * Updates the merchant's offers to the given list of offers.
		 * 
		 * @param newOffers
		 *            the new offers
		 */
		protected abstract void setOffers(List<O> newOffers);

		/**
		 * Creates a new offer for the given {@link TradingRecipeDraft}.
		 * <p>
		 * The given trading recipe draft is {@link TradingRecipeDraft#isValid() valid}. However, this method may
		 * implement additional verifications and return <code>null</code> if it considers the trading recipe draft to
		 * be invalid according to these checks: If this is the case, no offer is added to the merchant for the given
		 * trading recipe draft, any previous offer is removed, and
		 * {@link #handleInvalidTradingRecipe(Player, TradingRecipeDraft)} is invoked.
		 * 
		 * @param recipe
		 *            the trading recipe draft, not <code>null</code> or {@link TradingRecipeDraft#isValid() invalid}
		 * @return the offer, or <code>null</code> to consider the trading recipe draft as invalid
		 */
		protected abstract O createOffer(TradingRecipeDraft recipe);

		/**
		 * Checks whether the given old and new offers are considered equivalent.
		 * <p>
		 * This method is called when the new offers {@link #createOffer(TradingRecipeDraft) created} based on the
		 * trading recipe drafts from the editor are compared with the merchant's previous offers. This method serves
		 * two purposes:
		 * <ul>
		 * <li>It is used to detect whether the offers have changed.
		 * <li>It allows to decide which of these two offers shall be further used by the merchant: If the given offers
		 * are considered equal by this method, the previous offer is reused when building the list of new offers for
		 * the merchant. Otherwise, the new offer replaces the previous one.
		 * </ul>
		 * <p>
		 * By default, this method compares the offers based on {@link Object#equals(Object)}. However, this method may
		 * be overridden if there are cases in which the offers may not be fully equal, but equal enough that the old
		 * offer is meant to be reused nevertheless and not be replaced by the new offer. This situation can for example
		 * arise if the editor does not represent all aspects of the merchant's previous offers.
		 * 
		 * @param oldOffer
		 *            the previous offer, not <code>null</code>
		 * @param newOffer
		 *            the new offer derived from a trading recipe draft, not <code>null</code>
		 * @return <code>true</code> if the offers are considered equal and the old offer shall not be replaced
		 */
		protected boolean areOffersEqual(O oldOffer, O newOffer) {
			return oldOffer.equals(newOffer);
		}

		/**
		 * When the trading recipes from the editor are applied to the merchant, this is called for every
		 * {@link TradingRecipeDraft#isValid() invalid} trading recipe draft, as well as for every trading recipe draft
		 * that is considered invalid by {@link #createOffer(TradingRecipeDraft)}.
		 * 
		 * @param player
		 *            the editing player
		 * @param invalidRecipe
		 *            the invalid trading recipe draft
		 */
		protected void handleInvalidTradingRecipe(Player player, TradingRecipeDraft invalidRecipe) {
		}
	}

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
		inventory.setItem(column + RESULT_ITEM_OFFSET, recipe.getResultItem());
		inventory.setItem(column + ITEM_1_OFFSET, recipe.getItem1());
		inventory.setItem(column + ITEM_2_OFFSET, recipe.getItem2());
	}

	// TODO Avoid creating new TradingRecipeDraft objects here and instead update the drafts of the session?
	protected TradingRecipeDraft getTradingRecipe(Inventory inventory, int column) {
		ItemStack resultItem = inventory.getItem(column + RESULT_ITEM_OFFSET);
		ItemStack item1 = inventory.getItem(column + ITEM_1_OFFSET);
		ItemStack item2 = inventory.getItem(column + ITEM_2_OFFSET);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	// EDITOR BUTTONS

	public static abstract class Button {

		// Volume 0.25 matches Minecraft's default button click volume.
		protected static final SoundEffect DEFAULT_BUTTON_CLICK_SOUND = new SoundEffect(Sound.UI_BUTTON_CLICK).withVolume(0.25f);

		private static final int NO_SLOT = -1;

		private final boolean placeAtEnd;

		private AbstractEditorHandler editorHandler;
		private int slot = NO_SLOT;

		public Button() {
			this(false);
		}

		public Button(boolean placeAtEnd) {
			this.placeAtEnd = placeAtEnd;
		}

		private void setEditorHandler(AbstractEditorHandler editorHandler) {
			if (this.editorHandler != null) {
				throw new IllegalStateException("The button has already been added to some editor handler!");
			}
			this.editorHandler = editorHandler;
		}

		protected boolean isApplicable(AbstractEditorHandler editorHandler) {
			return true;
		}

		protected AbstractEditorHandler getEditorHandler() {
			return editorHandler;
		}

		public abstract ItemStack getIcon(Session session);

		// Updates the icon in all sessions.
		// Note: Cannot deal with changes to the registered buttons (the button's slot) while the inventory is open.
		protected final void updateIcon() {
			if (slot != NO_SLOT && editorHandler != null) {
				for (Session session : editorHandler.sessions.values()) {
					session.inventory.setItem(slot, this.getIcon(session));
					session.player.updateInventory();
				}
			}
		}

		// Updates all icons in all sessions.
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

	// A button for simple one-click actions.
	public static abstract class ActionButton extends Button {

		public ActionButton() {
			super();
		}

		public ActionButton(boolean placeAtEnd) {
			super(placeAtEnd);
		}

		protected void playButtonClickSound(Player player, boolean actionSuccess) {
			DEFAULT_BUTTON_CLICK_SOUND.play(player);
		}

		@Override
		protected final void onClick(InventoryClickEvent clickEvent, Player player) {
			if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // Ignore double clicks

			// Run action:
			boolean success = this.runAction(clickEvent, player);
			if (!success) return;

			// Post-processing:
			this.onActionSuccess(clickEvent, player);

			// Play sound:
			this.playButtonClickSound(player, success);

			// Icon might have changed:
			this.updateIcon();
		}

		// Returns true on success:
		protected abstract boolean runAction(InventoryClickEvent clickEvent, Player player);

		protected void onActionSuccess(InventoryClickEvent clickEvent, Player player) {
			// Nothing by default.
		}
	}

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
		prevPageButton.slot = TRADES_PAGE_BAR_START;
		tradesPageBarButtons[0] = prevPageButton;

		Button tradeSetupButton = this.createTradeSetupButton();
		tradeSetupButton.slot = TRADES_SETUP_ICON;
		tradesPageBarButtons[TRADES_SETUP_ICON - TRADES_PAGE_BAR_START] = tradeSetupButton;

		Button currentPageButton = this.createCurrentPageButton();
		currentPageButton.slot = TRADES_PAGE_ICON;
		tradesPageBarButtons[TRADES_PAGE_ICON - TRADES_PAGE_BAR_START] = currentPageButton;

		Button nextPageButton = this.createNextPageButton();
		nextPageButton.slot = TRADES_PAGE_BAR_END;
		tradesPageBarButtons[TRADES_PAGE_BAR_END - TRADES_PAGE_BAR_START] = nextPageButton;
	}

	protected Button createPrevPageButton() {
		return new ActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
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
				int newPage = Math.max(1, session.currentPage - 1);
				if (newPage == session.currentPage) return false; // Page has not changed

				// Update page:
				session.setPage(newPage);
				setupPage(player, session.currentPage);
				player.updateInventory();
				return true;
			}
		};
	}

	protected Button createNextPageButton() {
		return new ActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
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
				int newPage = Math.min(getMaxTradesPages(), session.currentPage + 1);
				if (newPage == session.currentPage) return false; // Page has not changed

				// Update page:
				session.setPage(newPage);
				setupPage(player, session.currentPage);
				player.updateInventory();
				return true;
			}
		};
	}

	protected Button createCurrentPageButton() {
		return new Button() {
			@Override
			public ItemStack getIcon(Session session) {
				int page = session.currentPage;
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
			button.slot = Button.NO_SLOT;
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
			if (button.placeAtEnd) {
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
	 * The editor state of a player.
	 */
	public static class Session {

		private final Player player;
		private final List<TradingRecipeDraft> recipes;
		private final Inventory inventory;
		private int currentPage = 1;

		protected Session(Player player, List<TradingRecipeDraft> recipes, Inventory inventory) {
			Validate.notNull(player, "player is null");
			Validate.notNull(recipes, "recipes is null");
			Validate.notNull(inventory, "inventory is null");
			this.player = player;
			this.recipes = recipes;
			this.inventory = inventory;
		}

		public final Player getPlayer() {
			return player;
		}

		// Starts at 1.
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
		player.openInventory(session.inventory);
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

		// Insert trades:
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
			inventory.setItem(button.slot, icon);
		}
	}

	// Note: This cannot deal with new button rows being required due to newly added buttons (which would require
	// creating and freshly open a new inventory, resulting in flicker).
	protected void updateButtons(Session session) {
		this.setupButtons(session);
	}

	// Also used to refresh all button icons in an already open inventory.
	protected void setupButtons(Session session) {
		Inventory inventory = session.inventory;
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
		Inventory inventory = session.inventory;
		int page = session.currentPage;
		assert page >= 1;
		List<TradingRecipeDraft> recipes = session.recipes;

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
