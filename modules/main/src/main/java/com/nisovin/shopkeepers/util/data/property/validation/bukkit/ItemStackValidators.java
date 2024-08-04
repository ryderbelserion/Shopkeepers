package com.nisovin.shopkeepers.util.data.property.validation.bukkit;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.data.property.validation.PropertyValidator;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link PropertyValidator}s for {@link ItemStack} values.
 */
public final class ItemStackValidators {

	/**
	 * Default {@link PropertyValidator}s for {@link UnmodifiableItemStack} values.
	 */
	public static final class Unmodifiable {

		/**
		 * A {@link PropertyValidator} that ensures that the validated {@link UnmodifiableItemStack}
		 * is not {@link ItemUtils#isEmpty(UnmodifiableItemStack) empty}.
		 */
		public static final PropertyValidator<UnmodifiableItemStack> NON_EMPTY = (value) -> {
			Validate.isTrue(!ItemUtils.isEmpty(value), "Item stack is empty!");
		};

		private Unmodifiable() {
		}
	}

	/**
	 * A {@link PropertyValidator} that ensures that the validated {@link ItemStack} is not
	 * {@link ItemUtils#isEmpty(ItemStack) empty}.
	 */
	public static final PropertyValidator<ItemStack> NON_EMPTY = (value) -> {
		Validate.isTrue(!ItemUtils.isEmpty(value), "Item stack is empty!");
	};

	private ItemStackValidators() {
	}
}
