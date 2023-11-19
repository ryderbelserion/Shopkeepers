package com.nisovin.shopkeepers.trading.commandtrading;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Helpers related to command trading: Items that cause the server to run a command when they are
 * traded, i.e. when they are either sold by or offered to a shopkeeper.
 * <p>
 * This feature is not meant to replace the requirement for custom third-party plugins or scripts in
 * order to implement complex or custom behaviors. There are a lot of things that a proper command
 * item plugin could support, but in order to reduce implementation and maintenance effort, this
 * feature is intentionally kept very simple and focuses on only providing the basic mechanism to
 * trigger a command during trading: Only a single command can be set and only a very limited set of
 * placeholders is supported:
 * <ul>
 * <li>{player_name}: Replaced with the name of the trading player.</li>
 * <li>{player_uuid}: Replaced with the unique id of the trading player.</li>
 * <li>{player_displayname}: Replaced with the display name of the trading player.</li>
 * <li>{shop_uuid}: Replaced with the unique id of the shopkeeper.</li>
 * </ul>
 * <p>
 * Any more advanced features are left to third-party plugins or scripts that can be invoked via
 * command. These limitations prevent the vast number of possible feature requests that could
 * otherwise arise and avoids this feature from evolving into a complex scripting engine over time.
 * <p>
 * For instance, if we would support for a list of commands to be specified, different users would
 * have different requirements as to how these commands should be executed. For example, we would
 * have to decide whether to run the commands sequentially or pick a random command instead, whether
 * to support time delays between commands, how to deal with failing commands in the sequence,
 * whether to only conditionally execute some of the commands, etc. Simple command sequences can
 * also be defined in Bukkit's "commands.yml" (see https://bukkit.fandom.com/wiki/Commands.yml).
 * <p>
 * Similarly, we only support a very limited set of placeholders, since the possibilities as to
 * which context information a user might want to use inside the command are basically endless. For
 * example, we don't provide placeholders for any shop owner or location information. A third-party
 * plugin can easily implement a command that looks up any other required shopkeeper information via
 * the provided unique id. And if any trade-specific information is required, such as the involved
 * trading recipe or items, a custom third-party plugin that listens for the
 * {@link ShopkeeperTradeEvent} might be better suited to implement the intended behavior.
 */
public final class CommandTradingUtils {

	/**
	 * The {@link NamespacedKey} by which we store the traded command of an item inside its
	 * persistent data.
	 */
	private static final NamespacedKey KEY_TRADED_COMMAND = NamespacedKeyUtils.create("shopkeepers", "traded_command");

	/**
	 * Sets the traded command of the given item stack.
	 * <p>
	 * This modifies the given item stack!
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code> or empty
	 * @param tradedCommand
	 *            the traded command, without any leading '/', or <code>null</code> or empty to
	 *            remove any traded command from the item stack
	 */
	public static void setTradedCommand(@ReadWrite ItemStack itemStack, @Nullable String tradedCommand) {
		Validate.isTrue(!ItemUtils.isEmpty(itemStack), "itemStack is empty");
		assert itemStack != null;

		ItemMeta meta = Unsafe.assertNonNull(itemStack.getItemMeta());
		PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
		if (StringUtils.isEmpty(tradedCommand)) {
			dataContainer.remove(KEY_TRADED_COMMAND);
		} else {
			dataContainer.set(
					KEY_TRADED_COMMAND,
					Unsafe.assertNonNull(PersistentDataType.STRING),
					Unsafe.assertNonNull(tradedCommand)
			);
		}
		itemStack.setItemMeta(meta);
	}

	/**
	 * Gets the traded command of the given item stack.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the traded command, or <code>null</code> if no traded command is set.
	 */
	public static @Nullable String getTradedCommand(@ReadOnly @Nullable ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return null;
		assert itemStack != null;

		ItemMeta meta = Unsafe.assertNonNull(itemStack.getItemMeta());
		PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
		@Nullable String tradedCommand = dataContainer.get(KEY_TRADED_COMMAND, Unsafe.assertNonNull(PersistentDataType.STRING));
		return StringUtils.getNotEmpty(tradedCommand);
	}

	/**
	 * Gets the traded command of the given item stack.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the traded command, or <code>null</code> if no traded command is set.
	 */
	public static @Nullable String getTradedCommand(@Nullable UnmodifiableItemStack itemStack) {
		return getTradedCommand(ItemUtils.asItemStackOrNull(itemStack));
	}
}
