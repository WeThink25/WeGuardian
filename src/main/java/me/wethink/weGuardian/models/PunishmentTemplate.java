package me.wethink.weGuardian.models;

import java.util.List;
import java.util.Map;

public class PunishmentTemplate {
    private String name;
    private String category;
    private List<EscalationLevel> escalationLevels;
    private boolean enabled;
    private Map<String, String> variables;

    public PunishmentTemplate(String name, String category, List<EscalationLevel> escalationLevels) {
        this.name = name;
        this.category = category;
        this.escalationLevels = escalationLevels;
        this.enabled = true;
    }

    public EscalationLevel getEscalationLevel(int currentOffenses) {
        if (escalationLevels.isEmpty()) return null;

        EscalationLevel selectedLevel = escalationLevels.get(0);
        for (EscalationLevel level : escalationLevels) {
            if (currentOffenses >= level.getLevel()) {
                selectedLevel = level;
            } else {
                break;
            }
        }
        return selectedLevel;
    }

    public String processReason(String baseReason, Map<String, String> placeholders) {
        String processedReason = baseReason;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processedReason = processedReason.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return processedReason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<EscalationLevel> getEscalationLevels() {
        return escalationLevels;
    }

    public void setEscalationLevels(List<EscalationLevel> escalationLevels) {
        this.escalationLevels = escalationLevels;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public static class EscalationLevel {
        private int level;
        private PunishmentType type;
        private String duration;
        private String reason;
        private boolean permanent;

        public EscalationLevel(int level, PunishmentType type, String duration, String reason) {
            this.level = level;
            this.type = type;
            this.duration = duration;
            this.reason = reason;
            this.permanent = duration == null || duration.equalsIgnoreCase("permanent");
        }

        public int getLevel() {
            return level;
        }

        public PunishmentType getType() {
            return type;
        }

        public String getDuration() {
            return duration;
        }

        public String getReason() {
            return reason;
        }

        public boolean isPermanent() {
            return permanent;
        }
    }
}
