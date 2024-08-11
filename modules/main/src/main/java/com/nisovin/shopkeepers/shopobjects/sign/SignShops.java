package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.HashMap;

import org.bukkit.block.Sign;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.SignUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * Helpers related to sign and hanging sign shops.
 */
final class SignShops {

	// Reused array to pass sign lines:
	private static final @NonNull String[] TEMP_SIGN_LINES = new @NonNull String[4];

	static void updateShopSign(Sign sign, Shopkeeper shopkeeper) {
		// Sign content:
		if (shopkeeper instanceof PlayerShopkeeper) {
			updatePlayerShopSign(sign, (PlayerShopkeeper) shopkeeper);
		} else {
			assert shopkeeper instanceof AdminShopkeeper;
			updateAdminShopSign(sign, (AdminShopkeeper) shopkeeper);
		}
	}

	private static void updatePlayerShopSign(Sign sign, PlayerShopkeeper shop) {
		ShopObject shopObject = shop.getShopObject();

		var arguments = new HashMap<String, Object>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(shopObject.prepareName(shop.getName())));
		arguments.put("owner", shop.getOwnerName());  // Not null, can be empty

		TEMP_SIGN_LINES[0] = StringUtils.replaceArguments(Messages.playerSignShopLine1, arguments);
		TEMP_SIGN_LINES[1] = StringUtils.replaceArguments(Messages.playerSignShopLine2, arguments);
		TEMP_SIGN_LINES[2] = StringUtils.replaceArguments(Messages.playerSignShopLine3, arguments);
		TEMP_SIGN_LINES[3] = StringUtils.replaceArguments(Messages.playerSignShopLine4, arguments);
		SignUtils.setBothSidesText(sign, TEMP_SIGN_LINES);
		// Array will be reset with the next use.
	}

	private static void updateAdminShopSign(Sign sign, AdminShopkeeper shop) {
		ShopObject shopObject = shop.getShopObject();

		var arguments = new HashMap<String, Object>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(shopObject.prepareName(shop.getName())));

		TEMP_SIGN_LINES[0] = StringUtils.replaceArguments(Messages.adminSignShopLine1, arguments);
		TEMP_SIGN_LINES[1] = StringUtils.replaceArguments(Messages.adminSignShopLine2, arguments);
		TEMP_SIGN_LINES[2] = StringUtils.replaceArguments(Messages.adminSignShopLine3, arguments);
		TEMP_SIGN_LINES[3] = StringUtils.replaceArguments(Messages.adminSignShopLine4, arguments);
		SignUtils.setBothSidesText(sign, TEMP_SIGN_LINES);
		// Array will be reset with the next use.
	}

	private SignShops() {
	}
}
