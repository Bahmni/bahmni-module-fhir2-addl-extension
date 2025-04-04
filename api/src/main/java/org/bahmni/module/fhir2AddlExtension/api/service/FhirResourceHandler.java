/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

/**
 *  Notice:
 *  - Partial content of this file, specifically related to identification of resourceBindings have been copied from HAPI FhirServlet class
 */

package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.ResourceBinding;
import ca.uhn.fhir.rest.server.method.BaseMethodBinding;
import ca.uhn.fhir.rest.server.method.ConformanceMethodBinding;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.ReflectionUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.module.fhir2.FhirActivator;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Transactional
@Slf4j
public class FhirResourceHandler {
    private final FhirContext fhirContext;

    private Map<Bundle.HTTPVerb, RequestTypeEnum> httpVerbToRequestTypeEnum = new HashMap<>();

    @Autowired
    public FhirResourceHandler(@Qualifier("fhirR4") FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        this.initialize();
    }

    public Optional<IResourceProvider> getResourceProvider(Class clazz) {
        ConfigurableApplicationContext context = FhirActivator.getApplicationContext();
        Set<String> validBeanNames = Arrays.stream(context.getBeanNamesForAnnotation(R4Provider.class))
                .collect(Collectors.toSet());
        List<IResourceProvider> resourceProviders = context.getBeansOfType(IResourceProvider.class).entrySet().stream()
                .filter(entry -> validBeanNames.contains(entry.getKey())).map(Map.Entry::getValue)
                .collect(Collectors.toList());
        Optional<IResourceProvider> resourceProvider = resourceProviders.stream().filter(provider -> provider.getResourceType().equals(clazz)).findFirst();
        return resourceProvider;
    }

    @SneakyThrows
    public Optional<MethodOutcome> invokeResourceProvider(Bundle.HTTPVerb httpVerb, Resource resource) {
        if (!supportsVerb(httpVerb)) {
            return Optional.empty();
        }
        Optional<IResourceProvider> resourceProvider = this.getResourceProvider(resource.getClass());
        if (!resourceProvider.isPresent()) {
            throw new RuntimeException(String.format("There are no resource provider for resource [%s]",resource.getClass().getSimpleName()));
        }
        return invokeResourceProviderInternal(httpVerb, resource, resourceProvider.get());

    }

    @SneakyThrows
    public Optional<MethodOutcome> invokeResourceProvider(Bundle.HTTPVerb httpVerb, Resource resource, IResourceProvider resourceProvider) {
        if (!supportsVerb(httpVerb)) {
            return Optional.empty();
        }
        return invokeResourceProviderInternal(httpVerb, resource, resourceProvider);
    }

    private void initialize() {
        httpVerbToRequestTypeEnum.put(Bundle.HTTPVerb.POST, RequestTypeEnum.POST);
        httpVerbToRequestTypeEnum.put(Bundle.HTTPVerb.PUT, RequestTypeEnum.PUT);
    }

    private Map<String, ResourceBinding> getResourceBindingMap(final IResourceProvider resourceProvider) {
        Map<String, ResourceBinding> myResourceNameToBinding = new HashMap<>();
        ResourceBinding myGlobalBinding = new ResourceBinding();
        ResourceBinding myServerBinding = new ResourceBinding();
        for (Method m : ReflectionUtil.getDeclaredMethods(resourceProvider.getClass())) {
            BaseMethodBinding<?> foundMethodBinding = BaseMethodBinding.bindMethod(m, fhirContext, resourceProvider);
            if (foundMethodBinding == null || foundMethodBinding instanceof ConformanceMethodBinding) {
                log.debug("There are no methods on optionalResProvider to bind. Or methods are for conformance binding.");
                continue;
            }

            if (!Modifier.isPublic(m.getModifiers())) {
                log.warn(Msg.code(290) + "Method '" + m.getName() + "' is not public");
                continue;
            }
            if (Modifier.isStatic(m.getModifiers())) {
                log.warn(Msg.code(291) + "Method '" + m.getName() + "' is static, FHIR RESTful methods must not be static");
                continue;
            }
            log.debug("Scanning public method: {}#{}", resourceProvider.getClass(), m.getName());
            String resourceName = foundMethodBinding.getResourceName();
            ResourceBinding resourceBinding;
            if (resourceName == null) {
                if (foundMethodBinding.isGlobalMethod()) {
                    resourceBinding = myGlobalBinding;
                } else {
                    resourceBinding = myServerBinding;
                }
            } else {
                RuntimeResourceDefinition definition = this.fhirContext.getResourceDefinition(resourceName);
                if (myResourceNameToBinding.containsKey(definition.getName())) {
                    resourceBinding = myResourceNameToBinding.get(definition.getName());
                } else {
                    resourceBinding = new ResourceBinding();
                    resourceBinding.setResourceName(resourceName);
                    myResourceNameToBinding.put(resourceName, resourceBinding);
                }
            }

            List<Class<?>> allowableParams = foundMethodBinding.getAllowableParamAnnotations();
            if (allowableParams != null) {
                for (Annotation[] nextParamAnnotations : m.getParameterAnnotations()) {
                    for (Annotation annotation : nextParamAnnotations) {
                        Package pack = annotation.annotationType().getPackage();
                        if (pack.equals(IdParam.class.getPackage())) {
                            if (!allowableParams.contains(annotation.annotationType())) {
                                throw new ConfigurationException(Msg.code(292) + "Method[" + m.toString() + "] is not allowed to have a parameter annotated with " + annotation);
                            }
                        }
                    }
                }
            }

            resourceBinding.addMethod(foundMethodBinding);
            log.debug(" * Method: {}#{} is a handler", resourceProvider.getClass(), m.getName());
        }
        return myResourceNameToBinding;
    }

    private Optional<BaseMethodBinding<?>> identifyResourceMethod(Map<String, ResourceBinding> myResourceNameToBinding, String resourceName, RequestTypeEnum requestTypeEnum) {
        ServletRequestDetails requestDetails = new ServletRequestDetails();
        requestDetails.setRequestType(requestTypeEnum);
        requestDetails.setResourceName(resourceName);
        BaseMethodBinding<?> resourceMethod;
        ResourceBinding resourceBinding = myResourceNameToBinding.get(resourceName);
        if (resourceBinding == null) {
            return Optional.empty();
        }
        resourceMethod = resourceBinding.getMethod(requestDetails);
        return Optional.ofNullable(resourceMethod);

    }

    private Optional<MethodOutcome> invokeResourceProviderInternal(Bundle.HTTPVerb httpVerb, Resource resource, IResourceProvider resourceProvider) throws IllegalAccessException, InvocationTargetException {
        Map<String, ResourceBinding> myResourceNameToBinding = this.getResourceBindingMap(resourceProvider);
        String resourceName = resourceProvider.getResourceType().getSimpleName();
        Optional<BaseMethodBinding<?>> methodBinding = this.identifyResourceMethod(myResourceNameToBinding, resourceName, httpVerbToRequestTypeEnum.get(httpVerb));
        if (!methodBinding.isPresent()) {
            return Optional.empty();
        }

        MethodOutcome response;
        Object invocationResult = methodBinding.get().getMethod().invoke(resourceProvider, new Object[] {resource});
        if (invocationResult instanceof IBaseOperationOutcome) {
            response = new MethodOutcome();
            response.setOperationOutcome((IBaseOperationOutcome) invocationResult);
        } else {
            response = (MethodOutcome) invocationResult;
        }
        return Optional.ofNullable(response);
    }

    private boolean supportsVerb(Bundle.HTTPVerb httpVerb) {
        return httpVerbToRequestTypeEnum.get(httpVerb) != null;
    }
}
