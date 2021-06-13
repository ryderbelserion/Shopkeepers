package com.nisovin.shopkeepers.testutil;

public abstract class AbstractBukkitTest {

	static {
		// Setup dummy server prior to running tests:
		ServerMock.setup();
	}
}
