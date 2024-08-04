package com.nisovin.shopkeepers.testutil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * Base class that handled as proxies.
 *
 * @param <T>
 *            the proxied type
 */
abstract class ProxyHandler<@NonNull T> implements InvocationHandler {

	@FunctionalInterface
	public interface MethodHandler<@NonNull T> {
		@Nullable
		Object handle(@NonNull T proxy, @Nullable Object @Nullable [] args);
	}

	private final Map<Method, MethodHandler<@NonNull T>> methodHandlers = new HashMap<>();
	private final Class<@NonNull T> proxiedInterface;

	public ProxyHandler(Class<@NonNull T> proxiedInterface) {
		assert proxiedInterface != null && proxiedInterface.isInterface();
		this.proxiedInterface = proxiedInterface;
		try {
			Unsafe.initialized(this).setupMethodHandlers();
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	protected void setupMethodHandlers() throws Exception {
	}

	protected final void addHandler(Method method, MethodHandler<@NonNull T> handler) {
		methodHandlers.put(method, handler);
	}

	public final @NonNull T newProxy() {
		return Unsafe.cast(Proxy.newProxyInstance(
				proxiedInterface.getClassLoader(),
				new Class<?>[] { proxiedInterface },
				this
		));
	}

	@Override
	public final @Nullable Object invoke(
			Object proxy,
			Method method,
			@Nullable Object @Nullable [] args
	) {
		MethodHandler<@NonNull T> handler = methodHandlers.get(method);
		if (handler != null) {
			return handler.handle(Unsafe.castNonNull(proxy), args);
		}
		throw new UnsupportedOperationException(String.valueOf(method));
	}
}
