package com.nisovin.shopkeepers;

import static com.nisovin.shopkeepers.config.ConfigHelper.loadConfigValue;
import static com.nisovin.shopkeepers.config.ConfigHelper.toConfigKey;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.shopkeepers.config.ConfigLoadException;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.Log;

public class Messages {

	// TODO Replace all with Text? Will require converting back to String, especially for texts used by items.
	public static String shopTypeAdminRegular = "Admin shop";
	public static String shopTypeSelling = "Selling shop";
	public static String shopTypeBuying = "Buying shop";
	public static String shopTypeTrading = "Trading shop";
	public static String shopTypeBook = "Book shop";

	public static String shopTypeDescAdminRegular = "has unlimited stock";
	public static String shopTypeDescSelling = "sells items to players";
	public static String shopTypeDescBuying = "buys items from players";
	public static String shopTypeDescTrading = "trades items with players";
	public static String shopTypeDescBook = "sells book copies";

	public static String shopObjectTypeLiving = "{type}";
	public static String shopObjectTypeSign = "sign";
	public static String shopObjectTypeNpc = "npc";

	public static Text selectedShopType = Text.parse("&aSelected shop type: &6{type} &7({description})");
	public static Text selectedShopObjectType = Text.parse("&aSelected object type: &6{type}");

	public static Text creationItemSelected = Text.parse("&aShop creation:\n"
			+ "&e  Left/Right-click to select the shop type.\n"
			+ "&e  Sneak + left/right-click to select the object type.\n"
			+ "&e  Right-click a container to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.");

	public static String buttonPreviousPage = "&6<- Previous page ({prev_page} of {max_page})";
	public static List<String> buttonPreviousPageLore = Arrays.asList();
	public static String buttonNextPage = "&6Next page ({next_page} of {max_page}) ->";
	public static List<String> buttonNextPageLore = Arrays.asList();
	public static String buttonCurrentPage = "&6Page {page} of {max_page}";
	public static List<String> buttonCurrentPageLore = Arrays.asList();

	public static String buttonName = "&aSet shop name";
	public static List<String> buttonNameLore = Arrays.asList("Lets you rename", "your shopkeeper");
	public static String buttonContainer = "&aView shop inventory";
	public static List<String> buttonContainerLore = Arrays.asList("Lets you view the inventory", " your shopkeeper is using");
	public static String buttonDelete = "&4Delete";
	public static List<String> buttonDeleteLore = Arrays.asList("Closes and removes", "this shopkeeper");
	public static String buttonHire = "&aHire";
	public static List<String> buttonHireLore = Arrays.asList("Buy this shop");

	public static String buttonSignVariant = "&aChoose sign variant";
	public static List<String> buttonSignVariantLore = Arrays.asList("Changes the sign's", "wood type");
	public static String buttonBaby = "&aToggle baby variant";
	public static List<String> buttonBabyLore = Arrays.asList("Toggles between the mob's", "baby and adult variant");
	public static String buttonSitting = "&aToggle sitting pose";
	public static List<String> buttonSittingLore = Arrays.asList("Toggles the mob's", "sitting pose");
	public static String buttonCatVariant = "&aChoose cat variant";
	public static List<String> buttonCatVariantLore = Arrays.asList("Changes the cat's look");
	public static String buttonRabbitVariant = "&aChoose rabbit variant";
	public static List<String> buttonRabbitVariantLore = Arrays.asList("Changes the rabbit's look");
	public static String buttonCollarColor = "&aChoose collar color";
	public static List<String> buttonCollarColorLore = Arrays.asList("Changes the mob's", "collar color");
	public static String buttonWolfAngry = "&aToggle angry wolf";
	public static List<String> buttonWolfAngryLore = Arrays.asList("Toggles the wolf's", "angry state");
	public static String buttonCarryingChest = "&aToggle carrying chest";
	public static List<String> buttonCarryingChestLore = Arrays.asList("Toggles whether the mob", "is carrying a chest");
	public static String buttonHorseColor = "&aChoose horse color";
	public static List<String> buttonHorseColorLore = Arrays.asList("Changes the color", "of the horse");
	public static String buttonHorseStyle = "&aChoose horse style";
	public static List<String> buttonHorseStyleLore = Arrays.asList("Changes the coat pattern", "of the horse");
	public static String buttonHorseArmor = "&aChoose horse armor";
	public static List<String> buttonHorseArmorLore = Arrays.asList("Changes the armor", "of the horse");
	public static String buttonLlamaVariant = "&aChoose llama variant";
	public static List<String> buttonLlamaVariantLore = Arrays.asList("Changes the llama's look");
	public static String buttonLlamaCarpetColor = "&aLlama carpet color";
	public static List<String> buttonLlamaCarpetColorLore = Arrays.asList("Changes the llama's", "carpet color");
	public static String buttonCreeperCharged = "&aToggle charged creeper";
	public static List<String> buttonCreeperChargedLore = Arrays.asList("Toggles the creeper's", "charged state");
	public static String buttonFoxVariant = "&aChoose fox variant";
	public static List<String> buttonFoxVariantLore = Arrays.asList("Changes the fox's look");
	public static String buttonFoxCrouching = "&aToggle crouching pose";
	public static List<String> buttonFoxCrouchingLore = Arrays.asList("Toggles the fox's", "crouching pose");
	public static String buttonFoxSleeping = "&aToggle sleeping pose";
	public static List<String> buttonFoxSleepingLore = Arrays.asList("Toggles the fox's", "sleeping pose");
	public static String buttonMooshroomVariant = "&aChoose mooshroom variant";
	public static List<String> buttonMooshroomVariantLore = Arrays.asList("Changes the look", "of the mooshroom");
	public static String buttonPandaVariant = "&aChoose panda variant";
	public static List<String> buttonPandaVariantLore = Arrays.asList("Changes the panda's look");
	public static String buttonParrotVariant = "&aChoose parrot variant";
	public static List<String> buttonParrotVariantLore = Arrays.asList("Changes the parrot's look");
	public static String buttonPigSaddle = "&aToggle pig saddle";
	public static List<String> buttonPigSaddleLore = Arrays.asList("Toggles the pig's saddle");
	public static String buttonSheepColor = "&aChoose sheep color";
	public static List<String> buttonSheepColorLore = Arrays.asList("Changes the sheep's", "wool color");
	public static String buttonSheepSheared = "&aToggle sheared sheep";
	public static List<String> buttonSheepShearedLore = Arrays.asList("Toggles the sheep's", "sheared state");
	public static String buttonVillagerProfession = "&aChoose villager profession";
	public static List<String> buttonVillagerProfessionLore = Arrays.asList("Changes the profession", "of the villager");
	public static String buttonVillagerVariant = "&aChoose villager variant";
	public static List<String> buttonVillagerVariantLore = Arrays.asList("Changes the look", "of the villager");
	public static String buttonVillagerLevel = "&aChoose villager badge color";
	public static List<String> buttonVillagerLevelLore = Arrays.asList("Changes the badge color", "of the villager");
	public static String buttonZombieVillagerProfession = "&aChoose villager profession";
	public static List<String> buttonZombieVillagerProfessionLore = Arrays.asList("Changes the profession", "of the zombie villager");
	public static String buttonSlimeSize = "&aChoose slime size";
	public static List<String> buttonSlimeSizeLore = Arrays.asList("Cycles the slime's size.", "Current size: &e{size}");
	public static String buttonMagmaCubeSize = "&aChoose magma cube size";
	public static List<String> buttonMagmaCubeSizeLore = Arrays.asList("Cycles the magma cube's size.", "Current size: &e{size}");

	public static String tradingTitlePrefix = "&2";
	public static String tradingTitleDefault = "Shopkeeper";

	public static Text containerSelected = Text.parse("&aContainer selected! Right-click a block to place your shopkeeper.");
	public static Text unsupportedContainer = Text.parse("&7This type of container cannot be used for shops.");
	public static Text mustSelectContainer = Text.parse("&7You must right-click a container before placing your shopkeeper.");
	public static Text invalidContainer = Text.parse("&7The selected block is not a valid container!");
	public static Text containerTooFarAway = Text.parse("&7The shopkeeper's container is too far away!");
	public static Text containerNotPlaced = Text.parse("&7You must select a container you have recently placed!");
	public static Text containerAlreadyInUse = Text.parse("&7Another shopkeeper is already using the selected container!");
	public static Text noContainerAccess = Text.parse("&7You cannot access the selected container!");
	public static Text tooManyShops = Text.parse("&7You have too many shops!");
	public static Text noAdminShopTypeSelected = Text.parse("&7You have to select an admin shop type!");
	public static Text noPlayerShopTypeSelected = Text.parse("&7You have to select a player shop type!");
	public static Text shopCreateFail = Text.parse("&7You cannot create a shopkeeper there.");

	public static Text typeNewName = Text.parse("&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.");
	public static Text nameSet = Text.parse("&aThe shop's name has been set!");
	public static Text nameHasNotChanged = Text.parse("&aThe shop's name has not changed.");
	public static Text nameInvalid = Text.parse("&aThat name is not valid!");

	public static Text shopTypeDisabled = Text.parse("&7The shop type '&6{type}&7' is disabled.");
	public static Text shopObjectTypeDisabled = Text.parse("&7The shop object type '&6{type}&7' is disabled.");

	public static Text mustTargetShop = Text.parse("&7You have to target a shopkeeper.");
	public static Text mustTargetAdminShop = Text.parse("&7You have to target an admin shopkeeper.");
	public static Text mustTargetPlayerShop = Text.parse("&7You have to target a player shopkeeper.");
	public static Text targetEntityIsNoShop = Text.parse("&7The targeted entity is no shopkeeper.");
	public static Text targetShopIsNoAdminShop = Text.parse("&7The targeted shopkeeper is no admin shopkeeper.");
	public static Text targetShopIsNoPlayerShop = Text.parse("&7The targeted shopkeeper is no player shopkeeper.");
	public static Text unusedContainer = Text.parse("&7No shopkeeper is using this container.");
	public static Text notOwner = Text.parse("&7You are not the owner of this shopkeeper.");
	// Placeholders: {owner} -> new owners name
	public static Text ownerSet = Text.parse("&aNew owner was set to &e{owner}");
	public static Text shopCreationItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!");
	public static Text shopCreationItemsReceived = Text.parse("&aYou have received &e{amount}&a shop creation item(s)!");
	public static Text currencyItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a currency item(s)!");
	public static Text currencyItemsReceived = Text.parse("&aYou have received &e{amount}&a currency item(s)!");
	public static Text highCurrencyItemsGiven = Text.parse("&aPlayer &e{player}&a has received &e{amount}&a high currency item(s)!");
	public static Text highCurrencyItemsReceived = Text.parse("&aYou have received &e{amount}&a high currency item(s)!");
	public static Text highCurrencyDisabled = Text.parse("&cThe high currency is disabled!");
	public static Text itemsConverted = Text.parse("&aConverted &e{count}&a item stack(s)!");
	public static String unknownBookAuthor = "Unknown";

	public static Text tradePermSet = Text.parse("&aThe shop's trading permission has been set to '&e{perm}&a'!");
	public static Text tradePermRemoved = Text.parse("&aThe shop's trading permission '&e{perm}&a' has been removed!");
	public static Text tradePermView = Text.parse("&aThe shop's current trading permission is '&e{perm}&a'.");

	public static Text zombieVillagerCuringDisabled = Text.parse("&7Curing of zombie villagers is disabled.");
	public static Text mustHoldHireItem = Text.parse("&7You have to hold the required hire item in your hand.");
	public static Text setForHire = Text.parse("&aThe Shopkeeper was set for hire.");
	public static Text hired = Text.parse("&aYou have hired this shopkeeper!");
	public static Text missingHirePerm = Text.parse("&7You do not have the permission to hire shopkeepers.");
	public static Text cantHire = Text.parse("&aYou cannot afford to hire this shopkeeper.");
	public static Text cantHireShopType = Text.parse("&7You do not have the permission to hire this type of shopkeeper.");
	// Placeholders: {costs}, {hire-item}
	public static Text villagerForHire = Text.parse("&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.");

	public static Text missingTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text missingCustomTradePerm = Text.parse("&7You do not have the permission to trade with this shop.");
	public static Text cantTradeWithOwnShop = Text.parse("&7You cannot trade with your own shop.");
	public static Text cantTradeWhileOwnerOnline = Text.parse("&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.");
	public static Text cantTradeWithShopMissingContainer = Text.parse("&7You cannot trade with this shop, because its container is missing.");

	public static Text shopkeeperCreated = Text.parse("&aShopkeeper created: &6{type} &7({description})\n{setupDesc}");

	public static String shopSetupDescSelling = "&e  Add items you want to sell to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String shopSetupDescBuying = "&e  Add one of each item you want to buy to your container,\n"
			+ "&e  then right-click the shop while sneaking to modify costs.";
	public static String shopSetupDescTrading = "&e  Add items you want to sell to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String shopSetupDescBook = "&e  Add written books and blank books to your container, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String shopSetupDescAdminRegular = "&e  Right-click the shop while sneaking to modify trades.";

	public static String tradeSetupDescHeader = "&6{shopType}";
	public static List<String> tradeSetupDescAdminRegular = Arrays.asList("Has unlimited stock.", "Insert items from your inventory.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> tradeSetupDescSelling = Arrays.asList("Sells items.", "Insert items to sell into the container.", "Left/Right click to adjust amounts.", "Top row: Items being sold", "Bottom rows: Cost items");
	public static List<String> tradeSetupDescBuying = Arrays.asList("Buys items.", "Insert one of each item you want to", "buy and plenty of currency items", "into the container.", "Left/Right click to adjust amounts.", "Top row: Cost items", "Bottom row: Items being bought");
	public static List<String> tradeSetupDescTrading = Arrays.asList("Trades items.", "Pickup an item from your inventory", "and then click a slot to place it.", "Left/Right click to adjust amounts.", "Top row: Result items", "Bottom rows: Cost items");
	public static List<String> tradeSetupDescBook = Arrays.asList("Sells book copies.", "Insert written and blank books", "into the container.", "Left/Right click to adjust costs.", "Top row: Books being sold", "Bottom rows: Cost items");

	public static Text missingEditVillagersPerm = Text.parse("&7You do not have the permission to edit villagers.");
	public static Text missingEditWanderingTradersPerm = Text.parse("&7You do not have the permission to edit wandering traders.");
	public static Text mustTargetEntity = Text.parse("&7You have to target an entity.");
	public static Text mustTargetVillager = Text.parse("&7You have to target a villager.");
	public static Text targetEntityIsNoVillager = Text.parse("&7The targeted entity is no regular villager.");

	public static String villagerEditorTitle = "&aVillager Editor: &e{villagerName}";
	public static String villagerEditorDescriptionHeader = "&6Villager Editor";
	public static List<String> villagerEditorDescription = Arrays.asList(
			"Top row: Result items",
			"Bottom rows: Cost items",
			"Edited trades have infinite",
			"uses and no XP rewards."
	);

	public static String buttonDeleteVillager = "&4Delete";
	public static List<String> buttonDeleteVillagerLore = Arrays.asList("Deletes the villager");
	public static String buttonVillagerInventory = "&aView villager inventory";
	public static List<String> buttonVillagerInventoryLore = Arrays.asList(
			"Lets you view a copy of",
			"the villager's inventory"
	);
	public static String buttonMobAi = "&aToggle mob AI";
	public static List<String> buttonMobAiLore = Arrays.asList("Toggles the mob's AI");

	public static String villagerInventoryTitle = "Villager inventory (copy)";
	public static String setVillagerXp = "&aSet the villager's XP to &e{xp}";
	public static String noVillagerTradesChanged = "&aNo trades have been changed.";
	public static String villagerTradesChanged = "&e{changedTrades}&a trades have been changed.";

	public static Text listAdminShopsHeader = Text.parse("&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})");
	public static Text listAllShopsHeader = Text.parse("&9There are &e{shopsCount} &9shops in total: &e(Page {page} of {maxPage})");
	public static Text listPlayerShopsHeader = Text.parse("&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})");
	public static Text listShopsEntry = Text.parse("  &e{shopId}) &7{shopName}&r&8at &7({location})&8, type: &7{shopType}&8, object: &7{objectType}");

	public static Text removedAdminShops = Text.parse("&e{shopsCount} &aadmin shops were removed.");
	public static Text removedShopsOfPlayer = Text.parse("&e{shopsCount} &ashops of player '&e{player}&a' were removed.");
	public static Text removedPlayerShops = Text.parse("&e{shopsCount} &aplayer shops were removed.");

	public static Text confirmRemoveAllAdminShops = Text.parse("&cYou are about to irrevocable remove all admin shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllOwnShops = Text.parse("&cYou are about to irrevocable remove all your shops (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllShopsOfPlayer = Text.parse("&cYou are about to irrevocable remove all shops of player &6{player}&c (&6{shopsCount}&c)!");
	public static Text confirmRemoveAllPlayerShops = Text.parse("&cYou are about to irrevocable remove all player shops of all players (&6{shopsCount}&c)!");

	public static Text confirmationRequired = Text.parse("&7Please confirm this action by typing &6/shopkeepers confirm");
	public static Text confirmationExpired = Text.parse("&cConfirmation expired.");
	public static Text nothingToConfirm = Text.parse("&cThere is nothing to confirm currently.");

	public static Text noPermission = Text.parse("&cYou don't have the permission to do that.");

	public static Text commandUnknown = Text.parse("&cUnknown command '&e{command}&c'!");
	public static Text commandArgumentUnexpected = Text.parse("&cUnexpected argument '&e{argument}&c'.");
	public static Text commandArgumentRequiresPlayer = Text.parse("&cArgument '&e{argumentFormat}&c' requires a player to execute the command.");
	public static Text commandArgumentMissing = Text.parse("&cMissing argument '&e{argumentFormat}&c'.");
	public static Text commandArgumentInvalid = Text.parse("&cInvalid argument '&e{argument}&c'.");
	public static Text commandPlayerArgumentMissing = Text.parse("&cNo player specified for '&e{argumentFormat}&c'.");
	public static Text commandPlayerArgumentInvalid = Text.parse("&cNo player found for '&e{argument}&c'.");
	public static Text commandShopTypeArgumentInvalid = Text.parse("&cUnknown shop type '&e{argument}&c'.");
	public static Text commandShopObjectTypeArgumentInvalid = Text.parse("&cUnknown shop object type '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentInvalid = Text.parse("&cNo shopkeeper found for '&e{argument}&c'.");
	public static Text commandShopkeeperArgumentNoAdminShop = Text.parse("&cShopkeeper '&e{argument}&c' is no admin shopkeeper.");
	public static Text commandShopkeeperArgumentNoPlayerShop = Text.parse("&cShopkeeper '&e{argument}&c' is no player shopkeeper.");

	public static Text ambiguousPlayerName = Text.parse("&cThere are multiple matches for the name '&e{name}&c'!");
	public static Text ambiguousPlayerNameEntry = Text.parse("&c  - '&e{name}&r&c' (&6{uuid}&c)");
	public static Text ambiguousPlayerNameMore = Text.parse("&c  ....");

	public static Text commandHelpTitle = Text.parse("&9***** &8[&6Shopkeepers v{version}&8] &9*****");
	public static Text commandHelpUsageFormat = Text.parse("&e{usage}");
	public static Text commandHelpDescriptionFormat = Text.parse("&8 - &3{description}");

	public static Text commandDescriptionShopkeeper = Text.parse("Creates a shopkeeper.");
	public static Text commandDescriptionHelp = Text.parse("Shows this help page.");
	public static Text commandDescriptionReload = Text.parse("Reloads this plugin.");
	public static Text commandDescriptionDebug = Text.parse("Toggles debug mode on and off.");
	public static Text commandDescriptionList = Text.parse("Lists all shops for the specified player, or all admin shops.");
	public static Text commandDescriptionRemove = Text.parse("Removes all shops for the specified player, all players, or all admin shops.");
	public static Text commandDescriptionGive = Text.parse("Gives shop creation item(s) to the specified player.");
	public static Text commandDescriptionGiveCurrency = Text.parse("Gives currency item(s) to the specified player.");
	public static Text commandDescriptionConvertItems = Text.parse("Converts the held (or all) items to conform to Spigot's data format.");
	public static Text commandDescriptionRemote = Text.parse("Remotely opens a shop.");
	public static Text commandDescriptionRemoteEdit = Text.parse("Remotely edits a shop.");
	public static Text commandDescriptionTransfer = Text.parse("Transfers the ownership of a shop.");
	public static Text commandDescriptionSettradeperm = Text.parse("Sets, removes (-) or displays (?) the trading permission.");
	public static Text commandDescriptionSetforhire = Text.parse("Sets one of your shops for sale.");
	public static Text commandDescriptionEditVillager = Text.parse("Opens the editor for the target villager.");

	private static final String DEFAULT_LANGUAGE = "en-default";

	private static String getLanguageFileName(String language) {
		return "language-" + language + ".yml";
	}

	// Relative to the plugin's data folder:
	private static String getLanguageFilePath(String language) {
		return SKShopkeepersPlugin.LANG_FOLDER + "/" + getLanguageFileName(language);
	}

	private static File getLanguageFile(String language) {
		String languageFilePath = getLanguageFilePath(language);
		return new File(SKShopkeepersPlugin.getInstance().getDataFolder(), languageFilePath);
	}

	/**
	 * The default language file is freshly written on every startup and overwrites the already existing default
	 * language file.
	 */
	private static void saveDefaultLanguageFile() {
		String languageFilePath = getLanguageFilePath(DEFAULT_LANGUAGE);
		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();
		assert plugin.getResource(languageFilePath) != null;
		plugin.saveResource(languageFilePath, true);
	}

	public static void loadLanguageFile() {
		// Create default language file:
		saveDefaultLanguageFile();

		SKShopkeepersPlugin plugin = SKShopkeepersPlugin.getInstance();
		String language = Settings.language;

		// Create language file if it is missing and there exists a default:
		String languageFilePath = getLanguageFilePath(language);
		File languageFile = getLanguageFile(language);
		if (!languageFile.exists() && plugin.getResource(languageFilePath) != null) {
			plugin.saveResource(languageFilePath, false);
		}

		// Load language config:
		if (!languageFile.exists()) {
			Log.warning("Could not find language file '" + languageFile.getName() + "'!");
		} else {
			Log.info("Loading language file: " + languageFile.getName());
			try {
				YamlConfiguration langConfig = new YamlConfiguration();
				langConfig.load(languageFile);
				loadLanguageConfiguration(langConfig);
			} catch (Exception e) {
				Log.warning("Could not load language file '" + languageFile.getName() + "'!", e);
			}
		}
	}

	private static void loadLanguageConfiguration(Configuration config) throws ConfigLoadException {
		Set<String> messageKeys = new HashSet<>();
		try {
			Field[] fields = Messages.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.isSynthetic()) continue;
				if (!Modifier.isPublic(field.getModifiers())) {
					continue;
				}
				Class<?> typeClass = field.getType();
				Class<?> genericType = null;
				if (typeClass == List.class) {
					genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				String configKey = toConfigKey(field.getName());
				messageKeys.add(configKey);
				if (!config.isSet(configKey)) {
					Log.warning("  Missing message: " + configKey);
					continue; // Skip, keeps current value (default)
				}

				Object value = loadConfigValue(config, configKey, Collections.emptySet(), typeClass, genericType);
				if (value == null) {
					Log.warning("  Could not load message: " + configKey);
					continue; // Skip, keeps current value (default)
				}
				field.set(null, value);
			}
		} catch (Exception e) {
			throw new ConfigLoadException("Error while loading messages from language file!", e);
		}

		Set<String> configKeys = config.getKeys(false);
		if (configKeys.size() != messageKeys.size()) {
			for (String configKey : configKeys) {
				if (!messageKeys.contains(configKey)) {
					Log.warning("  Unknown message: " + configKey);
				}
			}
		}

		Settings.onSettingsChanged();
	}

	private Messages() {
	}
}
