package com.nisovin.shopkeepers.util.data.property.validation.bukkit;

import org.bukkit.Material;

import com.nisovin.shopkeepers.util.data.property.validation.PropertyValidator;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link PropertyValidator}s for {@link Material} values.
 */
public final class MaterialValidators {

	/**
	 * A {@link PropertyValidator} that ensures that the validated {@link Material} is not
	 * {@link Material#AIR}.
	 */
	public static final PropertyValidator<Material> NOT_AIR = (value) -> {
		Validate.isTrue(value != Material.AIR, "Material cannot be AIR!");
	};

	/**
	 * A {@link PropertyValidator} that ensures that the validated {@link Material} is an
	 * {@link Material#isItem() item}.
	 */
	public static final PropertyValidator<Material> IS_ITEM = (value) -> {
		// Note: AIR is a valid item type. It is for example used for empty slots in inventories.
		Validate.isTrue(value.isItem(), "Material is not an item: " + value);
	};

	/**
	 * A {@link PropertyValidator} that ensures that the validated {@link Material} is not a
	 * {@link Material#isLegacy() legacy} material.
	 */
	public static final PropertyValidator<Material> NON_LEGACY = (value) -> {
		Validate.isTrue(!value.isLegacy(), "Unsupported legacy material: " + value);
	};

	private MaterialValidators() {
	}
}
