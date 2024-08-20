package com.nisovin.shopkeepers.compat;

import com.nisovin.shopkeepers.NMSHandler;
import org.checkerframework.checker.nullness.qual.Nullable;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.java.Validate;

public final class NMSManager {

	// ----

	private static @Nullable NMSCallProvider provider;

	public static boolean hasProvider() {
		return (provider != null);
	}

	public static void load() {
        try {
            provider = new NMSHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	public static NMSCallProvider getProvider() {
		return Validate.State.notNull(provider, "NMS provider is not set up!");
	}
}