package org.openmrs.module.interop;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirConverterUtil {
	
	private static final Logger log = LoggerFactory.getLogger(FhirConverterUtil.class);
	
	private final String fhirUrl;
	
	private final String sourceUser;
	
	private final String sourcePw;
	
	private final FhirContext fhirContext;
	
	public FhirConverterUtil(String sourceFhirUrl, String sourceUser, String sourcePw, FhirContext fhirContext) {
		this.fhirUrl = sourceFhirUrl;
		this.sourceUser = sourceUser;
		this.sourcePw = sourcePw;
		this.fhirContext = fhirContext;
	}
	
	public Resource fetchFhirResource(String resourceType, String resourceId) {
		try {
			IGenericClient client = getSourceClient();
			IBaseResource resource = client.read().resource(resourceType).withId(resourceId).execute();
			return (Resource) resource;
		}
		catch (Exception e) {
			log.error(String.format("Failed fetching FHIR %s resource with Id %s: %s", resourceType, resourceId, e));
			return null;
		}
	}
	
	public Resource fetchFhirResource(String resourceUrl) {
		// Parse resourceUrl
		String[] sepUrl = resourceUrl.split("/");
		String resourceId = sepUrl[sepUrl.length - 1];
		String resourceType = sepUrl[sepUrl.length - 2];
		return fetchFhirResource(resourceType, resourceId);
	}
	
	public IGenericClient getSourceClient() {
		return getSourceClient(false);
	}
	
	public IGenericClient getSourceClient(boolean enableRequestLogging) {
		IClientInterceptor authInterceptor = new BasicAuthInterceptor(this.sourceUser, this.sourcePw);
		fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);
		
		IGenericClient client = fhirContext.getRestfulClientFactory().newGenericClient(this.fhirUrl);
		client.registerInterceptor(authInterceptor);
		
		if (enableRequestLogging) {
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
			loggingInterceptor.setLogger(log);
			loggingInterceptor.setLogRequestSummary(true);
			client.registerInterceptor(loggingInterceptor);
		}
		
		return client;
	}
	
	public String getSourceFhirUrl() {
		return fhirUrl;
	}
}
