package com.growth360.model;

/**
 * Record representing RFM (Recency, Frequency, Monetary) segmentation analysis for a customer.
 */
public record RfmResult(
        int customerId,
        String customerName,
        long recencyDays,
        int frequency,
        double monetary,
        int rScore,
        int fScore,
        int mScore,
        Segment segment
) {
    public enum Segment {
        CHAMPIONS("Champions", "#10B981", "Highest value, buys frequently & recently"),
        LOYAL("Loyal Customers", "#3B82F6", "Consistent buyers with strong relationship"),
        POTENTIAL("Potential Loyalists", "#06B6D4", "Recent buyers with good spend potential"),
        AT_RISK("At-Risk", "#F59E0B", "Used to buy frequently, haven't purchased recently"),
        LOST("Lost / Inactive", "#EF4444", "Lowest recency and frequency, needs re-engagement");

        private final String displayName;
        private final String colorHex;
        private final String description;

        Segment(String displayName, String colorHex, String description) {
            this.displayName = displayName;
            this.colorHex = colorHex;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getColorHex() { return colorHex; }
        public String getDescription() { return description; }
    }
}
