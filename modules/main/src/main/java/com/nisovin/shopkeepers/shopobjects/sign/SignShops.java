package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.block.Sign;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * Helpers related to sign and hanging sign shops.
 */
final class SignShops {

	private SignShops() {
	}

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

		Map<@NonNull String, @NonNull Object> arguments = new HashMap<>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(shopObject.prepareName(shop.getName())));
		arguments.put("owner", shop.getOwnerName());  // Not null, can be empty

		sign.setLine(0, StringUtils.replaceArguments(Messages.playerSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.playerSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.playerSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.playerSignShopLine4, arguments));

		// MC 1.20: Apply the text to both sign sides:
		NMSManager.getProvider().setSignBackLines(sign, Unsafe.castNonNull(sign.getLines()));
	}

	private static void updateAdminShopSign(Sign sign, AdminShopkeeper shop) {
		ShopObject shopObject = shop.getShopObject();

		Map<@NonNull String, @NonNull Object> arguments = new HashMap<>();
		// Not null, can be empty:
		arguments.put("shopName", Unsafe.assertNonNull(shopObject.prepareName(shop.getName())));

		sign.setLine(0, StringUtils.replaceArguments(Messages.adminSignShopLine1, arguments));
		sign.setLine(1, StringUtils.replaceArguments(Messages.adminSignShopLine2, arguments));
		sign.setLine(2, StringUtils.replaceArguments(Messages.adminSignShopLine3, arguments));
		sign.setLine(3, StringUtils.replaceArguments(Messages.adminSignShopLine4, arguments));

		// MC 1.20: Apply the text to both sign sides:
		NMSManager.getProvider().setSignBackLines(sign, Unsafe.castNonNull(sign.getLines()));
	}
}
