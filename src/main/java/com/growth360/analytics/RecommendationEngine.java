package com.growth360.analytics;

import com.growth360.model.Alert;
import com.growth360.model.Recommendation;
import com.growth360.model.Recommendation.Priority;

import java.util.ArrayList;
import java.util.List;

/**
 * Prescriptive Decision Engine that maps operational alerts and statistical anomalies
 * into actionable business recommendations.
 */
public class RecommendationEngine {

    public List<Recommendation> generateRecommendations(List<Alert> alerts) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (alerts == null) return recommendations;

        for (Alert alert : alerts) {
            switch (alert.type()) {
                case INVENTORY -> {
                    if (alert.severity() == Alert.Severity.CRITICAL) {
                        recommendations.add(new Recommendation(
                                "REC-INV-" + alert.id(),
                                alert.id(),
                                "Horology Inventory Supply Chain",
                                "Expedite Assembly & Boutique Restock",
                                "Fast-track assembly at the Hosur watch facility and expedite boutique shipment to prevent lost high-ticket sales.",
                                Priority.URGENT,
                                "Prevents out-of-stock lost sales and protects Titan boutique brand prestige."
                        ));
                    } else if (alert.severity() == Alert.Severity.WARNING) {
                        recommendations.add(new Recommendation(
                                "REC-INV-" + alert.id(),
                                alert.id(),
                                "Inventory Planning",
                                "Trigger Reorder Point Batch Order",
                                "Generate standard replenishment batch order for watch movements and casings before lead time expires.",
                                Priority.HIGH,
                                "Ensures boutique display continuity without incurring expedited freight costs."
                        ));
                    } else if (alert.title().contains("Overstock")) {
                        recommendations.add(new Recommendation(
                                "REC-INV-" + alert.id(),
                                alert.id(),
                                "Merchandising Strategy",
                                "Gift-with-Purchase (GWP) Bundle Promo",
                                "Bundle slow-moving accessories as complimentary festive gifts with premium Titan Edge or Octane purchases.",
                                Priority.MEDIUM,
                                "Liberates tied-up working capital and enhances customer perceived value."
                        ));
                    }
                }
                case EXPENSE -> {
                    Priority prio = alert.severity() == Alert.Severity.CRITICAL ? Priority.URGENT : Priority.HIGH;
                    recommendations.add(new Recommendation(
                            "REC-EXP-" + alert.id(),
                            alert.id(),
                            "Financial Control",
                            "Audit Festive Marketing ROI & Spend",
                            "Audit media agency billings, celebrity endorsement fees, and boutique promotional costs against actual sales lift.",
                            prio,
                            "Curtails promotional leakage and optimizes Return on Ad Spend (ROAS)."
                    ));
                }
                case REVENUE -> {
                    recommendations.add(new Recommendation(
                            "REC-REV-" + alert.id(),
                            alert.id(),
                            "Boutique Revenue Recovery",
                            "Launch Festive Exchange & Upgrade Drive",
                            "Deploy old-watch exchange incentives and corporate gifting outreach to boost boutique footfall.",
                            Priority.HIGH,
                            "Stabilizes monthly cash flow and meets regional quarterly revenue targets."
                    ));
                }
                case CUSTOMER -> {
                    recommendations.add(new Recommendation(
                            "REC-CUST-" + alert.id(),
                            alert.id(),
                            "Encircle Loyalty Retention",
                            "Deploy Encircle Bonus Points & VIP Preview",
                            "Send personalized invitation for private boutique preview of new Nebula collections with 500 bonus Encircle points.",
                            Priority.HIGH,
                            "Reactivates high-value luxury collectors and safeguards Lifetime Value (LTV)."
                    ));
                }
            }
        }

        // If no urgent recommendations, provide general proactive advice
        if (recommendations.isEmpty()) {
            recommendations.add(new Recommendation(
                    "REC-OPT-GENERAL",
                    "NONE",
                    "Strategic Growth",
                    "Upsell to Champion Customer Cohort",
                    "Reward Champion tier customers with early access to upcoming product releases or VIP tier pricing.",
                    Priority.LOW,
                    "Maximizes repeat purchase velocity and boosts customer lifetime value."
            ));
        }

        return recommendations;
    }
}
