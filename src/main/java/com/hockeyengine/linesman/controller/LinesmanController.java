/**
 * 
 */
package com.hockeyengine.linesman.controller;

import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hockeyengine.quickshift.core.PluginService;
import com.hockeyengine.quickshift.core.PluginStatus;


/**
 * @author mpietras
 *
 */
@RestController
public class LinesmanController {
	
	private Logger logger = LoggerFactory.getLogger(LinesmanController.class);
	
	@Autowired
	private PluginService pluginService;
	
	@RequestMapping("/")
	public String index() {
		logger.info("REST Getting default");
		return "Hello from HockeyEngine Linesman (linesman)!";
	}
	
	@RequestMapping(value = "/{plugin}/start", method = RequestMethod.GET)
	public ResponseEntity<?> start(@PathVariable("plugin") @Nonnull String plugin) {
		logger.info("REST Starting pluginName={}", plugin);
		try {
			String result = this.pluginService.startPlugin(plugin);
			PluginStatus status = this.pluginService.getPluginStatus(plugin);
			logger.info("pluginName={} {} ", plugin, status);
			return ResponseEntity.ok().body(plugin + " " + status + ": " + result);
		} catch (Exception e) {
			logger.error("Starting plugin fail ", e);
			return ResponseEntity.badRequest().body("Error while starting plugin: " + e.getMessage());
		}		
	}
	
	@RequestMapping(value = "/{plugin}/stop", method = RequestMethod.GET)
	public ResponseEntity<?> stop(@PathVariable("plugin") @Nonnull String plugin) {
		logger.info("REST Stopping pluginName={}", plugin);
		try {
			String result = this.pluginService.stopPlugin(plugin);
			PluginStatus status = this.pluginService.getPluginStatus(plugin);
			logger.info("pluginName={} {} ", plugin, status);
			return ResponseEntity.ok().body(plugin + " " + status + ": " + result);
		} catch (Exception e) {
			logger.error("Stopping plugin fail ", e);
			return ResponseEntity.badRequest().body("Error while stopping plugin: " + e.getMessage());
		}		
	}
	
	@RequestMapping(value = "/{plugin}/status", method = RequestMethod.GET)
	public ResponseEntity<?> getStatus(@PathVariable("plugin") @Nonnull String plugin) {
		logger.info("REST Getting status of pluginName={}", plugin);
		try {
			PluginStatus status = this.pluginService.getPluginStatus(plugin);
			logger.info("pluginName={} {} ", plugin, status);
			return ResponseEntity.ok().body(status);
		} catch (Exception e) {
			logger.error("Getting status for plugin fail ", e);
			return ResponseEntity.badRequest().body("Error while getting status for plugin: " + e.getMessage());
		}		
	}
	
	@RequestMapping(value = "/{plugin}/report", method = RequestMethod.GET)
	public ResponseEntity<?> getDetails(@PathVariable("plugin") @Nonnull String plugin) {
		logger.info("REST Getting report of pluginName={}", plugin);
		try {
			String report = this.pluginService.getPluginReport(plugin);
			logger.info("pluginName={} {} ", plugin, report);
			return ResponseEntity.ok().body(report);
		} catch (Exception e) {
			logger.error("Getting report for plugin fail ", e);
			return ResponseEntity.badRequest().body("Error while getting report for plugin: " + e.getMessage());
		}		
	}
	
	@RequestMapping(value = "/{plugin}/healthcheck", method = RequestMethod.GET)
	public ResponseEntity<?> getHealthCHeck(@PathVariable("plugin") @Nonnull String plugin) {
		logger.info("REST Getting healthCheck of pluginName={}", plugin);
		try {
			String result = this.pluginService.healthCheck(plugin);
			logger.info("pluginName={} {} ", plugin, result);
			return ResponseEntity.ok().body(result);
		} catch (Exception e) {
			logger.error("Getting report for plugin fail ", e);
			return ResponseEntity.badRequest().body("Error while getting report for plugin: " + e.getMessage());
		}		
	}
	
	@RequestMapping(value = "/allknownplugins", method = RequestMethod.GET)
	public ResponseEntity<?> getAllKnownPlugins() {
		logger.info("REST Getting all known plugins");
		try {
			Set<String> allKnownPlugins = this.pluginService.getAllKnownPlugins();
			return ResponseEntity.ok().body(allKnownPlugins);
		} catch (Exception e) {
			logger.error("Getting all known plugins fail ", e);
			return ResponseEntity.badRequest().body("Error while getting all known plugins: " + e.getMessage());
		}		
	}

}
