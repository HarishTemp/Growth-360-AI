package com.growth360.util;

/**
 * Design system tokens and inline style constants for the pure-Java JavaFX UI.
 * Zero external CSS files are used, strictly adhering to the pure-Java requirement.
 */
public final class Theme {

    private Theme() {}

    // Color Palette Tokens
    public static final String COLOR_BG_APP = "#0A0E17";
    public static final String COLOR_BG_SIDEBAR = "#0F172A";
    public static final String COLOR_BG_CARD = "#162032";
    public static final String COLOR_BG_CARD_HOVER = "#1E2C44";
    public static final String COLOR_BG_INPUT = "#111827";
    public static final String COLOR_BORDER = "#1E293B";
    public static final String COLOR_BORDER_LIGHT = "#334155";
    public static final String COLOR_BORDER_FOCUS = "#38BDF8";

    // Accent Colors
    public static final String COLOR_ACCENT_PRIMARY = "#38BDF8";      // Sky Cyan
    public static final String COLOR_ACCENT_GRADIENT = "linear-gradient(to right, #0EA5E9, #6366F1)";
    public static final String COLOR_ACCENT_PURPLE = "#818CF8";
    public static final String COLOR_SUCCESS = "#10B981";             // Emerald
    public static final String COLOR_WARNING = "#F59E0B";             // Amber
    public static final String COLOR_DANGER = "#EF4444";              // Rose Red
    public static final String COLOR_INFO = "#06B6D4";                // Cyan

    // Text Colors
    public static final String TEXT_PRIMARY = "#F8FAFC";
    public static final String TEXT_SECONDARY = "#94A3B8";
    public static final String TEXT_MUTED = "#64748B";

    // Currency Formatting for Titan Company
    public static final String CURRENCY_SYMBOL = "₹";

    // Font Family
    public static final String FONT_FAMILY = "font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;";

    // UI Styles
    public static final String ROOT_STYLE =
            "-fx-background-color: " + COLOR_BG_APP + "; " +
            FONT_FAMILY;

    public static final String SIDEBAR_STYLE =
            "-fx-background-color: " + COLOR_BG_SIDEBAR + "; " +
            "-fx-border-color: " + COLOR_BORDER + "; " +
            "-fx-border-width: 0 1 0 0;";

    public static final String HEADER_STYLE =
            "-fx-background-color: " + COLOR_BG_SIDEBAR + "; " +
            "-fx-border-color: " + COLOR_BORDER + "; " +
            "-fx-border-width: 0 0 1 0; " +
            "-fx-padding: 16 28 16 28;";

    public static final String CONTENT_AREA_STYLE =
            "-fx-background-color: " + COLOR_BG_APP + "; " +
            "-fx-padding: 24 32 32 32;";

    public static final String CARD_STYLE =
            "-fx-background-color: " + COLOR_BG_CARD + "; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: " + COLOR_BORDER + "; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 20px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0, 0, 4);";

    public static final String STAT_CARD_STYLE =
            "-fx-background-color: linear-gradient(to bottom right, #162032, #0F172A); " +
            "-fx-background-radius: 14px; " +
            "-fx-border-color: " + COLOR_BORDER_LIGHT + "; " +
            "-fx-border-radius: 14px; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 22px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 5);";

    public static final String NAV_BUTTON_DEFAULT =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + TEXT_SECONDARY + "; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 500; " +
            "-fx-alignment: BASELINE_LEFT; " +
            "-fx-padding: 12 18 12 18; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand;";

    public static final String NAV_BUTTON_ACTIVE =
            "-fx-background-color: rgba(56, 189, 248, 0.12); " +
            "-fx-text-fill: " + COLOR_ACCENT_PRIMARY + "; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: BASELINE_LEFT; " +
            "-fx-padding: 12 18 12 18; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: transparent transparent transparent " + COLOR_ACCENT_PRIMARY + "; " +
            "-fx-border-width: 0 0 0 4px; " +
            "-fx-cursor: hand;";

    public static final String BTN_PRIMARY =
            "-fx-background-color: linear-gradient(to right, #0284C7, #2563EB); " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 18 9 18; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(14,165,233,0.35), 8, 0, 0, 2);";

    public static final String BTN_SUCCESS =
            "-fx-background-color: #059669; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 18 9 18; " +
            "-fx-cursor: hand;";

    public static final String BTN_DANGER =
            "-fx-background-color: #DC2626; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 18 9 18; " +
            "-fx-cursor: hand;";

    public static final String BTN_SECONDARY =
            "-fx-background-color: #1E293B; " +
            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
            "-fx-border-color: " + COLOR_BORDER_LIGHT + "; " +
            "-fx-border-radius: 8px; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 500; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand;";

    public static final String TEXT_INPUT =
            "-fx-background-color: " + COLOR_BG_INPUT + "; " +
            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
            "-fx-prompt-text-fill: " + TEXT_MUTED + "; " +
            "-fx-border-color: " + COLOR_BORDER_LIGHT + "; " +
            "-fx-border-radius: 7px; " +
            "-fx-background-radius: 7px; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 8 12 8 12;";

    public static final String TABLE_VIEW =
            "-fx-background-color: " + COLOR_BG_CARD + "; " +
            "-fx-border-color: " + COLOR_BORDER + "; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-table-cell-border-color: transparent;";

    public static String getBadgeStyle(String bgHex, String textHex) {
        return "-fx-background-color: " + bgHex + "; " +
               "-fx-text-fill: " + textHex + "; " +
               "-fx-background-radius: 20px; " +
               "-fx-padding: 3 10 3 10; " +
               "-fx-font-size: 11px; " +
               "-fx-font-weight: bold;";
    }
}
