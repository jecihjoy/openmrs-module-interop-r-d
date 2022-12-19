package org.openmrs.module.interop.EventBus;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;
import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class EventEngine {
	
	Log log = LogFactory.getLog(EventEngine.class);
	
	protected JmsTemplate jmsTemplate = null;
	
	protected SingleConnectionFactory connectionFactory;
	
	private synchronized void initializeIfNeeded() {
		if (jmsTemplate == null) {
			log.info("creating connection factory");
			String property = "http://localhost:8161/";
			String brokerURL;
			if (property == null || property.isEmpty()) {
				String dataDirectory = new File(OpenmrsUtil.getApplicationDataDirectory(), "activemq-data")
				        .getAbsolutePath();
				try {
					dataDirectory = URLEncoder.encode(dataDirectory, "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					throw new RuntimeException("Failed to encode URI", e);
				}
				brokerURL = "vm://localhost?broker.persistent=true&broker.useJmx=false&broker.dataDirectory="
				        + dataDirectory;
			} else {
				brokerURL = "tcp://" + property;
			}
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(brokerURL);
			connectionFactory = new SingleConnectionFactory(cf); // or CachingConnectionFactory ?
			jmsTemplate = new JmsTemplate(connectionFactory);
		} else {
			log.trace("messageListener already defined");
		}
	}
	
	private String getExternalUrl() {
		try {
			return Context.getRegisteredComponent("adminService", AdministrationService.class).getGlobalProperty(
			    "activeMQ.externalUrl");
		}
		catch (NullPointerException ex) {
			log.error("AdministrationService not yet initialized to get the activeMQ.externalUrl setting", ex);
		}
		return null;
	}
	
	public Destination getDestination(final String topicName) {
		return new Topic() {
			
			@Override
			public String getTopicName() throws JMSException {
				return topicName;
			}
		};
	}
	
	public void fireEvent(final Destination dest, final Object object) {
		EventMessage eventMessage = new EventMessage();
		if (object instanceof OpenmrsObject) {
			log.error("IS INSTANCE OF OPENMRS");
			eventMessage.put("uuid", ((OpenmrsObject) object).getUuid());
		}
		eventMessage.put("classname", object.getClass().getName());
		eventMessage.put("action", "CREATE");
		
		doFireEvent(dest, eventMessage);
	}
	
	private void doFireEvent(final Destination dest, final EventMessage eventMessage) {
		
		initializeIfNeeded();
		
		jmsTemplate.send(dest, new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				log.error("Sending data " + eventMessage);
				
				MapMessage mapMessage = session.createMapMessage();
				if (eventMessage != null) {
					for (Map.Entry<String, Serializable> entry : eventMessage.entrySet()) {
						mapMessage.setObject(entry.getKey(), entry.getValue());
					}
				}
				
				return mapMessage;
			}
		});
	}
}
