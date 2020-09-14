package com.hockeyengine.linesman.plugin.iceoasis;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hockeyengine.quickshift.core.PluginException;

public class WatchJob implements Job {
	
	private Logger logger = LoggerFactory.getLogger(WatchJob.class);

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		IceOasisWatcher iceOasisWatcher = (IceOasisWatcher) jobExecutionContext.getJobDetail().getJobDataMap().get("plugin");
		Map<String, String> context = (Map<String, String>) jobExecutionContext.getJobDetail().getJobDataMap().get("context");
		try {
			iceOasisWatcher.watchIceOasis(context);
		} catch (PluginException e) {
			logger.error("Error when watching for Stick N Shoot", e);
		}
		
//		try {
//			iceOasisWatcher.watchIceOasisForFreestyle();
//		} catch (PluginException e) {
//			logger.error("Error when watching for Freestyle", e);
//		}
		
	}

}
