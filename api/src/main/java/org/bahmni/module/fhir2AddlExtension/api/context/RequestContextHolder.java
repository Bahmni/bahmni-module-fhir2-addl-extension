package org.bahmni.module.fhir2AddlExtension.api.context;

public class RequestContextHolder {
    private static final ThreadLocal<String> REQUEST_CONTEXT = new ThreadLocal<>();

    public static void setValue(String value) {
        REQUEST_CONTEXT.set(value);
    }

    public static String getValue() {
        return REQUEST_CONTEXT.get();
    }

    public static void clear() {
        REQUEST_CONTEXT.remove(); // CRITICAL: Prevent memory leaks
    }
}