package com.hockeyengine.linesman.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hockeyengine.linesman.plugin.iceoasis.IceOasisWatcher;
import com.hockeyengine.quickshift.core.PluginException;

class IceOasisWatcherTest {

	private IceOasisWatcher iceOasisWatcher;

	@BeforeEach
	void setUp() throws Exception {
		iceOasisWatcher = new IceOasisWatcher();
	}

//	@Test
	void testGetReport() {
		fail("Not yet implemented");
	}

//	@Test
	void testInit() {
		fail("Not yet implemented");
	}

//	@Test
	void testStart() throws PluginException {
		iceOasisWatcher.start();
	}

//	@Test
	void testStop() {
		fail("Not yet implemented");
	}
//	@Test
	void testSendSMSMessage() {
		iceOasisWatcher.sendSMSMessage("Test message from linesman", "+13023454133");
	}

}
