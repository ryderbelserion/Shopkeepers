package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.java.ObjectUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link CommandArgument} that returns the targeted entity without consuming any arguments.
 * <p>
 * If the sender is not a player, a 'requires a player' error message is thrown. If no entity is
 * targeted or the targeted entity is not accepted, the filter's corresponding error message is
 * used.
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
				return Messages.mustTargetEntity;
			}

			@Override
			public Text getInvalidTargetErrorMsg(Entity entity) {
				return Text.EMPTY; // Not used
			}
		};

		public abstract Text getNoTargetErrorMsg();

		public abstract Text getInvalidTargetErrorMsg(Entity entity);
	}

	private final TargetEntityFilter filter; // Not null

	public TargetEntityArgument(String name) {
		this(name, TargetEntityFilter.ANY);
	}

	public TargetEntityArgument(String name, TargetEntityFilter filter) {
		super(name);
		Validate.notNull(filter, "filter is null");
		this.filter = filter;
	}

	@Override
	public boolean isOptional() {
		return true; // Does not require user input
	}

	@Override
	public Entity parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		Player player = ObjectUtils.castOrNull(input.getSender(), Player.class);
		if (player == null) {
			throw this.requiresPlayerError();
		}

		Entity targetedEntity = EntityUtils.getTargetedEntity(player);
		if (targetedEntity == null) {
			throw new ArgumentParseException(this, filter.getNoTargetErrorMsg());
		} else if (!filter.test(targetedEntity)) {
			throw new ArgumentParseException(this, filter.getInvalidTargetErrorMsg(targetedEntity));
		}
		return targetedEntity;
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return Collections.emptyList();
	}
}
