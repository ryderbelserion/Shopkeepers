package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FixedValuesArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

class CommandDebug extends Command {

	private static final class DebugOptionArgument extends FixedValuesArgument {

		private static Map<? extends String, ? extends String> getDebugOptionsMap() {
			Map<String, String> debugOptionsMap = new LinkedHashMap<>();
			DebugOptions.getAll().forEach(debugOption -> {
				debugOptionsMap.put(debugOption, debugOption);
			});
			return debugOptionsMap;
		}

		private static final Text invalidArgumentMsg = Text.parse(
				"&cUnknown debug option '{argument}'. Available options: "
						+ String.join(", ", DebugOptions.getAll())
		);

		public DebugOptionArgument(String name) {
			super(name, getDebugOptionsMap());
		}

		@Override
		protected Text getInvalidArgumentErrorMsgText() {
			return invalidArgumentMsg;
		}
	}

	private static final String ARGUMENT_DEBUG_OPTION = "option";

	CommandDebug() {
		super("debug");

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Messages.commandDescriptionDebug);

		// Arguments:
		this.addArgument(new DebugOptionArgument(ARGUMENT_DEBUG_OPTION).optional());
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		String debugOption = context.getOrNull(ARGUMENT_DEBUG_OPTION);
		if (debugOption == null) {
			// Toggle debug mode:
			Settings.debug = !Settings.debug;
			Settings.onSettingsChanged();
			sender.sendMessage(ChatColor.GREEN + "Debug mode "
					+ (Settings.debug ? "enabled" : "disabled"));
		} else {
			assert DebugOptions.getAll().contains(debugOption); // Validated by the argument
			// Toggle debug option:
			boolean enabled;
			if (Settings.debugOptions.contains(debugOption)) {
				Settings.debugOptions.remove(debugOption);
				enabled = false;
			} else {
				Settings.debugOptions.add(debugOption);
				enabled = true;
			}
			Settings.onSettingsChanged();
			sender.sendMessage(ChatColor.GREEN + "Debug option '" + debugOption + "' "
					+ (enabled ? "enabled" : "disabled"));
		}
	}
}
