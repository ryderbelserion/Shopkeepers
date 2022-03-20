package com.nisovin.shopkeepers.testutil;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_14_R1.util.Versioning;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Mocks the Server (at least the functions required for our tests).
 * <p>
 * Adopted from CraftBukkit.
 */
class ServerMock extends ProxyHandler<@NonNull Server> {

	// Static initializer: Ensures that this is only setup once across all tests.
	static {
		// Set up the server mock as Bukkit API provider:
		Server serverMock = new ServerMock().newProxy();
		Bukkit.setServer(serverMock);
	}

	// Calling this method ensures that the static initializer is invoked.
	public static void setup() {
	}

	private ServerMock() {
		super(Server.class);
	}

	@Override
	protected void setupMethodHandlers() throws Exception {
		this.addHandler(Server.class.getMethod("getItemFactory"), (proxy, args) -> {
			return Unsafe.assertNonNull(CraftItemFactory.instance());
		});

		this.addHandler(Server.class.getMethod("getName"), (proxy, args) -> {
			return ServerMock.class.getName();
		});

		this.addHandler(Server.class.getMethod("getVersion"), (proxy, args) -> {
			Package pkg = Unsafe.assertNonNull(ServerMock.class.getPackage());
			return pkg.getImplementationVersion();
		});

		this.addHandler(Server.class.getMethod("getBukkitVersion"), (proxy, args) -> {
			return Versioning.getBukkitVersion();
		});

		final Logger logger = Logger.getLogger(ServerMock.class.getCanonicalName());
		this.addHandler(Server.class.getMethod("getLogger"), (proxy, args) -> {
			return logger;
		});

		this.addHandler(Server.class.getMethod("getUnsafe"), (proxy, args) -> {
			return CraftMagicNumbers.INSTANCE;
		});

		this.addHandler(
				Server.class.getMethod("createBlockData", Material.class),
				(proxy, args) -> {
					Validate.notNull(args, "args is null");
					assert args != null;
					Material material = Unsafe.castNonNull(args[0]);
					return CraftBlockData.newData(material, Unsafe.uncheckedNull());
				}
		);
	}
}
