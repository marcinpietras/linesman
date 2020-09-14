package com.hockeyengine.linesman.plugin;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hockeyengine.linesman.plugin.iceoasis.IceOasisWatcher;
import com.hockeyengine.quickshift.core.PluginException;

class IceOasisWatcherTest {
	
	private Logger logger = LoggerFactory.getLogger(IceOasisWatcherTest.class);

	private IceOasisWatcher iceOasisWatcher;

	@BeforeEach
	void setUp() throws Exception {
		iceOasisWatcher = new IceOasisWatcher();
		iceOasisWatcher.init();
	}

//	@Test
	void testGetReport() throws PluginException {
		String report = iceOasisWatcher.getReport();
		logger.info("IceOasisWatcher report: " + report);
	}

//	@Test
	void testInit() {
		iceOasisWatcher.init();
	}

//	@Test
	void testStart() throws PluginException {
		iceOasisWatcher.start();
	}

//	@Test
	void testStop() throws PluginException {
		iceOasisWatcher.stop();
	}
	
//	@Test
	void testWstchIceOasis() throws PluginException {
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "test");
		iceOasisWatcher.watchIceOasis(context);
	}
	
//	@Test
	void testSendSMSMessage() {
		iceOasisWatcher.sendSMSMessage("Test message from linesman", "+13023454133");
	}
	

}
