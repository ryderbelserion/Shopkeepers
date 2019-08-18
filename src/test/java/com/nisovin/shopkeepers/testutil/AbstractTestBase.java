package com.nisovin.shopkeepers.testutil;

public abstract class AbstractTestBase {

	static {
		// Setup dummy server prior to running tests:
		DummyServer.setup();
	}
}
