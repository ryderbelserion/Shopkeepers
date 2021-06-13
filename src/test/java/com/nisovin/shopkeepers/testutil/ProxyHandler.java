package com.nisovin.shopkeepers.testutil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class that handles method invocations for mocks that are implemented as proxies.
 *
 * @param <T>
 *            the proxied type
 */
abstract class ProxyHandler<T> implements InvocationHandler {

	@FunctionalInterface
	public interface MethodHandler<T> {
		Object handle(T proxy, Object[] args);
	}

	private final Map<Method, MethodHandler<T>> methodHandlers = new HashMap<>();
	private final Class<T> proxiedInterface;

	public ProxyHandler(Class<T> proxiedInterface) {
		assert proxiedInterface != null && proxiedInterface.isInterface();
		this.proxiedInterface = proxiedInterface;
		try {
			this.setupMethodHandlers();
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	protected void setupMethodHandlers() throws Exception {
	}

	protected final void addHandler(Method method, MethodHandler<T> handler) {
		methodHandlers.put(method, handler);
	}

	@SuppressWarnings("unchecked")
	public final T newProxy() {
		return (T) Proxy.newProxyInstance(proxiedInterface.getClassLoader(), new Class<?>[] { proxiedInterface }, this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final Object invoke(Object proxy, Method method, Object[] args) {
		MethodHandler<T> handler = methodHandlers.get(method);
		if (handler != null) {
			return handler.handle((T) proxy, args);
		}
		throw new UnsupportedOperationException(String.valueOf(method));
	}
}
