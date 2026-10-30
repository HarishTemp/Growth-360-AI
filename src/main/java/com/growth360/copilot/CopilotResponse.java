package com.growth360.copilot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable Java Record representing an intelligent response synthesized
 * by the Growth 360 AI Copilot reasoning engine.
 */
public record CopilotResponse(
        String userQuery,
        String headline,
        String narrative,
        List<String> keyMetrics,
        List<String> suggestedActions,
        List<String> followUpPrompts,
        Urgency urgency,
        LocalDateTime timestamp
) {
    public enum Urgency {
        NORMAL,
        WATCH,
        HIGH,
        CRITICAL
    }

    public static CopilotResponse of(
            String userQuery,
            String headline,
            String narrative,
            List<String> keyMetrics,
            List<String> suggestedActions,
            List<String> followUpPrompts,
            Urgency urgency
    ) {
        return new CopilotResponse(
                userQuery,
                headline,
                narrative,
                keyMetrics,
                suggestedActions,
                followUpPrompts,
                urgency,
                LocalDateTime.now()
        );
    }
}
