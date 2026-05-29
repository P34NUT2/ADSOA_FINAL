package org.up.cd.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.up.cd.config.Config;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton. Dynamically loads service JARs at runtime via URLClassLoader.
 * Services are plain Java classes with a public byte[] execute(byte[] input) method.
 * No interface needed — pure reflection.
 */
public class DynamicServiceLoader {

    private static final Logger logger = LogManager.getLogger(DynamicServiceLoader.class);

    private final Map<Integer, Object>  instanceCache = new ConcurrentHashMap<>();
    private final Map<Integer, Method>  methodCache   = new ConcurrentHashMap<>();

    private DynamicServiceLoader() {}

    public static DynamicServiceLoader getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final DynamicServiceLoader INSTANCE = new DynamicServiceLoader();
    }

    /**
     * Executes the service for the given serviceId.
     * Loads and caches the JAR on first call.
     * Returns null if service not found or execution fails.
     */
    public byte[] execute(int serviceId, byte[] input) {
        try {
            Method method = methodCache.get(serviceId);
            Object instance = instanceCache.get(serviceId);

            if (method == null || instance == null) {
                if (!load(serviceId)) return null;
                method   = methodCache.get(serviceId);
                instance = instanceCache.get(serviceId);
            }

            return (byte[]) method.invoke(instance, (Object) input);

        } catch (Exception e) {
            logger.error("[DynamicServiceLoader] Execute error serviceId={}: {}", serviceId, e.getMessage());
            return null;
        }
    }

    private boolean load(int serviceId) {
        Config cfg = Config.getInstance();

        Config.ServiceEntry entry = cfg.getServices().stream()
                .filter(s -> s.serviceId == serviceId)
                .findFirst()
                .orElse(null);

        if (entry == null) {
            logger.warn("[DynamicServiceLoader] No config for serviceId={}", serviceId);
            return false;
        }

        try {
            File jarFile = new File(cfg.getServicesDir(), entry.jar);
            if (!jarFile.exists()) {
                logger.error("[DynamicServiceLoader] JAR not found: {}", jarFile.getAbsolutePath());
                return false;
            }

            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    this.getClass().getClassLoader()
            );

            Class<?> clazz    = loader.loadClass(entry.className);
            Object   instance = clazz.getDeclaredConstructor().newInstance();
            Method   method   = clazz.getMethod("execute", byte[].class);

            instanceCache.put(serviceId, instance);
            methodCache.put(serviceId, method);

            logger.info("[DynamicServiceLoader] Loaded serviceId={} class={} jar={}",
                    serviceId, entry.className, jarFile.getName());
            return true;

        } catch (Exception e) {
            logger.error("[DynamicServiceLoader] Load error serviceId={}: {}", serviceId, e.getMessage());
            return false;
        }
    }

    /** Force reload a service (e.g. after JAR update). */
    public void reload(int serviceId) {
        instanceCache.remove(serviceId);
        methodCache.remove(serviceId);
        logger.info("[DynamicServiceLoader] Cleared cache for serviceId={}", serviceId);
    }
}
