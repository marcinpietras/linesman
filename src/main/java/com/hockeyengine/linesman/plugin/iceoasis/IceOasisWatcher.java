/**
 * 
 */
package com.hockeyengine.linesman.plugin.iceoasis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.base.Stopwatch;
import com.hockeyengine.quickshift.core.Plugin;
import com.hockeyengine.quickshift.core.PluginException;

/**
 * @author mpietras
 *
 */
//TODO 2. single notification
//TODO 3. fs filtering
@Component
public class IceOasisWatcher implements Plugin {

	private Logger logger = LoggerFactory.getLogger(IceOasisWatcher.class);

	private static final String SCHEDULE_URL = "https://iceoasis.frontline-connect.com/sessionslist.cfm?fac=iceoasis&facid=1";

	private static final String SOLD_OUT = "SOLD OUT!";
	
	private static final String CRON_EXPRESSION = "0 0/5 * * * ?";

	private RestTemplate restTemplate = new RestTemplate();

	private AmazonSNSClient snsClient = new AmazonSNSClient();

	private Scheduler scheduler;
	
	private List<String> lastExecutionLog = new ArrayList<String>();

	@Override
	public void init() {
		logger.info("Initializing plugin iceOasisWatcher");
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			logger.error("Initialize plugin iceOasisWatcher fail", e);
		}

	}
	
	@Override
	public String helthCheck() throws PluginException {
		logger.info("Healthchecking plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		context.put("mode", "test");
		watchIceOasis(context);
		return "Healthcheck success. Last execution log: " + this.lastExecutionLog;
	}
	
	@Override
	public String getReport() throws PluginException {
		logger.info("Getting report from plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		
		StringBuilder report = new StringBuilder();
		report.append("Cron job set to ");
		report.append(CRON_EXPRESSION);
		report.append(" is running: ");
		try {
			report.append(this.scheduler.checkExists(createWatchJobDetail(context).getKey()));
		} catch (SchedulerException e) {
			throw new PluginException("Stopping plugin iceOasisWatcher fail", e);
		}
		report.append(". Last execution log: ");
		report.append(this.lastExecutionLog);
		return report.toString();
	}

	@Override
	public String start() throws PluginException {
		logger.info("Starting plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("cronTrigger1", "group1")
				.withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)).build();
		JobDetail watchJobDetail = createWatchJobDetail(context);
		try {
			if (this.scheduler.checkExists(watchJobDetail.getKey())) {
				this.scheduler.deleteJob(watchJobDetail.getKey());
			}
			scheduler.scheduleJob(watchJobDetail, trigger);
		} catch (SchedulerException e) {
			throw new PluginException("Starting plugin iceOasisWatcher fail", e);
		}

		return "IceOasisWatcher started";
	}

	@Override
	public String stop() throws PluginException {
		logger.info("Stopping plugin iceOasisWatcher");
		Map<String, String> context = new HashMap<String, String>();
		
		JobDetail watchJobDetail = createWatchJobDetail(context);
		try {
			if (this.scheduler.checkExists(watchJobDetail.getKey())) {
				this.scheduler.deleteJob(watchJobDetail.getKey());
			}
		} catch (SchedulerException e) {
			throw new PluginException("Stopping plugin iceOasisWatcher fail", e);
		}
		return "IceOasisWatcher stopped";
	}
	
	public void watchIceOasis(Map<String, String> context) throws PluginException {
		List<String> executionLog = new ArrayList<String>();
		executionLog.add("Context=" + context.toString());
		
		String htmlSchedule = getIceOasisHtmlSchedule(executionLog, context);
		List<DayIO> schedule = htmlToSchedule(htmlSchedule, executionLog, context);
//		logger.info("IceOasis schedule: " + schedule);
		
		// Stick N Shoot
		String eventName = "Stick N Shoot";
		List<DayIO> filteredSchedule = filterOpenScheduleByName(eventName, schedule, executionLog, context);
		if (filteredSchedule.isEmpty()) {
			executionLog.add("Notification=There is nothing to notify about event " + eventName);
			logger.info("There is nothing to notify about event {}", eventName);
		} else {
			notifyAboutEvents(eventName, filteredSchedule, executionLog, context);
		}
		
		// Figure Skating FreeStyle 60 Minutes
		eventName = "Figure Skating FreeStyle 60 Minutes";
		filteredSchedule = filterOpenScheduleByName(eventName, schedule, executionLog, context);
		if (filteredSchedule.isEmpty()) {
			executionLog.add("Notification=There is nothing to notify about event " + eventName);
			logger.info("There is nothing to notify about event {}", eventName);
		} else {
			notifyAboutEvents(eventName, filteredSchedule, executionLog, context);
		}
		
		// Figure Skating FREESTYLE 90 Minutes
		eventName = "Skating FREESTYLE 90 Minutes";
		filteredSchedule = filterOpenScheduleByName(eventName, schedule, executionLog, context);
		if (filteredSchedule.isEmpty()) {
			executionLog.add("Notification=There is nothing to notify about event " + eventName);
			logger.info("There is nothing to notify about event {}", eventName);
		} else {
			notifyAboutEvents(eventName, filteredSchedule, executionLog, context);
		}
		this.lastExecutionLog = executionLog;
		
	}

//	public void watchIceOasisForStickNShoot() throws PluginException {
//		String eventName = "Stick N Shoot";
//		List<DayIO> schedule = checkScheduleFor(eventName);
//		if (schedule.isEmpty()) {
//			logger.info("There is nothing to notify about event {}", eventName);
//		} else {
//			notifyAboutEvents(eventName, schedule);
//		}
//	}
//
//	public void watchIceOasisForFreestyle() throws PluginException {
//		String eventName = "Figure Skating FreeStyle 60 Minutes";
//		List<DayIO> schedule = checkScheduleFor(eventName);
//
//		String eventName2 = "Figure Skating FREESTYLE 90 Minutes";
//		List<DayIO> schedule2 = checkScheduleFor(eventName2);
//		
//		if (schedule.isEmpty() && schedule2.isEmpty()) {
//			logger.info("There is nothing to notify about event {}", eventName);
//		} else {
//			notifyAboutEvents(eventName + " / " + eventName2, schedule);
//		}
//	}

//	private List<DayIO> checkScheduleFor(String eventName) throws PluginException {
//		logger.info("Checking IceOasis schedule");
//		Stopwatch stopwatch = Stopwatch.createStarted();
//		String htmlSchedule = getIceOasisHtmlSchedule();
//		List<DayIO> schedule = htmlToSchedule(htmlSchedule);
////		logger.info("IceOasis schedule: " + schedule);
//
//		List<DayIO> filteredSchedule = filterOpenScheduleByName(eventName, schedule);
////		logger.info("Filtered schedule: " + filteredSchedule);
//
//		logger.info("Checking IceOasis schedule success in {} ms ", stopwatch.elapsed(TimeUnit.SECONDS));
//		return filteredSchedule;
//	}

	private List<DayIO> filterOpenScheduleByName(String name, List<DayIO> sourceSchedule, List<String> executionLog, Map<String, String> context) {
		List<DayIO> filteredSchedule = new ArrayList<DayIO>();
		int filteredNumberOfSessions = 0;

		for (DayIO day : sourceSchedule) {
			DayIO newDay = new DayIO(day.getDate());
			for (SessionIO session : day.getSessions()) {
				if (name.equalsIgnoreCase(session.getName().trim())
						&& !SOLD_OUT.equalsIgnoreCase(session.getOpenings().trim())) {
					newDay.getSessions().add(session);
					filteredNumberOfSessions++;
				}
			}
			if (!newDay.getSessions().isEmpty()) {
				filteredSchedule.add(newDay);
			}
		}
		executionLog.add("FilterOpenSchedule=Success: Filtered schedule for name " + name + ": " + filteredSchedule.size() + " days, " + filteredNumberOfSessions + " sessions");
		logger.info("Filtered schedule for name {}: {} days, {} sessions", name, filteredSchedule.size(),
				filteredNumberOfSessions);
		return filteredSchedule;
	}

	private void notifyAboutEvents(String eventName, List<DayIO> schedule, List<String> executionLog, Map<String, String> context) {
		logger.info("Notifing about event {}, schedule {}", eventName, schedule);
		StringBuilder message = new StringBuilder();
		if ("test".equalsIgnoreCase(context.get("mode"))) {
			message.append("TEST ");
		}
		message.append("HockeyEngine Linesman found open events ");
		message.append(eventName);
		message.append(". Schedule at ");
		message.append(SCHEDULE_URL);

		String phoneNumber = "+13023454133";
		sendSMSMessage(message.toString(), phoneNumber);
		executionLog.add("Notification=Success");
	}

	public void sendSMSMessage(String message, String phoneNumber) {
		Map<String, MessageAttributeValue> smsAttributes = new HashMap<String, MessageAttributeValue>();
		smsAttributes.put("AWS.SNS.SMS.SenderID",
				new MessageAttributeValue().withStringValue("Linesman").withDataType("String"));
		smsAttributes.put("AWS.SNS.SMS.MaxPrice",
				new MessageAttributeValue().withStringValue("1.00").withDataType("Number"));
		smsAttributes.put("AWS.SNS.SMS.SMSType",
				new MessageAttributeValue().withStringValue("Transactional").withDataType("String"));
		PublishResult result = this.snsClient.publish(new PublishRequest().withMessage(message)
				.withPhoneNumber(phoneNumber).withMessageAttributes(smsAttributes));
		logger.info("Send SMS message result: {}", result);
	}

	private String getIceOasisHtmlSchedule(List<String> executionLog, Map<String, String> context) throws PluginException {
		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			String html = restTemplate.getForObject(SCHEDULE_URL, String.class);
			logger.info("Getting IceOasis schedule success in {} ms ", stopwatch.elapsed(TimeUnit.SECONDS));
			executionLog.add("GetIceOawsisSchedule=Success");
			return html;
		} catch (Exception e) {
			executionLog.add("GetIceOawsisSchedule=Error");
			throw new PluginException("Getting IceOasis schedule fail", e);
		}
	}

	private List<DayIO> htmlToSchedule(String html, List<String> executionLog, Map<String, String> context) {
		List<DayIO> days = new ArrayList<DayIO>();
		Document doc = Jsoup.parse(html);
		Elements tableBody = doc.select("tbody");

		DayIO dayIO = null;
		int totalNumberOfSessions = 0;
		for (Element tableRow : tableBody.first().select("tr")) {
			Elements tableData = tableRow.select("td");
			Elements elements = tableData.get(0).getElementsByClass("alert alert-info");
			if (elements.isEmpty()) {
				String time = tableData.get(1).text();
				String openings = tableData.get(2).text();
				String name = tableData.get(3).text();
				String length = tableData.get(4).text();
				String price = tableData.get(6).text();
				SessionIO sessionIO = new SessionIO(time, openings, name, length, price);
				totalNumberOfSessions++;
				dayIO.getSessions().add(sessionIO);
//				logger.info("SessionIO: " + sessionIO);
			} else {
				String date = tableData.get(0).text();
				dayIO = new DayIO(date);
				days.add(dayIO);
//				logger.info("DayIO: " + dayIO);
			}
		}
		executionLog.add("ParseHTML=Success: Found schedule for " + days.size() + " days, " + totalNumberOfSessions + " sessions in total");
		logger.info("Found schedule for {} days, {} sessions in total", days.size(), totalNumberOfSessions);
		return days;
	}

	private JobDetail createWatchJobDetail(Map<String, String> context) {
		JobDetail watchJobDetail = JobBuilder.newJob(WatchJob.class).withIdentity("WatchJob1", "group1").build();
		watchJobDetail.getJobDataMap().put("plugin", this);
		watchJobDetail.getJobDataMap().put("context", context);
		return watchJobDetail;
	}

}
