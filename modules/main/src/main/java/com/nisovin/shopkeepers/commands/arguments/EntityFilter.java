package com.nisovin.shopkeepers.commands.arguments;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.TargetEntityArgument.TargetEntityFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopobjects.ShopkeeperMetadata;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

public final class EntityFilter {

	public static final ArgumentFilter<Entity> ANY = ArgumentFilter.acceptAny();

	public static final ArgumentFilter<@Nullable Entity> VILLAGER = new ArgumentFilter<@Nullable Entity>() {
		@Override
		public boolean test(
				CommandInput input,
				CommandContextView context,
				@Nullable Entity value
		) {
			return isRegularVillager(value);
		}

		@Override
		public Text getInvalidArgumentErrorMsg(
				CommandArgument<?> argument,
				String argumentInput,
				@Nullable Entity value
		) {
			Validate.notNull(argumentInput, "argumentInput is null");
			Text text = Messages.commandEntityArgumentNoVillager;
			text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
			text.setPlaceholderArguments("argument", argumentInput);
			return text;
		}
	};

	public static final TargetEntityFilter VILLAGER_TARGET = new TargetEntityFilter() {
		@Override
		public boolean test(Entity entity) {
			return isRegularVillager(entity);
		}

		@Override
		public Text getNoTargetErrorMsg() {
			return Messages.mustTargetVillager;
		}

		@Override
		public Text getInvalidTargetErrorMsg(Entity entity) {
			return Messages.targetEntityIsNoVillager;
		}
	};

	public static boolean isRegularVillager(@Nullable Entity entity) {
		if (!(entity instanceof AbstractVillager)) {
			return false; // No villager or wandering trader.
		}
		if (ShopkeeperMetadata.isTagged(entity) || CitizensUtils.isNPC(entity)) {
			return false; // Shopkeeper or Citizens NPC.
		}
		return true;
	}

	private EntityFilter() {
	}
}
