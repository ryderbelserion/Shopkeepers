package com.nisovin.shopkeepers.testutil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_14_R1.util.Versioning;

/**
 * Mocks the Server (at least the functions required for our tests).
 * <p>
 * Adopted from CraftBukkit.
 */
public class DummyServer implements InvocationHandler {

	private static interface MethodHandler {
		Object handle(DummyServer server, Object[] args);
	}

	private static final Map<Method, MethodHandler> methods = new HashMap<>();
	static {
		try {
			methods.put(Server.class.getMethod("getItemFactory"), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return CraftItemFactory.instance();
				}
			});
			methods.put(Server.class.getMethod("getName"), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return DummyServer.class.getName();
				}
			});
			methods.put(Server.class.getMethod("getVersion"), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return DummyServer.class.getPackage().getImplementationVersion();
				}
			});
			methods.put(Server.class.getMethod("getBukkitVersion"), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return Versioning.getBukkitVersion();
				}
			});
			methods.put(Server.class.getMethod("getLogger"), new MethodHandler() {
				final Logger logger = Logger.getLogger(DummyServer.class.getCanonicalName());

				@Override
				public Object handle(DummyServer server, Object[] args) {
					return logger;
				}
			});
			methods.put(Server.class.getMethod("getUnsafe"), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return CraftMagicNumbers.INSTANCE;
				}
			});
			methods.put(Server.class.getMethod("createBlockData", Material.class), new MethodHandler() {
				@Override
				public Object handle(DummyServer server, Object[] args) {
					return CraftBlockData.newData((Material) args[0], null);
				}
			});

			// set dummy server:
			Bukkit.setServer((Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] { Server.class }, new DummyServer()));
		} catch (Throwable t) {
			throw new Error(t);
		}
	}

	public static void setup() {
	}

	private DummyServer() {
	};

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		MethodHandler handler = methods.get(method);
		if (handler != null) {
			return handler.handle(this, args);
		}
		throw new UnsupportedOperationException(String.valueOf(method));
	}
}
