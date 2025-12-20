package com.codix.tools;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class JiraService {

    private final String jiraUrl;
    private final String token;
    private final OkHttpClient client;

    private static final DateTimeFormatter JIRA_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 1. MAPPING DOMAINES (Existant)
    private static final Map<String, String> THEME_MAPPING = new HashMap<>();

    static {
        THEME_MAPPING.put("AD", "BI");
        THEME_MAPPING.put("ELLISPHERE", "IT");
        THEME_MAPPING.put("GED", "IT");
        THEME_MAPPING.put("TH1", "GP");
        THEME_MAPPING.put("TH10", "COMPTA");
        THEME_MAPPING.put("TH11", "BO");
        THEME_MAPPING.put("TH12", "BO");
        THEME_MAPPING.put("TH13", "BO");
        THEME_MAPPING.put("TH14", "BO");
        THEME_MAPPING.put("TH16_API", "BOARD");
        THEME_MAPPING.put("TH16_Interfaces", "IT");
        THEME_MAPPING.put("TH17_Migration", "MIGRATION");
        THEME_MAPPING.put("TH18", "IT");
        THEME_MAPPING.put("TH19", "BO");
        THEME_MAPPING.put("TH2", "GP");
        THEME_MAPPING.put("TH20", "PDF");
        THEME_MAPPING.put("TH3", "BO");
        THEME_MAPPING.put("TH4", "BO");
        THEME_MAPPING.put("TH5.1", "BO");
        THEME_MAPPING.put("TH5.2", "GP");
        THEME_MAPPING.put("TH6.1", "GP");
        THEME_MAPPING.put("TH6.2", "GP");
        THEME_MAPPING.put("TH6.3", "GP");
        THEME_MAPPING.put("TH7", "BO");
        THEME_MAPPING.put("TH8", "GP");
        THEME_MAPPING.put("TRANSVERSE", "TRANSVERSE");
    }

    // 2. NOUVEAU MAPPING FONCTIONNEL
    private static final Map<String, String> FUNCTIONAL_MAPPING = new HashMap<>();

    static {
        FUNCTIONAL_MAPPING.put("AD", "BI");
        FUNCTIONAL_MAPPING.put("ELLISPHERE", "INTERFACE");
        FUNCTIONAL_MAPPING.put("GED", "INTERFACE");
        FUNCTIONAL_MAPPING.put("TH1", "TH1 - Structure du leasing / paramétrage");
        FUNCTIONAL_MAPPING.put("TH10", "COMPTA");
        FUNCTIONAL_MAPPING.put("TH11", "TH11 - Recouvrement AMI et CTX");
        FUNCTIONAL_MAPPING.put("TH12", "TH13 - Facturation"); // Regroupé avec TH13
        FUNCTIONAL_MAPPING.put("TH13", "TH13 - Facturation");
        FUNCTIONAL_MAPPING.put("TH14", "TH14 - Paiement et SEPA");
        FUNCTIONAL_MAPPING.put("TH16_API", "BOARD");
        FUNCTIONAL_MAPPING.put("TH16_Interfaces", "INTERFACE");
        FUNCTIONAL_MAPPING.put("TH17_Migration", "MIGRATION");
        FUNCTIONAL_MAPPING.put("TH18", "IT");
        FUNCTIONAL_MAPPING.put("TH19", "TH19 - Extranet client");
        FUNCTIONAL_MAPPING.put("TH2", "TH2 - Individus");
        FUNCTIONAL_MAPPING.put("TH20", "Intégration PDF");
        FUNCTIONAL_MAPPING.put("TH3", "TH3 - OFI");
        FUNCTIONAL_MAPPING.put("TH4", "TH4 - Scoring");
        FUNCTIONAL_MAPPING.put("TH5.1", "TH5.1 - Mise en Financement");
        FUNCTIONAL_MAPPING.put("TH5.2", "TH5.2 - Reversement");
        FUNCTIONAL_MAPPING.put("TH6.1", "TH6.1 - Entrée En Relation Client");
        FUNCTIONAL_MAPPING.put("TH6.2", "TH6.2 - Pilotage Engagements Partenaires");
        FUNCTIONAL_MAPPING.put("TH6.3", "TH6.3 - Recouvrement Partenaire");
        FUNCTIONAL_MAPPING.put("TH7", "TH7 - Après-vente / Fin contrat");
        FUNCTIONAL_MAPPING.put("TH8", "TH8 - Prospection, CRM, Xnet réseau");
        FUNCTIONAL_MAPPING.put("TRANSVERSE", "TRANSVERSE");
    }

    public static final List<String> CODIX_CATEGORIES = Arrays.asList(
            "Action Point", "Assistance", "Adjustment", "Defect",
            "Evolution", "Incident", "Information", "Question", "Under Classification"
    );

    public enum MetricType {
        STOCK("Ouverts", false),
        CREATED("Créés", false),
        CLOSED("Clos", true),
        DELIVERED("Livrés", true);

        public final String label;
        public final boolean higherIsBetter;

        MetricType(String label, boolean higherIsBetter) {
            this.label = label;
            this.higherIsBetter = higherIsBetter;
        }
    }

    public JiraService(String jiraUrl, String token) {
        this.jiraUrl = jiraUrl;
        this.token = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    // Récupère les stats pour le Tableau 1 (Domaines classiques)
    public Map<String, Map<String, Integer>> getCurrentDomainStats(String baseJql) throws IOException {
        System.out.println("Analyse du stock actuel par domaine...");
        return getStatsByMapping(baseJql, THEME_MAPPING);
    }

    // Récupère les stats pour le NOUVEAU Tableau (Domaines Fonctionnels)
    public Map<String, Map<String, Integer>> getCurrentFunctionalStats(String baseJql) throws IOException {
        System.out.println("Analyse du stock actuel par domaine fonctionnel...");
        return getStatsByMapping(baseJql, FUNCTIONAL_MAPPING);
    }

    private Map<String, Map<String, Integer>> getStatsByMapping(String baseJql, Map<String, String> mapping) throws IOException {
        JSONObject searchResult = searchJira(baseJql, 1000, null);
        Map<String, Map<String, Integer>> stats = new TreeMap<>();

        // Initialisation des thèmes existants dans le mapping
        for (String val : new TreeSet<>(mapping.values())) {
            stats.put(val, createActorMap());
        }
        // Ajout d'une catégorie "AUTRES" pour pointer les tickets sans thème (différences)
        stats.put("AUTRES", createActorMap());

        if (searchResult == null) {
            return stats;
        }

        // Statuts à exclure selon ta demande
        List<String> excluded = Arrays.asList("Closed", "Cancel", "Pending");

        JSONArray issues = searchResult.getJSONArray("issues");
        for (int i = 0; i < issues.length(); i++) {
            JSONObject fields = issues.getJSONObject(i).getJSONObject("fields");
            String status = fields.getJSONObject("status").getString("name");

            // 1. Filtrage des statuts exclus
            boolean isExcluded = false;
            for (String s : excluded) {
                if (s.equalsIgnoreCase(status)) {
                    isExcluded = true;
                    break;
                }
            }
            if (isExcluded) {
                continue;
            }

            // 2. Identification du thème
            JSONArray labelsJson = fields.optJSONArray("labels");
            String categoryFound = identifyCategory(labelsJson, mapping);

            // Si aucun label de thème n'est trouvé, on utilise "AUTRES"
            if (categoryFound == null) {
                categoryFound = "AUTRES";
            }

            Map<String, Integer> ds = stats.get(categoryFound);
            if (ds != null) {
                if ("Replied by CODIX".equalsIgnoreCase(status)) {
                    ds.put("LOCAM", ds.get("LOCAM") + 1);
                } else {
                    ds.put("Codix", ds.get("Codix") + 1);
                }
            }
        }
        return stats;
    }

// Helper pour initialiser la structure
    private Map<String, Integer> createActorMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("LOCAM", 0);
        m.put("Codix", 0);
        return m;
    }

    public HistoryData getHistoryMetrics(String baseJql) {
        System.out.println("Analyse historique Globale (8 dernières semaines)...");
        Map<Integer, Map<MetricType, Integer>> weeklyStats = new LinkedHashMap<>();
        List<String> weekLabels = new ArrayList<>();
        List<Integer> weekOrder = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String scopeJql = "project = LOCAMWEB AND labels = MEP2 AND type != CRQ";
        String excludedStats = "(Closed, Cancel, Pending)";

        for (int i = 7; i >= 0; i--) {
            LocalDate endOfWeek = today.minusWeeks(i).with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
            LocalDate startOfWeek = endOfWeek.minusDays(6);
            int weekNum = endOfWeek.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());

            weekLabels.add("S" + weekNum);
            weekOrder.add(weekNum);

            Map<MetricType, Integer> stats = new HashMap<>();
            String startStr = startOfWeek.format(JIRA_DATE_FMT);
            String endStr = endOfWeek.format(JIRA_DATE_FMT);
            // Le snapshot se fait au lundi matin 00h00 pour inclure tout le dimanche
            String snapshotDate = endOfWeek.plusDays(1).format(JIRA_DATE_FMT);

            String jqlStock;
            if (!endOfWeek.isBefore(today)) {
                // Semaine en cours : Recherche temps réel
                jqlStock = scopeJql + " AND status NOT IN " + excludedStats;
            } else {
                // Semaines passées : Snapshot + Condition sur la date de création
                jqlStock = scopeJql + " AND status WAS NOT IN " + excludedStats
                        + " ON \"" + snapshotDate + "\" AND created < \"" + snapshotDate + "\"";
            }

            stats.put(MetricType.STOCK, getCount(jqlStock));
            stats.put(MetricType.CREATED, getCount(scopeJql + " AND assignee changed to hotline DURING (\"" + startStr + "\", \"" + endStr + "\") AND NOT assignee changed to hotline BEFORE \"" + startStr + "\""));
            stats.put(MetricType.CLOSED, getCount(scopeJql + " AND \"Codix reply\" is not EMPTY AND status changed to Closed DURING (\"" + startStr + "\", \"" + endStr + "\")"));
            stats.put(MetricType.DELIVERED, countDeliveredViaChangelog(scopeJql, startOfWeek, endOfWeek));

            weeklyStats.put(weekNum, stats);
            System.out.print(".");
        }
        System.out.println("\nCalculs globaux terminés.");
        return new HistoryData(weeklyStats, weekLabels, weekOrder);
    }

    public CategoryHistoryData getCodixCategoryHistory(String baseJql) {
        System.out.println("Analyse historique par Catégorie (8 dernières semaines)...");
        Map<Integer, Map<String, Integer>> weeklyCatStats = new LinkedHashMap<>();
        List<String> weekLabels = new ArrayList<>();
        List<Integer> weekOrder = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String scopeJql = "project = LOCAMWEB AND labels = MEP2 AND type != CRQ";
        String excludedStats = "(Closed, Cancel, Pending)";

        for (int i = 7; i >= 0; i--) {
            LocalDate endOfWeek = today.minusWeeks(i).with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
            int weekNum = endOfWeek.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());

            weekLabels.add("S" + weekNum);
            weekOrder.add(weekNum);

            String snapshotDate = endOfWeek.plusDays(1).format(JIRA_DATE_FMT);

            String stockAtDateJql;
            if (!endOfWeek.isBefore(today)) {
                stockAtDateJql = scopeJql + " AND status NOT IN " + excludedStats;
            } else {
                stockAtDateJql = scopeJql + " AND status WAS NOT IN " + excludedStats
                        + " ON \"" + snapshotDate + "\" AND created < \"" + snapshotDate + "\"";
            }

            Map<String, Integer> catStats = new HashMap<>();
            for (String category : CODIX_CATEGORIES) {
                String finalJql = "Under Classification".equals(category)
                        ? stockAtDateJql + " AND (\"Codix Category\" is EMPTY OR \"Codix Category\" = \"Under Classification\")"
                        : stockAtDateJql + " AND \"Codix Category\" = \"" + category + "\"";
                catStats.put(category, getCount(finalJql));
            }
            weeklyCatStats.put(weekNum, catStats);
            System.out.print(".");
        }
        System.out.println("\nCalculs catégories terminés.");
        return new CategoryHistoryData(weeklyCatStats, weekLabels, weekOrder);
    }

    private int countDeliveredViaChangelog(String coreJql, LocalDate start, LocalDate end) {
        int count = 0;
        try {
            String jql = coreJql + " AND updated >= \"" + start.format(JIRA_DATE_FMT) + "\" AND updated <= \"" + end.format(JIRA_DATE_FMT) + "\"";
            JSONObject result = searchJira(jql, 1000, "changelog");
            if (result == null) {
                return 0;
            }

            JSONArray issues = result.getJSONArray("issues");
            LocalDateTime startDt = start.atStartOfDay();
            LocalDateTime endDt = end.atTime(23, 59, 59);

            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                if (!issue.has("changelog")) {
                    continue;
                }

                JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
                boolean delivered = false;

                for (int h = 0; h < histories.length(); h++) {
                    JSONObject history = histories.getJSONObject(h);
                    String createdStr = history.getString("created");
                    LocalDateTime changeDate;
                    try {
                        changeDate = LocalDateTime.parse(createdStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                    } catch (Exception ex) {
                        continue;
                    }

                    if (changeDate.isAfter(startDt) && changeDate.isBefore(endDt)) {
                        JSONArray items = history.getJSONArray("items");
                        for (int k = 0; k < items.length(); k++) {
                            JSONObject item = items.getJSONObject(k);
                            String fieldName = item.optString("field", "");
                            String toString = item.optString("toString", "");
                            if ("Statut Codix".equalsIgnoreCase(fieldName) && "Livrée".equalsIgnoreCase(toString)) {
                                delivered = true;
                                break;
                            }
                        }
                    }
                    if (delivered) {
                        break;
                    }
                }
                if (delivered) {
                    count++;
                }
            }
        } catch (Exception e) {
            System.err.println("Err Changelog: " + e.getMessage());
        }
        return count;
    }

    private int getCount(String jql) {
        try {
            JSONObject json = searchJira(jql, 0, null);
            return json != null ? json.getInt("total") : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private JSONObject searchJira(String jql, int maxResults, String expand) throws IOException {
        String url = jiraUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&maxResults=" + maxResults + "&fields=status,labels";
        if (expand != null) {
            url += "&expand=" + expand;
        }
        Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            return new JSONObject(response.body().string());
        }
    }

    // Identifie la catégorie selon le mapping fourni
    private String identifyCategory(JSONArray labels, Map<String, String> mapping) {
        if (labels == null) {
            return null;
        }
        for (int j = 0; j < labels.length(); j++) {
            String l = labels.getString(j);
            if (mapping.containsKey(l)) {
                return mapping.get(l);
            }
        }
        return null;
    }

    public static class HistoryData {

        public final Map<Integer, Map<MetricType, Integer>> stats;
        public final List<String> labels;
        public final List<Integer> order;

        public HistoryData(Map<Integer, Map<MetricType, Integer>> stats, List<String> labels, List<Integer> order) {
            this.stats = stats;
            this.labels = labels;
            this.order = order;
        }
    }

    public static class CategoryHistoryData {

        public final Map<Integer, Map<String, Integer>> stats;
        public final List<String> labels;
        public final List<Integer> order;

        public CategoryHistoryData(Map<Integer, Map<String, Integer>> stats, List<String> labels, List<Integer> order) {
            this.stats = stats;
            this.labels = labels;
            this.order = order;
        }
    }

    public Map<String, Map<String, Integer>> getCurrentThemeStats(String baseJql) throws IOException {
        Map<String, String> themeMapping = new LinkedHashMap<>();
        String[] themes = {"AD", "ELLISPHERE", "GED", "TH1", "TH10", "TH11", "TH12", "TH13", "TH14", "TH16_API",
            "TH16_Interfaces", "TH17_Migration", "TH18", "TH19", "TH2", "TH20", "TH3", "TH4",
            "TH5.1", "TH5.2", "TH6.1", "TH6.2", "TH6.3", "TH7", "TH8", "TRANSVERSE"};
        for (String t : themes) {
            themeMapping.put(t, t);
        }
        return getStatsByMapping(baseJql, themeMapping);
    }
}
