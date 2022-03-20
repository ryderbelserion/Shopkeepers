package com.nisovin.shopkeepers.commands.lib.arguments;

import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityArgument.TargetEntityFilter;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that returns the targeted entity without consuming any arguments.
 * <p>
 * If the sender is not a player, the parsing exception of the original argument is thrown (the
 * original argument might get reevaluated is some parsing context has changed).
 */
public class TargetEntityFallback extends TypedFallbackArgument<@NonNull Entity> {

	public TargetEntityFallback(CommandArgument<@NonNull Entity> argument) {
		this(argument, TargetEntityFilter.ANY);
	}

	public TargetEntityFallback(
			CommandArgument<@NonNull Entity> argument,
			TargetEntityFilter filter
	) {
		super(
				Validate.notNull(argument, "argument is null"),
				new TargetEntityArgument(argument.getName(), filter)
		);
	}
}
