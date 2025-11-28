package com.frog.common.security.stepup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Component
@Slf4j
public class StepUpPolicyLoader {
    private final StepUpProperties properties;
    private volatile StepUpPolicy cached;
    private volatile long loadedAt = 0L;

    public StepUpPolicyLoader(StepUpProperties properties) {
        this.properties = properties;
    }

    public StepUpPolicy getPolicy() {
        long now = System.currentTimeMillis();
        StepUpPolicy snap = cached;
        if (snap != null && (now - loadedAt) < Math.max(5, properties.getRefreshSeconds()) * 1000L) {
            return snap;
        }
        synchronized (this) {
            if (cached != null && (now - loadedAt) < Math.max(5, properties.getRefreshSeconds()) * 1000L) {
                return cached;
            }
            cached = loadInternal();
            loadedAt = System.currentTimeMillis();
            return cached;
        }
    }

    private StepUpPolicy loadInternal() {
        try {
            Resource explicit = null;
            if (properties.getPolicyPath() != null && !properties.getPolicyPath().isBlank()) {
                explicit = new FileSystemResource(properties.getPolicyPath());
            }
            Resource[] candidates = new Resource[] {
                    explicit,
                    new ClassPathResource("security/stepup-policy.yaml"),
                    new FileSystemResource("docs/security/stepup-policy.yaml")
            };
            for (Resource r : candidates) {
                if (r != null && r.exists()) {
                    try (InputStream is = r.getInputStream()) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> map = yaml.load(is);
                        return mapToPolicy(map);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load step-up policy: {}", e.getMessage());
        }
        return null;
    }

    private StepUpPolicy mapToPolicy(Map<String, Object> map) {
        if (map == null) return null;
        StepUpPolicy policy = new StepUpPolicy();
        StepUpPolicy.Stepup step = new StepUpPolicy.Stepup();
        policy.setStepup(step);
        Object stepup = map.get("stepup");
        if (stepup instanceof Map<?,?> s) {
            Object triggers = s.get("triggers");
            if (triggers instanceof java.util.List<?> list) {
                java.util.List<StepUpPolicy.Trigger> ts = new java.util.ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?,?> tm) {
                        StepUpPolicy.Trigger t = new StepUpPolicy.Trigger();
                        Object action = tm.get("action");
                        Object require = tm.get("require");
                        Object conditions = tm.get("conditions");
                        if (action != null) t.setAction(action.toString());
                        if (require != null) t.setRequire(require.toString());
                        if (conditions instanceof java.util.List<?> cl) {
                            t.setConditions(cl.stream().map(String::valueOf).toList());
                        }
                        ts.add(t);
                    }
                }
                step.setTriggers(ts);
            }
        }
        return policy;
    }
}
