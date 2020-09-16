package com.hockeyengine.linesman.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
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

	@Test
	void testGetReport() throws PluginException {
		String report = iceOasisWatcher.getReport();
		logger.info("IceOasisWatcher report: " + report);
	}
	
//	@Test
	void testHelthCheck() throws PluginException {
		String result = iceOasisWatcher.helthCheck();
		logger.info("IceOasisWatcher helthcheck: " + result);
	}

	@Test
	void testInit() {
		iceOasisWatcher.init();
	}

//	@Test
	void testStart() throws PluginException {
		iceOasisWatcher.start();
	}

	@Test
	void testStop() throws PluginException {
		iceOasisWatcher.stop();
	}
	
//	@Test
	void testWatchIceOasis_1() throws PluginException {
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "test");
		iceOasisWatcher.watchIceOasis(context);
	}
	
//	@Test
	void testWatchIceOasis_2() throws PluginException {
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "test");
		iceOasisWatcher.watchIceOasis(context);
		iceOasisWatcher.watchIceOasis(context);
	}
	
//	@Test
	void testSendSMSMessage() {
		iceOasisWatcher.sendSMSMessage("Test message from linesman", "+13023454133");
	}
	
	@Test
	void testParseTime_1() {
		LocalTime localTime = iceOasisWatcher.parseStartTime("6:30 A - 8:00 A");
		logger.info("LocalTime: " + localTime);
		assertEquals("06:30", localTime.toString());
	}
	
	@Test
	void testParseTime_2() {
		LocalTime localTime = iceOasisWatcher.parseStartTime("6:30 P - 8:00 P");
		logger.info("LocalTime: " + localTime);
		assertEquals("18:30", localTime.toString());
	}
	
	@Test
	void testParseTime_3() {
		LocalTime localTime = iceOasisWatcher.parseStartTime("12:30 P - 2:00 P");
		logger.info("LocalTime: " + localTime);
		assertEquals("12:30", localTime.toString());
	}
	

}
