package com.growth360.analytics;

import com.growth360.model.Alert;
import com.growth360.model.Alert.AlertType;
import com.growth360.model.Alert.Severity;
import com.growth360.model.Customer;
import com.growth360.model.RfmResult;
import com.growth360.model.RfmResult.Segment;
import com.growth360.model.Sale;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure Java Customer Intelligence engine computing Recency, Frequency, Monetary (RFM) quintiles and segments.
 */
public class RfmSegmenter {

    private record RawCustomerStats(Customer customer, long recencyDays, int frequency, double monetary) {}

    public List<RfmResult> segmentCustomers(List<Customer> customers, List<Sale> sales) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        // Group sales by customer ID
        Map<Integer, List<Sale>> salesByCustomer = sales.stream()
                .collect(Collectors.groupingBy(Sale::getCustomerId));

        List<RawCustomerStats> rawStatsList = new ArrayList<>();

        for (Customer c : customers) {
            List<Sale> customerSales = salesByCustomer.getOrDefault(c.getId(), Collections.emptyList());

            long recencyDays;
            if (customerSales.isEmpty()) {
                // If never bought, recency relative to account creation date or 365 days
                recencyDays = Math.max(30, Duration.between(c.getCreatedAt(), now).toDays());
            } else {
                LocalDateTime latest = customerSales.stream()
                        .map(Sale::getSaleDate)
                        .max(LocalDateTime::compareTo)
                        .orElse(c.getCreatedAt());
                recencyDays = Math.max(0, Duration.between(latest, now).toDays());
            }

            int frequency = customerSales.size();
            double monetary = customerSales.stream().mapToDouble(Sale::getAmount).sum();

            rawStatsList.add(new RawCustomerStats(c, recencyDays, frequency, monetary));
        }

        // Rank into quintiles (1 to 5)
        // For Recency: lower days is better -> rank ascending
        List<RawCustomerStats> sortedByRecency = rawStatsList.stream()
                .sorted(Comparator.comparingLong(RawCustomerStats::recencyDays))
                .toList();

        // For Frequency: higher count is better -> rank ascending for score assignment
        List<RawCustomerStats> sortedByFreq = rawStatsList.stream()
                .sorted(Comparator.comparingInt(RawCustomerStats::frequency))
                .toList();

        // For Monetary: higher amount is better -> rank ascending
        List<RawCustomerStats> sortedByMonetary = rawStatsList.stream()
                .sorted(Comparator.comparingDouble(RawCustomerStats::monetary))
                .toList();

        int total = rawStatsList.size();
        Map<Integer, Integer> rScores = new HashMap<>();
        Map<Integer, Integer> fScores = new HashMap<>();
        Map<Integer, Integer> mScores = new HashMap<>();

        for (int i = 0; i < total; i++) {
            RawCustomerStats rStat = sortedByRecency.get(i);
            int rScore;
            if (rStat.recencyDays() <= 14) {
                rScore = 5;
            } else if (rStat.recencyDays() <= 30) {
                rScore = 4;
            } else if (rStat.recencyDays() <= 60) {
                rScore = 3;
            } else if (rStat.recencyDays() <= 90) {
                rScore = 2;
            } else {
                rScore = 1;
            }
            // Blend with rank
            int rankR = 5 - (int) ((double) i / total * 5);
            int finalR = Math.min(rScore, Math.max(1, Math.min(5, rankR)));
            rScores.put(rStat.customer().getId(), finalR);

            RawCustomerStats fStat = sortedByFreq.get(i);
            int fScore;
            if (fStat.frequency() >= 10) fScore = 5;
            else if (fStat.frequency() >= 6) fScore = 4;
            else if (fStat.frequency() >= 3) fScore = 3;
            else if (fStat.frequency() >= 2) fScore = 2;
            else fScore = 1;
            int rankF = 1 + (int) ((double) i / total * 5);
            fScores.put(fStat.customer().getId(), Math.max(fScore, Math.max(1, Math.min(5, rankF))));

            RawCustomerStats mStat = sortedByMonetary.get(i);
            int rankM = 1 + (int) ((double) i / total * 5);
            mScores.put(mStat.customer().getId(), Math.max(1, Math.min(5, rankM)));
        }

        List<RfmResult> results = new ArrayList<>();
        for (RawCustomerStats raw : rawStatsList) {
            int cid = raw.customer().getId();
            int r = rScores.getOrDefault(cid, 3);
            int f = fScores.getOrDefault(cid, 3);
            int m = mScores.getOrDefault(cid, 3);

            Segment segment = determineSegment(r, f, m);

            results.add(new RfmResult(
                    cid,
                    raw.customer().getName(),
                    raw.recencyDays(),
                    raw.frequency(),
                    raw.monetary(),
                    r, f, m,
                    segment
            ));
        }

        return results;
    }

    private Segment determineSegment(int r, int f, int m) {
        if (r >= 4 && f >= 4 && m >= 4) {
            return Segment.CHAMPIONS;
        } else if (r >= 3 && f >= 3) {
            return Segment.LOYAL;
        } else if (r >= 4 && (f <= 2 || m <= 3)) {
            return Segment.POTENTIAL;
        } else if (r <= 2 && (f >= 3 || m >= 3)) {
            return Segment.AT_RISK;
        } else {
            return Segment.LOST;
        }
    }

    public List<Alert> generateCustomerAlerts(List<RfmResult> rfmResults) {
        List<Alert> alerts = new ArrayList<>();
        long atRiskCount = rfmResults.stream().filter(r -> r.segment() == Segment.AT_RISK).count();
        long championsCount = rfmResults.stream().filter(r -> r.segment() == Segment.CHAMPIONS).count();

        if (atRiskCount > 0) {
            double atRiskRevenue = rfmResults.stream()
                    .filter(r -> r.segment() == Segment.AT_RISK)
                    .mapToDouble(RfmResult::monetary)
                    .sum();

            alerts.add(new Alert(
                    "CUST-ATRISK-COUNT",
                    AlertType.CUSTOMER,
                    Severity.WARNING,
                    "High-Value Customers At Risk",
                    String.format("%d formerly active customers (representing $%.2f historical revenue) are lapsing.",
                            atRiskCount, atRiskRevenue),
                    atRiskCount,
                    LocalDateTime.now()
            ));
        }

        return alerts;
    }
}
