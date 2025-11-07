package org.bahmni.module.fhir2AddlExtension.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({ "classpath:applicationContext-service.xml", "classpath*:TestingApplicationContext.xml",
        "classpath*:moduleApplicationContext.xml" })
public class TestSpringConfiguraiton {
	
}
