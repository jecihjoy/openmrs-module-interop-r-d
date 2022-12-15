package org.openmrs.module.interop.api.advice;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

public class PatientCreationAdvice implements AfterReturningAdvice {
	
	@Autowired
	private PatientTranslator translator;

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientCreationAdvice.class);
	
	@Override
	public void afterReturning(Object o, Method method, Object[] args, Object o1) throws Throwable {
		
		if (method.getName().equals("saveEncounter")) {
			LOGGER.error("SAVING ENCOUNTER");
			Encounter encounter = (Encounter) args[0];
			if (encounter != null) {
				LOGGER.error("SAVED PATIENT " + encounter.getUuid());
				LOGGER.error("SAVED PATIENT " + encounter.getEncounterType().getName());
				Patient patient1 = encounter.getPatient();
				if (translator == null) {
					LOGGER.error("translator is a null object +++++++++++++");
				} else {
					LOGGER.error("Translator is not null  ================");
				}
				org.hl7.fhir.r4.model.Patient translated = translator.toFhirResource(patient1);
				LOGGER.error("AFTER HITTING TRANSLATOR");
				if (translated != null) {
					LOGGER.error("fhir patient uuid " + translated.getId());
				}
			}
			
		}
		if (method.getName().equals("saveEncounter")) {
			LOGGER.error("SAVING OBS");
		}
	}
}
