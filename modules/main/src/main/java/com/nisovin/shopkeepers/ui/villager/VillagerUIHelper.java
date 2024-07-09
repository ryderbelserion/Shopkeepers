package com.nisovin.shopkeepers.ui.villager;

import org.bukkit.entity.AbstractVillager;

import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public class VillagerUIHelper {

	public static boolean checkVillagerValid(AbstractVillager villager, UISession uiSession) {
		return checkVillagerValid(villager, uiSession, false, true);
	}

	public static boolean checkVillagerValid(
			AbstractVillager villager,
			UISession uiSession,
			boolean silent,
			boolean closeEditor
	) {
		if (!villager.isValid()) {
			if (!silent) {
				TextUtils.sendMessage(uiSession.getPlayer(), Messages.villagerNoLongerExists);
			}
			if (closeEditor) {
				uiSession.abortDelayed();
			}
			return false;
		}
		return true;
	}

	private VillagerUIHelper() {
	}
}
