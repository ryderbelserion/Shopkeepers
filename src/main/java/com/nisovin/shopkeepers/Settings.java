package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

public class Settings {

	public static final class DebugOptions {

		// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get combined into a
		// single logging entry to slightly reduce spam.
		public static final String logAllEvents = "log-all-events";
		// Prints the registered listeners for the first call of each event.
		public static final String printListeners = "print-listeners";
	}

	/*
	 * General Settings
	 */
	public static int configVersion = 2;
	public static boolean debug = false;
	// See DebugOptions for all available options.
	public static List<String> debugOptions = new ArrayList<>(0);
	public static boolean enableMetrics = true;

	/*
	 * Shopkeeper Data
	 */
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	/*
	 * Plugin Compatibility
	 */
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(Material.VILLAGER_SPAWN_EGG);
	public static boolean preventShopCreationItemRegularUsage = false;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireChestRecentlyPlaced = true;
	public static int maxChestDistance = 15;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";

	public static boolean protectChests = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakChest = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	public static List<String> enabledLivingShops = Arrays.asList(
			EntityType.VILLAGER.name(),
			EntityType.COW.name(),
			EntityType.MUSHROOM_COW.name(),
			EntityType.SHEEP.name(),
			EntityType.PIG.name(),
			EntityType.CHICKEN.name(),
			EntityType.OCELOT.name(),
			EntityType.RABBIT.name(),
			EntityType.WOLF.name(),
			EntityType.SNOWMAN.name(),
			EntityType.IRON_GOLEM.name(),
			EntityType.BLAZE.name(),
			EntityType.SILVERFISH.name(),
			EntityType.POLAR_BEAR.name(), // MC 1.10
			EntityType.SKELETON.name(),
			EntityType.STRAY.name(), // MC 1.11
			EntityType.WITHER_SKELETON.name(), // MC 1.11
			EntityType.SPIDER.name(),
			EntityType.CAVE_SPIDER.name(),
			EntityType.CREEPER.name(),
			EntityType.WITCH.name(),
			EntityType.ENDERMAN.name(),
			EntityType.ZOMBIE.name(),
			EntityType.ZOMBIE_VILLAGER.name(), // MC 1.11
			EntityType.PIG_ZOMBIE.name(),
			EntityType.HUSK.name(), // MC 1.11
			EntityType.GIANT.name(),
			EntityType.GHAST.name(),
			EntityType.SLIME.name(),
			EntityType.MAGMA_CUBE.name(),
			EntityType.SQUID.name(),
			EntityType.HORSE.name(),
			EntityType.MULE.name(),
			EntityType.DONKEY.name(),
			EntityType.SKELETON_HORSE.name(),
			EntityType.ZOMBIE_HORSE.name(),
			EntityType.EVOKER.name(), // MC 1.11
			EntityType.VEX.name(), // MC 1.11
			EntityType.VINDICATOR.name(), // MC 1.11
			EntityType.ILLUSIONER.name(), // MC 1.12
			EntityType.PARROT.name(), // MC 1.12
			EntityType.TURTLE.name(), // MC 1.13
			EntityType.PHANTOM.name(), // MC 1.13
			EntityType.COD.name(), // MC 1.13
			EntityType.SALMON.name(), // MC 1.13
			EntityType.PUFFERFISH.name(), // MC 1.13
			EntityType.TROPICAL_FISH.name(), // MC 1.13
			EntityType.DROWNED.name(), // MC 1.13
			EntityType.DOLPHIN.name(), // MC 1.13
			EntityType.CAT.name(), // MC 1.14
			EntityType.PANDA.name(), // MC 1.14
			EntityType.PILLAGER.name(), // MC 1.14
			EntityType.RAVAGER.name(), // MC 1.14
			EntityType.LLAMA.name(), // MC 1.11
			EntityType.TRADER_LLAMA.name(), // MC 1.14
			EntityType.WANDERING_TRADER.name(), // MC 1.14
			EntityType.FOX.name() // MC 1.14
	);

	public static boolean useLegacyMobBehavior = false;
	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;
	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";

	public static boolean enableCitizenShops = false;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;
	public static String signShopFirstLine = "[SHOP]";

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static String editorTitle = "Shopkeeper Editor";

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);

	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableChestOptionOnPlayerShop = true;
	public static ItemData chestItem = new ItemData(Material.CHEST);

	public static ItemData deleteItem = new ItemData(Material.BONE);

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean disableZombieVillagerCuring = false;
	public static boolean hireOtherVillagers = false;

	public static boolean disableWanderingTraders = false;
	public static boolean blockWanderingTraderSpawns = false;
	public static boolean hireWanderingTraders = false;

	/*
	 * Hiring
	 */
	public static ItemData hireItem = new ItemData(Material.EMERALD);
	public static int hireOtherVillagersCosts = 1;
	public static String forHireTitle = "For Hire";
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean incrementVillagerStatistics = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData zeroCurrencyItem = new ItemData(Material.BARRIER);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);
	public static ItemData zeroHighCurrencyItem = new ItemData(Material.BARRIER);

	// note: this can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	/*
	 * Messages
	 */
	public static String language = "en";

	public static String msgShopTypeAdminRegular = "Admin shop";
	public static String msgShopTypeSelling = "Selling shop";
	public static String msgShopTypeBuying = "Buying shop";
	public static String msgShopTypeTrading = "Trading shop";
	public static String msgShopTypeBook = "Book shop";

	public static String msgShopTypeDescAdminRegular = "has unlimited stock";
	public static String msgShopTypeDescSelling = "sells items to players";
	public static String msgShopTypeDescBuying = "buys items from players";
	public static String msgShopTypeDescTrading = "trades items with players";
	public static String msgShopTypeDescBook = "sells book copies";

	public static String msgShopObjectTypeLiving = "{type}";
	public static String msgShopObjectTypeSign = "sign";
	public static String msgShopObjectTypeNpc = "npc";

	public static String msgSelectedShopType = "&aSelected shop type: &6{type} &7({description})";
	public static String msgSelectedShopObjectType = "&aSelected object type: &6{type}";

	public static String msgCreationItemSelected = "&aShop creation:\n"
			+ "&e  Left/Right-click to select the shop type.\n"
			+ "&e  Sneak + left/right-click to select the object type.\n"
			+ "&e  Right-click a chest to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.";

	public static String msgButtonPreviousPage = "&6<- Previous page ({prev_page} of {max_page})";
	public static List<String> msgButtonPreviousPageLore = Arrays.asList();
	public static String msgButtonNextPage = "&6Next page ({next_page} of {max_page}) ->";
	public static List<String> msgButtonNextPageLore = Arrays.asList();
	public static String msgButtonCurrentPage = "&6Page {page} of {max_page}";
	public static List<String> msgButtonCurrentPageLore = Arrays.asList();

	public static String msgButtonName = "&aSet shop name";
	public static List<String> msgButtonNameLore = Arrays.asList("Lets you rename", "your shopkeeper");
	public static String msgButtonChest = "&aView chest inventory";
	public static List<String> msgButtonChestLore = Arrays.asList("Lets you view the inventory", " your shopkeeper is using");
	public static String msgButtonDelete = "&4Delete";
	public static List<String> msgButtonDeleteLore = Arrays.asList("Closes and removes", "this shopkeeper");
	public static String msgButtonHire = "&aHire";
	public static List<String> msgButtonHireLore = Arrays.asList("Buy this shop");

	public static String msgButtonSignVariant = "&aChoose sign variant";
	public static List<String> msgButtonSignVariantLore = Arrays.asList("Changes the sign's", "wood type");
	public static String msgButtonBaby = "&aToggle baby variant";
	public static List<String> msgButtonBabyLore = Arrays.asList("Toggles between the mob's", "baby and adult variant");
	public static String msgButtonSitting = "&aToggle sitting pose";
	public static List<String> msgButtonSittingLore = Arrays.asList("Toggles the mob's", "sitting pose");
	public static String msgButtonCatVariant = "&aChoose cat variant";
	public static List<String> msgButtonCatVariantLore = Arrays.asList("Changes the cat's look");
	public static String msgButtonCollarColor = "&aChoose collar color";
	public static List<String> msgButtonCollarColorLore = Arrays.asList("Changes the mob's", "collar color");
	public static String msgButtonWolfAngry = "&aToggle angry wolf";
	public static List<String> msgButtonWolfAngryLore = Arrays.asList("Toggles the wolf's", "angry state");
	public static String msgButtonCarryingChest = "&aToggle carrying chest";
	public static List<String> msgButtonCarryingChestLore = Arrays.asList("Toggles whether the mob", "is carrying a chest");
	public static String msgButtonHorseColor = "&aChoose horse color";
	public static List<String> msgButtonHorseColorLore = Arrays.asList("Changes the color", "of the horse");
	public static String msgButtonHorseStyle = "&aChoose horse style";
	public static List<String> msgButtonHorseStyleLore = Arrays.asList("Changes the coat pattern", "of the horse");
	public static String msgButtonHorseArmor = "&aChoose horse armor";
	public static List<String> msgButtonHorseArmorLore = Arrays.asList("Changes the armor", "of the horse");
	public static String msgButtonLlamaVariant = "&aChoose llama variant";
	public static List<String> msgButtonLlamaVariantLore = Arrays.asList("Changes the llama's look");
	public static String msgButtonLlamaCarpetColor = "&aLlama carpet color";
	public static List<String> msgButtonLlamaCarpetColorLore = Arrays.asList("Changes the llama's", "carpet color");
	public static String msgButtonCreeperCharged = "&aToggle charged creeper";
	public static List<String> msgButtonCreeperChargedLore = Arrays.asList("Toggles the creeper's", "charged state");
	public static String msgButtonFoxVariant = "&aChoose fox variant";
	public static List<String> msgButtonFoxVariantLore = Arrays.asList("Changes the fox's look");
	public static String msgButtonFoxCrouching = "&aToggle crouching pose";
	public static List<String> msgButtonFoxCrouchingLore = Arrays.asList("Toggles the fox's", "crouching pose");
	public static String msgButtonFoxSleeping = "&aToggle sleeping pose";
	public static List<String> msgButtonFoxSleepingLore = Arrays.asList("Toggles the fox's", "sleeping pose");
	public static String msgButtonMooshroomVariant = "&aChoose mooshroom variant";
	public static List<String> msgButtonMooshroomVariantLore = Arrays.asList("Changes the look", "of the mooshroom");
	public static String msgButtonPandaVariant = "&aChoose panda variant";
	public static List<String> msgButtonPandaVariantLore = Arrays.asList("Changes the panda's look");
	public static String msgButtonParrotVariant = "&aChoose parrot variant";
	public static List<String> msgButtonParrotVariantLore = Arrays.asList("Changes the parrot's look");
	public static String msgButtonPigSaddle = "&aToggle pig saddle";
	public static List<String> msgButtonPigSaddleLore = Arrays.asList("Toggles the pig's saddle");
	public static String msgButtonSheepColor = "&aChoose sheep color";
	public static List<String> msgButtonSheepColorLore = Arrays.asList("Changes the sheep's", "wool color");
	public static String msgButtonSheepSheared = "&aToggle sheared sheep";
	public static List<String> msgButtonSheepShearedLore = Arrays.asList("Toggles the sheep's", "sheared state");
	public static String msgButtonVillagerProfession = "&aChoose villager profession";
	public static List<String> msgButtonVillagerProfessionLore = Arrays.asList("Changes the profession", "of the villager");
	public static String msgButtonVillagerVariant = "&aChoose villager variant";
	public static List<String> msgButtonVillagerVariantLore = Arrays.asList("Changes the look", "of the villager");
	public static String msgButtonVillagerLevel = "&aChoose villager level";
	public static List<String> msgButtonVillagerLevelLore = Arrays.asList("Changes the level", "of the villager");
	public static String msgButtonZombieVillagerProfession = "&aChoose villager profession";
	public static List<String> msgButtonZombieVillagerProfessionLore = Arrays.asList("Changes the profession", "of the zombie villager");

	public static String msgTradingTitlePrefix = "&2";
	public static String msgTradingTitleDefault = "Shopkeeper";

	public static String msgSelectedChest = "&aChest selected! Right-click a block to place your shopkeeper.";
	public static String msgMustSelectChest = "&7You must right-click a chest before placing your shopkeeper.";
	public static String msgNoChestSelected = "&7The selected block is not a chest!";
	public static String msgChestTooFar = "&7The shopkeeper's chest is too far away!";
	public static String msgChestNotPlaced = "&7You must select a chest you have recently placed!";
	public static String msgChestAlreadyInUse = "&7Another shopkeeper is already using the selected chest!";
	public static String msgNoChestAccess = "&7You cannot access the selected chest!";
	public static String msgTooManyShops = "&7You have too many shops!";
	public static String msgNoAdminShopTypeSelected = "&7You have to select an admin shop type!";
	public static String msgNoPlayerShopTypeSelected = "&7You have to select a player shop type!";
	public static String msgShopCreateFail = "&7You cannot create a shopkeeper there.";

	public static String msgTypeNewName = "&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.";
	public static String msgNameSet = "&aThe shop's name has been set!";
	public static String msgNameHasNotChanged = "&aThe shop's name has not changed.";
	public static String msgNameInvalid = "&aThat name is not valid!";

	public static String msgShopTypeDisabled = "&7The shop type '&6{type}&7' is disabled.";
	public static String msgShopObjectTypeDisabled = "&7The shop object type '&6{type}&7' is disabled.";

	public static String msgMustTargetShop = "&7You have to target a shopkeeper.";
	public static String msgMustTargetAdminShop = "&7You have to target an admin shopkeeper.";
	public static String msgMustTargetPlayerShop = "&7You have to target a player shopkeeper.";
	public static String msgTargetEntityIsNoShop = "&7The targeted entity is no shopkeeper.";
	public static String msgTargetShopIsNoAdminShop = "&7The targeted shopkeeper is no admin shopkeeper.";
	public static String msgTargetShopIsNoPlayerShop = "&7The targeted shopkeeper is no player shopkeeper.";
	public static String msgUnusedChest = "&7No shopkeeper is using this chest.";
	public static String msgNotOwner = "&7You are not the owner of this shopkeeper.";
	// placeholders: {owner} -> new owners name
	public static String msgOwnerSet = "&aNew owner was set to &e{owner}";
	public static String msgShopCreationItemsGiven = "&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!";
	public static String msgUnknownBookAuthor = "Unknown";

	public static String msgTradePermSet = "&aThe shop's trading permission has been set to '&e{perm}&a'!";
	public static String msgTradePermRemoved = "&aThe shop's trading permission '&e{perm}&a' has been removed!";
	public static String msgTradePermView = "&aThe shop's current trading permission is '&e{perm}&a'.";

	public static String msgZombieVillagerCuringDisabled = "&7Curing of zombie villagers is disabled.";
	public static String msgMustHoldHireItem = "&7You have to hold the required hire item in your hand.";
	public static String msgSetForHire = "&aThe Shopkeeper was set for hire.";
	public static String msgHired = "&aYou have hired this shopkeeper!";
	public static String msgMissingHirePerm = "&7You do not have the permission to hire shopkeepers.";
	public static String msgCantHire = "&aYou cannot afford to hire this shopkeeper.";
	public static String msgCantHireShopType = "&7You do not have the permission to hire this type of shopkeeper.";
	// placeholders: {costs}, {hire-item}
	public static String msgVillagerForHire = "&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.";

	public static String msgMissingTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgMissingCustomTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgCantTradeWhileOwnerOnline = "&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.";

	public static String msgShopkeeperCreated = "&aShopkeeper created: &6{type} &7({description})\n{setupDesc}";

	public static String msgShopSetupDescSelling = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBuying = "&e  Add one of each item you want to buy to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescTrading = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBook = "&e  Add written books and blank books to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescAdminRegular = "&e  Right-click the shop while sneaking to modify trades.";

	public static String msgTradeSetupDescHeader = "&6{shopType}";
	public static List<String> msgTradeSetupDescAdminRegular = Arrays.asList("Has unlimited stock.", "Insert items from your inventory.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescSelling = Arrays.asList("Sells items.", "Insert items to sell into the chest.", "Left/Right click to adjust amounts.", "Top row: Items being sold", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescBuying = Arrays.asList("Buys items.", "Insert one of each item you want to", "buy and plenty of currency items", "into the chest.", "Left/Right click to adjust amounts.", "Top row: Cost items", "Bottom row: Items being bought");
	public static List<String> msgTradeSetupDescTrading = Arrays.asList("Trades items.", "Pickup an item from your inventory", "and then click a slot to place it.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> msgTradeSetupDescBook = Arrays.asList("Sells book copies.", "Insert written and blank books", "into the chest.", "Left/Right click to adjust costs.", "Top row: Books being sold", "Bottom rows: Cost items");

	public static String msgListAdminShopsHeader = "&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})";
	public static String msgListPlayerShopsHeader = "&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})";
	public static String msgListShopsEntry = "  &e{shopIndex}) &8{shopName}&r&7at &8({location})&7, type: &8{shopType}&7, object type: &8{objectType}";

	public static String msgRemovedAdminShops = "&e{shopsCount} &aadmin shops were removed.";
	public static String msgRemovedPlayerShops = "&e{shopsCount} &ashops of player '&e{player}&a' were removed.";
	public static String msgRemovedAllPlayerShops = "&aAll &e{shopsCount} &aplayer shops were removed.";

	public static String msgConfirmRemoveAdminShops = "&cYou are about to irrevocable remove all admin shops!";
	public static String msgConfirmRemoveOwnShops = "&cYou are about to irrevocable remove all your shops!";
	public static String msgConfirmRemovePlayerShops = "&cYou are about to irrevocable remove all shops of player &6{player}&c!";
	public static String msgConfirmRemoveAllPlayerShops = "&cYou are about to irrevocable remove all player shops of all players!";

	public static String msgConfirmationRequired = "&7Please confirm this action by typing &6/shopkeepers confirm";
	public static String msgConfirmationExpired = "&cConfirmation expired.";
	public static String msgNothingToConfirm = "&cThere is nothing to confirm currently.";

	public static String msgNoPermission = "&cYou don't have the permission to do that.";

	public static String msgCommandUnknown = "&cUnknown command '&e{command}&c'!";
	public static String msgCommandArgumentUnexpected = "&cUnexpected argument '&e{argument}&c'.";
	public static String msgCommandArgumentRequiresPlayer = "&cArgument '&e{argumentFormat}&c' requires a player to execute the command.";
	public static String msgCommandArgumentMissing = "&cMissing argument '&e{argumentFormat}&c'.";
	public static String msgCommandArgumentInvalid = "&cInvalid argument '&e{argument}&c'.";
	public static String msgCommandPlayerArgumentMissing = "&cNo player specified for '&e{argumentFormat}&c'.";
	public static String msgCommandPlayerArgumentInvalid = "&cNo player found for '&e{argument}&c'.";
	public static String msgCommandShopTypeArgumentInvalid = "&cUnknown shop type '&e{argument}&c'.";
	public static String msgCommandShopObjectTypeArgumentInvalid = "&cUnknown shop object type '&e{argument}&c'.";
	public static String msgCommandShopkeeperArgumentInvalid = "&cNo shopkeeper found for '&e{argument}&c'.";
	public static String msgCommandShopkeeperArgumentNoAdminShop = "&cShopkeeper '&e{argument}&c' is no admin shopkeeper.";
	public static String msgCommandShopkeeperArgumentNoPlayerShop = "&cShopkeeper '&e{argument}&c' is no player shopkeeper.";

	public static String msgCommandHelpTitle = "&9***** &8[&6Shopkeepers v{version}&8] &9*****";
	public static String msgCommandHelpUsageFormat = "&e{usage}";
	public static String msgCommandHelpDescriptionFormat = "&8 - &3{description}";

	public static String msgCommandDescriptionShopkeeper = "Creates a shopkeeper.";
	public static String msgCommandDescriptionHelp = "Shows this help page.";
	public static String msgCommandDescriptionReload = "Reloads this plugin.";
	public static String msgCommandDescriptionDebug = "Toggles debug mode on and off.";
	public static String msgCommandDescriptionList = "Lists all shops for the specified player, or all admin shops.";
	public static String msgCommandDescriptionRemove = "Removes all shops for the specified player, all players, or all admin shops.";
	public static String msgCommandDescriptionGive = "Gives shop creation item(s) to the specified player.";
	public static String msgCommandDescriptionRemote = "Remotely opens a shop.";
	public static String msgCommandDescriptionRemoteEdit = "Remotely edits a shop.";
	public static String msgCommandDescriptionTransfer = "Transfers the ownership of a shop.";
	public static String msgCommandDescriptionSettradeperm = "Sets, removes (-) or displays (?) the trading permission.";
	public static String msgCommandDescriptionSetforhire = "Sets one of your shops for sale.";

	// /////

	private static String toConfigKey(String fieldName) {
		return fieldName.replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
	}

	// returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) {
		boolean configChanged = false;

		// perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(config);
		if (migrated) {
			configChanged = true;
		}

		// exempt a few string / string list settings from color conversion:
		List<String> noColorConversionKeys = Arrays.asList(
				toConfigKey("fileEncoding"), toConfigKey("shopCreationItemSpawnEggEntityType"),
				toConfigKey("maxShopsPermOptions"), toConfigKey("enabledLivingShops"),
				toConfigKey("nameRegex"), toConfigKey("language"));
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.isSynthetic()) continue;
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());

				// initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					Log.warning("Config: Inserting default value for missing config entry: " + configKey);

					// determine default value:
					Configuration defaults = config.getDefaults();
					Object defaultValue = loadConfigValue(defaults, configKey, noColorConversionKeys, typeClass, genericType);

					// validate default value:
					if (defaultValue == null) {
						Log.warning("Config: Missing default value for missing config entry: " + configKey);
						continue;
					} else if (!Utils.isAssignableFrom(typeClass, defaultValue.getClass())) {
						Log.warning("Config: Default value for missing config entry '" + configKey + "' is of wrong type: "
								+ "Got " + defaultValue.getClass().getName() + ", expecting " + typeClass.getName());
						continue;
					}

					// set default value:
					setConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType, defaultValue);
					configChanged = true;
				}

				// load value:
				Object value = loadConfigValue(config, configKey, noColorConversionKeys, typeClass, genericType);
				field.set(null, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// validation:

		boolean foundInvalidEntityType = false;
		for (String entityTypeId : enabledLivingShops) {
			EntityType entityType = matchEntityType(entityTypeId);
			if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
				foundInvalidEntityType = true;
				Log.warning("Config: Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
			}
		}
		if (foundInvalidEntityType) {
			Log.warning("Config: All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
		}

		if (maxChestDistance > 50) {
			Log.warning("Config: 'max-chest-distance' can be at most 50.");
			maxChestDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning("Config: 'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		// certain items cannot be of type AIR:
		if (shopCreationItem.getType() == Material.AIR) {
			Log.warning("Config: 'shop-creation-item' can not be AIR.");
			shopCreationItem = shopCreationItem.withType(Material.VILLAGER_SPAWN_EGG);
		}
		if (hireItem.getType() == Material.AIR) {
			Log.warning("Config: 'hire-item' can not be AIR.");
			hireItem = hireItem.withType(Material.EMERALD);
		}
		if (currencyItem.getType() == Material.AIR) {
			Log.warning("Config: 'currency-item' can not be AIR.");
			currencyItem = currencyItem.withType(Material.EMERALD);
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem.getType() == Material.AIR) {
				Log.warning("Config: 'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
		}
		if (taxRate < 0) {
			Log.warning("Config: 'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning("Config: 'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}

		// prepare derived settings:
		DerivedSettings.setup();

		return configChanged;
	}

	private static Object loadConfigValue(Configuration config, String configKey, List<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType) {
		if (typeClass == String.class) {
			String string = config.getString(configKey);
			// colorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				string = TextUtils.colorize(string);
			}
			return string;
		} else if (typeClass == int.class) {
			return config.getInt(configKey);
		} else if (typeClass == short.class) {
			return (short) config.getInt(configKey);
		} else if (typeClass == boolean.class) {
			return config.getBoolean(configKey);
		} else if (typeClass == Material.class) {
			// this assumes that legacy item conversion has already been performed
			Material material = ConfigUtils.loadMaterial(config, configKey);
			if (material == null) {
				Log.warning("Config: Unknown material for config entry '" + configKey + "': " + config.get(configKey));
				Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
			}
			return material;
		} else if (typeClass == ItemData.class) {
			ItemData itemData = ItemData.deserialize(config.get(configKey), (warning) -> {
				Log.warning("Config: Couldn't load item data for config entry '" + configKey + "': " + warning);
				if (warning.contains("Unknown item type")) { // TODO this is ugly
					Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
				}
			});
			// normalize to not null:
			if (itemData == null) {
				itemData = new ItemData(Material.AIR);
			}
			return itemData;
		} else if (typeClass == List.class) {
			if (genericType == String.class) {
				List<String> stringList = config.getStringList(configKey);
				// colorize, if not exempted:
				if (!noColorConversionKeys.contains(configKey)) {
					stringList = TextUtils.colorize(stringList);
				}
				return stringList;
			} else {
				return null; // not supported currently
			}
		}
		return null;
	}

	private static void setConfigValue(Configuration config, String configKey, List<String> noColorConversionKeys, Class<?> typeClass, Class<?> genericType, Object value) {
		if (value == null) {
			config.set(configKey, value); // removes value
			return;
		}

		if (typeClass == Material.class) {
			config.set(configKey, ((Material) value).name());
		} else if (typeClass == String.class) {
			// decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize((String) value);
			}
			config.set(configKey, value);
		} else if (typeClass == List.class && genericType == String.class) {
			// decolorize, if not exempted:
			if (!noColorConversionKeys.contains(configKey)) {
				value = TextUtils.decolorize(ConversionUtils.toStringList((List<?>) value));
			}
			config.set(configKey, value);
		} else if (typeClass == ItemData.class) {
			config.set(configKey, ((ItemData) value).serialize());
		} else {
			config.set(configKey, value);
		}
	}

	public static void loadLanguageConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (!field.getName().startsWith("msg")) continue;
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());
				if (!config.isSet(configKey)) {
					continue; // skip, keeps default
				}

				Object value = loadConfigValue(config, configKey, Collections.emptyList(), typeClass, genericType);
				if (value == null) {
					continue; // skip, keeps default
				}
				field.set(null, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// item utilities:

	// stores derived settings that get setup after loading the config
	public static class DerivedSettings {

		public static ItemData namingItemData = new ItemData(Material.AIR);

		// button items:
		public static ItemData nameButtonItem = new ItemData(Material.AIR);
		public static ItemData chestButtonItem = new ItemData(Material.AIR);
		public static ItemData deleteButtonItem = new ItemData(Material.AIR);
		public static ItemData hireButtonItem = new ItemData(Material.AIR);

		// gets called after the config has been loaded:
		private static void setup() {
			// ignore display name (which is used for specifying the new shopkeeper name):
			namingItemData = new ItemData(ItemUtils.setItemStackName(nameItem.createItemStack(), null));

			// button items:
			nameButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(nameItem.createItemStack(), msgButtonName, msgButtonNameLore));
			chestButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(chestItem.createItemStack(), msgButtonChest, msgButtonChestLore));
			deleteButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), msgButtonDelete, msgButtonDeleteLore));
			hireButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(hireItem.createItemStack(), msgButtonHire, msgButtonHireLore));
		}
	}

	// creation item:
	public static ItemStack createShopCreationItem() {
		return shopCreationItem.createItemStack();
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return shopCreationItem.matches(item);
	}

	// naming item:
	public static boolean isNamingItem(ItemStack item) {
		return DerivedSettings.namingItemData.matches(item);
	}

	public static ItemStack createNameButtonItem() {
		return DerivedSettings.nameButtonItem.createItemStack();
	}

	// chest button:
	public static ItemStack createChestButtonItem() {
		return DerivedSettings.chestButtonItem.createItemStack();
	}

	// delete button:
	public static ItemStack createDeleteButtonItem() {
		return DerivedSettings.deleteButtonItem.createItemStack();
	}

	// hire item:
	public static ItemStack createHireButtonItem() {
		return DerivedSettings.hireButtonItem.createItemStack();
	}

	public static boolean isHireItem(ItemStack item) {
		return hireItem.matches(item);
	}

	// CURRENCY

	// currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return currencyItem.createItemStack(amount);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return currencyItem.matches(item);
	}

	// high currency item:
	public static boolean isHighCurrencyEnabled() {
		return (highCurrencyValue > 0 && highCurrencyItem.getType() != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return highCurrencyItem.createItemStack(amount);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return highCurrencyItem.matches(item);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (zeroCurrencyItem.getType() == Material.AIR) return null;
		return zeroCurrencyItem.createItemStack();
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (zeroCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroCurrencyItem.matches(item);
	}

	// high zero currency item:
	public static ItemStack createZeroHighCurrencyItem() {
		if (zeroHighCurrencyItem.getType() == Material.AIR) return null;
		return zeroHighCurrencyItem.createItemStack();
	}

	public static boolean isZeroHighCurrencyItem(ItemStack item) {
		if (zeroHighCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroHighCurrencyItem.matches(item);
	}

	//

	public static int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (PermissionUtils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	public static EntityType matchEntityType(String entityTypeId) {
		if (StringUtils.isEmpty(entityTypeId)) return null;
		// get by bukkit id:
		String normalizedEntityTypeId = entityTypeId.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		try {
			return EntityType.valueOf(normalizedEntityTypeId);
		} catch (IllegalArgumentException e) {
			// unknown entity type:
			return null;
		}
	}
}
