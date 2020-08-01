package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.EntityUtils;

/**
 * A {@link CommandArgument} that returns the targeted entity without consuming any arguments.
 * <p>
 * If the sender is not a player, a 'requires a player' error message is thrown. If no entity is targeted or the
 * targeted entity is not accepted, the filter's corresponding error message is used.
 */
public class TargetEntityArgument extends CommandArgument<Entity> {

	public interface TargetEntityFilter extends Predicate<Entity> {

		public static final TargetEntityFilter ANY = new TargetEntityFilter() {
			@Override
			public boolean test(Entity entity) {
				return true;
			}

			@Override
			public Text getNoTargetErrorMsg() {
				return Settings.msgMustTargetEntity;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Entity entity) {
				return Text.EMPTY; // Not used
			}
		};

		public abstract Text getNoTargetErrorMsg();

		public abstract Text getInvalidTargetErrorMsg(Entity entity);
	}

	private final TargetEntityFilter filter; // not null

	public TargetEntityArgument(String name) {
		this(name, TargetEntityFilter.ANY);
	}

	public TargetEntityArgument(String name, TargetEntityFilter filter) {
		super(name);
		this.filter = (filter == null) ? TargetEntityFilter.ANY : filter;
	}

	@Override
	public boolean isOptional() {
		return true; // Does not require user input
	}

	@Override
	public Entity parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		CommandSender sender = input.getSender();
		if (!(sender instanceof Player)) {
			throw this.requiresPlayerError();
		}

		Player player = (Player) sender;
		Entity targetedEntity = EntityUtils.getTargetedEntity(player);
		if (targetedEntity == null) {
			throw new ArgumentParseException(this, filter.getNoTargetErrorMsg());
		} else if (!filter.test(targetedEntity)) {
			throw new ArgumentParseException(this, filter.getInvalidTargetErrorMsg(targetedEntity));
		}
		return targetedEntity;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return Collections.emptyList();
	}
}
