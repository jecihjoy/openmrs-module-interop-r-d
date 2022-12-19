package org.openmrs.module.interop.api.advice;

import ca.uhn.fhir.context.FhirContext;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.interop.FhirConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("patientCreationAdvice")
public class PatientCreationAdvice implements AfterReturningAdvice {
	
	@Autowired
	private FhirPatientService fhirPatientService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PatientCreationAdvice.class);
	
	@Override
	public void afterReturning(Object o, Method method, Object[] args, Object o1) throws Throwable {
		if (method.getName().equals("saveEncounter")) {
			Encounter encounter = (Encounter) args[0];
			if (encounter != null) {
				LOGGER.error("Intercepting hiv enrolment for " + encounter.getPatient().getUuid());
				convertToFhir(encounter.getPatient());
				if (this.fhirPatientService == null) {
					LOGGER.error("***************** PATIENT SERVICE IS NULL");
				} else {
					LOGGER.error("================= PATIENT SERVICE IS NOT NULL");
					org.hl7.fhir.r4.model.Patient fhirPatient = this.fhirPatientService
					        .get(encounter.getPatient().getUuid());
					LOGGER.error("the fhir patient " + fhirPatient.getBirthDate());
				}
			}
			
		}
	}
	
	public static void convertToFhir(Patient patient) {
		FhirContext fhirContext = FhirContext.forR4();
		String username = "admin";
		String password = "Admin123";
		String serverUrl = "http://localhost:8080/openmrs/ws/fhir2/R4";
		
		FhirConverterUtil converterUtil = new FhirConverterUtil(serverUrl, username, password, fhirContext);
		
		final String fhirUrl = "/Patient/" + patient.getUuid();
		org.hl7.fhir.r4.model.Patient resource = (org.hl7.fhir.r4.model.Patient) converterUtil.fetchFhirResource(fhirUrl);
		if (resource == null) {
			LOGGER.error("RESOURCE IS EMPTY %%%%%");
		} else {
			LOGGER.error("SEND THI RESOURCE TO OpenHIM " + resource.getId());
		}
	}
}
