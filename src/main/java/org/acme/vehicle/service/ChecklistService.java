package org.acme.vehicle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.*;

@ApplicationScoped
public class ChecklistService {
    private static final Logger LOG = Logger.getLogger(ChecklistService.class);

    private final Set<String> validConditions = new HashSet<>(
            Arrays.asList("GOOD", "MINOR_DAMAGE", "MAJOR_DAMAGE", "NOT_INSPECTABLE")
    );

    private final Set<String> validStatuses = new HashSet<>(
            Arrays.asList("DRAFT", "SUBMITTED", "APPROVED", "RECHECK")
    );

    private final Set<String> carPointCodes = new LinkedHashSet<>();
    private final Map<String, String> pointToSectionMap = new HashMap<>();

    @Inject
    ObjectMapper objectMapper;

    void onStart(@Observes StartupEvent ev) {
        loadChecklist();
    }

    public synchronized void loadChecklist() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("checklist.json")) {
            if (is == null) {
                LOG.error("checklist.json not found in classpath!");
                return;
            }
            JsonNode root = objectMapper.readTree(is);
            JsonNode carPoints = root.path("vehicle_types").path("CAR");
            if (carPoints.isArray()) {
                carPointCodes.clear();
                pointToSectionMap.clear();
                for (JsonNode point : carPoints) {
                    String code = point.path("point_code").asText();
                    String section = point.path("section").asText();
                    carPointCodes.add(code);
                    pointToSectionMap.put(code, section);
                }
            }
            LOG.infof("Loaded %d checklist points for CAR from checklist.json", carPointCodes.size());
        } catch (Exception e) {
            LOG.error("Failed to load checklist.json", e);
        }
    }

    public boolean isValidPointCode(String pointCode) {
        if (carPointCodes.isEmpty()) {
            loadChecklist();
        }
        return pointCode != null && carPointCodes.contains(pointCode);
    }

    public boolean isValidCondition(String condition) {
        return condition != null && validConditions.contains(condition);
    }

    public boolean isValidStatus(String status) {
        return status != null && validStatuses.contains(status);
    }

    public boolean isEngineSection(String pointCode) {
        if (pointToSectionMap.isEmpty()) {
            loadChecklist();
        }
        return "ENGINE".equalsIgnoreCase(pointToSectionMap.get(pointCode));
    }

    public Set<String> getAllCarPointCodes() {
        if (carPointCodes.isEmpty()) {
            loadChecklist();
        }
        return Collections.unmodifiableSet(carPointCodes);
    }

    public String getSection(String pointCode) {
        if (pointToSectionMap.isEmpty()) {
            loadChecklist();
        }
        return pointToSectionMap.get(pointCode);
    }
}

