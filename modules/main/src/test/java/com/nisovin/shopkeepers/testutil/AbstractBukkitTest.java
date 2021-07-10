package com.nisovin.shopkeepers.testutil;

public abstract class AbstractBukkitTest {

	static {
		// Setup server and plugin mocks prior to running tests:
		ServerMock.setup();
		ShopkeepersPluginMock.setup();
	}
}
