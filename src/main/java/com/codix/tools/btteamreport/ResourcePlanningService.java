package com.codix.tools.btteamreport;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import okhttp3.*;
import javax.net.ssl.*;
import java.util.concurrent.TimeUnit;

public class ResourcePlanningService {

    private final String jiraUrl;
    private final String token;
    private final OkHttpClient client;
    private static final DateTimeFormatter JIRA_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double SECONDS_IN_MD = 25200.0;
    private static final String HR_URL = "https://hrcenter.codixfr.private/HR//index.php?tab=6";

    public static final Map<String, String> TARGET_USERS = new LinkedHashMap<>();

    static {
        TARGET_USERS.put("amirchev", "Aleksandar Mirchev");
        TARGET_USERS.put("amakki", "Amal Makki");
        TARGET_USERS.put("angeorgieva", "Anelia Georgieva");
        TARGET_USERS.put("eveli", "Emel Veli");
        TARGET_USERS.put("fsouab", "Farah Souab");
        TARGET_USERS.put("iatanasov", "Ivan Atanasov");
        TARGET_USERS.put("ikolchev", "Ivan Kolchev");
        TARGET_USERS.put("kmateeva", "Katya Mateeva");
        TARGET_USERS.put("msamareva", "Maria Samareva");
        TARGET_USERS.put("mniklenov", "Mihail Niklenov");
        TARGET_USERS.put("mmrabet", "Myriam Mrabet");
        TARGET_USERS.put("rgospodinova", "Ralitsa Gospodinova");
        TARGET_USERS.put("valmaleh", "Valeri Almaleh");
        TARGET_USERS.put("ayacoub", "Amal Yacoub");
        TARGET_USERS.put("atsirov", "Atanas Tsirov");
        TARGET_USERS.put("bnouaji", "Bilel Nouaji");
        TARGET_USERS.put("kslavchova", "Kameliya Slavchova");
        TARGET_USERS.put("kkomitov", "Kamen Komitov");
        TARGET_USERS.put("kbachvarova", "Katerina Bachvarova");
        TARGET_USERS.put("mdaaji", "Moez Daaji");
        TARGET_USERS.put("mhadji", "Mohamed Aymen Hadji");
        TARGET_USERS.put("ndelbecq", "Nicolas Delbecq");
        TARGET_USERS.put("rbensalem", "Riadh Ben Salem");
        TARGET_USERS.put("rtkhayat", "Rym Ben Tkhayat");
        TARGET_USERS.put("sabbassi", "Saber Abbassi");
        TARGET_USERS.put("sbraham", "Slim Haj Braham");
        TARGET_USERS.put("vrobert", "Valérie Robert");
        TARGET_USERS.put("wfadhloun", "Wafa Ben Fadhloun");
        TARGET_USERS.put("ypetrov", "Yordan Petrov");
    }

    public static final String[] ALL_THEMES = {
        "AD", "ELLISPHERE", "GED", "TH1", "TH10", "TH11", "TH12", "TH13", "TH14", "TH16_API",
        "TH16_Interfaces", "TH17_Migration", "TH18", "TH19", "TH2", "TH20", "TH3", "TH4",
        "TH5.1", "TH5.2", "TH6.1", "TH6.2", "TH6.3", "TH7", "TH8", "TRANSVERSE"
    };

    private static final Map<String, Double> THEME_MEDIANS = new HashMap<>();

    static {
        THEME_MEDIANS.put("AD", 0.07);
        THEME_MEDIANS.put("AUTRES", 0.24);
        THEME_MEDIANS.put("ELLISPHERE", 2.77);
        THEME_MEDIANS.put("GED", 0.52);
        THEME_MEDIANS.put("TH1", 0.71);
        THEME_MEDIANS.put("TH10", 0.64);
        THEME_MEDIANS.put("TH11", 0.60);
        THEME_MEDIANS.put("TH12", 0.06);
        THEME_MEDIANS.put("TH13", 0.39);
        THEME_MEDIANS.put("TH14", 0.93);
        THEME_MEDIANS.put("TH16_API", 0.60);
        THEME_MEDIANS.put("TH16_Interfaces", 0.62);
        THEME_MEDIANS.put("TH17_Migration", 0.31);
        THEME_MEDIANS.put("TH18", 0.18);
        THEME_MEDIANS.put("TH19", 0.32);
        THEME_MEDIANS.put("TH2", 0.48);
        THEME_MEDIANS.put("TH20", 0.65);
        THEME_MEDIANS.put("TH3", 0.68);
        THEME_MEDIANS.put("TH5.1", 0.39);
        THEME_MEDIANS.put("TH5.2", 0.33);
        THEME_MEDIANS.put("TH6.1", 0.28);
        THEME_MEDIANS.put("TH6.2", 0.51);
        THEME_MEDIANS.put("TH6.3", 0.85);
        THEME_MEDIANS.put("TH7", 0.51);
        THEME_MEDIANS.put("TH8", 0.27);
        THEME_MEDIANS.put("TRANSVERSE", 1.61);
    }

    private static final List<String> THEMES_LIST = Arrays.asList(
            "AD", "ELLISPHERE", "GED", "TH1", "TH2", "TH3", "TH4", "TH5.1", "TH5.2",
            "TH6.1", "TH6.2", "TH6.3", "TH7", "TH8", "TH10", "TH11", "TH12", "TH13",
            "TH14", "TH16_API", "TH16_Interfaces", "TH17_Migration", "TH18", "TH19",
            "TH20", "TRANSVERSE"
    );

    private static final Map<String, String> SPECIAL_MAPPINGS = new HashMap<>();

    static {
        SPECIAL_MAPPINGS.put("COMPTA", "TH10");
        SPECIAL_MAPPINGS.put("TH15", "TH10");
        SPECIAL_MAPPINGS.put("API", "TH16_API");
        SPECIAL_MAPPINGS.put("ALTECA", "TH16_API");
        SPECIAL_MAPPINGS.put("BOARD", "TH16_API");
        SPECIAL_MAPPINGS.put("XNET", "TH19");
        SPECIAL_MAPPINGS.put("EXTRANET", "TH19");
        SPECIAL_MAPPINGS.put("SAE", "GED");
        SPECIAL_MAPPINGS.put("TH.7", "TH7");
        SPECIAL_MAPPINGS.put("TH.11", "TH11");
        SPECIAL_MAPPINGS.put("ONLIZ", "TH20");
    }

    public ResourcePlanningService(String jiraUrl, String token) {
        this.jiraUrl = jiraUrl;
        this.token = token;
        this.client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();
    }

    public ReportData getReportData(String projectKeys, int nbWeeks) {
        ReportData report = new ReportData();
        report.planning = getPlanningData(projectKeys, nbWeeks);
        report.dashboard = getDashboardMetrics(projectKeys);
        try {
            report.themeStats = getThemeStats();
        } catch (IOException e) {
            e.printStackTrace();
            report.themeStats = new LinkedHashMap<>();
        }
        return report;
    }

    public CapacityAlerts calculateCapacityAlerts(String jql, Map<String, Map<String, Double>> specs, Map<String, Integer> absences, double workingDays) throws IOException {
        CapacityAlerts alerts = new CapacityAlerts();
        Map<String, Integer> themeStock = new HashMap<>();
        List<String> logsAutres = new ArrayList<>();

        // 1. Récupération du stock Jira
        JSONObject result = searchJira(jql, 0, 1000, null);
        if (result != null && result.has("issues")) {
            JSONArray issues = result.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                JSONObject fields = issue.getJSONObject("fields");

                // Utilisation de la logique d'identification (Extended conseillée)
                String theme = identifyThemeExtended(issue, this);

                if ("AUTRES".equals(theme)) {
                    logsAutres.add(issue.getString("key") + " : " + fields.optString("summary", "Sans titre"));
                }

                themeStock.put(theme, themeStock.getOrDefault(theme, 0) + 1);
            }
        }

        // Affichage des tickets AUTRES dans la console
        if (!logsAutres.isEmpty()) {
            System.out.println("\n--- TICKETS CLASSÉS EN 'AUTRES' (SANS LABEL NI MOT-CLÉ) ---");
            logsAutres.forEach(t -> System.out.println("  [?] " + t));
            System.out.println("-----------------------------------------------------------\n");
        }

        // 2. Calcul de la capacité par thème
        Map<String, Double> themeCapacity = new HashMap<>();
        specs.forEach((login, userSpecs) -> {
            double daysPresent = workingDays - absences.getOrDefault(login, 0);
            userSpecs.forEach((theme, pct) -> {
                themeCapacity.merge(theme, daysPresent * (pct / 100.0), Double::sum);
            });
        });

        List<SufferingTheme> allThemes = new ArrayList<>();
        themeStock.keySet().stream()
                .filter(t -> !"TH18".equals(t))
                .forEach(theme -> {
                    int count = themeStock.get(theme);
                    double mediane = THEME_MEDIANS.getOrDefault(theme, 0.4);
                    double chargeEstimee = count * mediane;
                    double capacite = themeCapacity.getOrDefault(theme, 0.0);

                    double tension = (capacite > 0) ? chargeEstimee / capacite : (chargeEstimee > 0 ? 99.0 : 0.0);
                    double extraResources = (chargeEstimee - capacite) / workingDays;

                    allThemes.add(new SufferingTheme(theme, tension, count, extraResources));

                    System.out.format("Thème: %-15s | Stock: %2d | Poids: %.2fj | Charge: %4.1fj | Capa: %4.1fj | Coef: %4.2fx | Extra: %+.2f res%n",
                            theme, count, mediane, chargeEstimee, capacite, tension, extraResources);
                });

        // 3. TOP 3 SURCHARGE
        alerts.underCapacity = allThemes.stream()
                .filter(t -> t.getTicketCount() >= 3 && (t.getTension() > 1.1 || t.getTension() == 99.0))
                .sorted(Comparator.comparingDouble(SufferingTheme::getExtraResources).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // 4. TOP 3 DISPONIBILITÉ
        alerts.overCapacity = allThemes.stream()
                .filter(t -> t.getTension() < 1.0)
                .sorted(Comparator.comparingDouble(SufferingTheme::getExtraResources))
                .limit(3)
                .collect(Collectors.toList());

        return alerts;
    }

    public String identifyTheme(JSONArray labels) {
        if (labels != null) {
            for (int i = 0; i < labels.length(); i++) {
                String label = labels.getString(i);
                for (String t : ALL_THEMES) {
                    if (t.equalsIgnoreCase(label)) {
                        return t;
                    }
                }
            }
        }
        return "AUTRES";
    }

    /**
     * Identifie le thème d'un ticket avec une logique étendue : 1. Labels JIRA
     * 2. Thème des tickets liés (is a prerequisite for / depends on issue) 3.
     * Mots-clés et patterns dans le titre (Summary)
     */
    private String identifyThemeExtended(JSONObject issue, ResourcePlanningService service) {
        JSONObject fields = issue.getJSONObject("fields");

        // 1. Recherche par labels (Logique standard)
        String theme = service.identifyTheme(fields.optJSONArray("labels"));
        if (!"AUTRES".equals(theme)) {
            return theme;
        }

        // 2. Recherche via les tickets liés (Dépendances)
        if (fields.has("issuelinks")) {
            JSONArray links = fields.getJSONArray("issuelinks");
            for (int j = 0; j < links.length(); j++) {
                JSONObject link = links.getJSONObject(j);
                JSONObject linkedIssue = null;

                // On cherche le ticket dont celui-ci dépend ou dont il est le prérequis
                if (link.has("outwardIssue") && "depends on issue".equals(link.getJSONObject("type").optString("outward"))) {
                    linkedIssue = link.getJSONObject("outwardIssue");
                } else if (link.has("inwardIssue") && "is a prerequisite for".equals(link.getJSONObject("type").optString("inward"))) {
                    linkedIssue = link.getJSONObject("inwardIssue");
                }

                if (linkedIssue != null && linkedIssue.has("fields")) {
                    String linkedTheme = service.identifyTheme(linkedIssue.getJSONObject("fields").optJSONArray("labels"));
                    if (!"AUTRES".equals(linkedTheme)) {
                        return linkedTheme;
                    }
                }
            }
        }

        // 3. Recherche par mots-clés dans le titre (Summary)
        String summary = fields.optString("summary", "").toUpperCase();

        // 3a. Recherche des thèmes standards (TH1, TH2, etc.)
        for (String t : THEMES_LIST) {
            if (summary.contains(t.toUpperCase())) {
                return t;
            }
        }

        // 3b. Recherche des mots-clés spéciaux (Logique VBA : COMPTA -> TH10, etc.)
        for (Map.Entry<String, String> entry : SPECIAL_MAPPINGS.entrySet()) {
            if (summary.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "AUTRES";
    }

    private Map<String, Map<String, Integer>> getThemeStats() throws IOException {
        System.out.println("Analyse du stock par thème...");
        String jql = "project = LOCAMWEB AND status in (Open, Reopened, \"Replied by CODIX\") AND type != CRQ";

        Map<String, Map<String, Integer>> stats = new LinkedHashMap<>();
        for (String t : ALL_THEMES) {
            stats.put(t, new HashMap<>());
            stats.get(t).put("LOCAM", 0);
            stats.get(t).put("Codix", 0);
        }

        int startAt = 0, maxResults = 100, total = 0;
        do {
            JSONObject result = searchJira(jql, startAt, maxResults, null);
            if (result == null) {
                break;
            }
            total = result.getInt("total");
            JSONArray issues = result.getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                JSONObject fields = issues.getJSONObject(i).getJSONObject("fields");
                String status = fields.getJSONObject("status").getString("name");
                String theme = identifyTheme(fields.optJSONArray("labels"));

                if (stats.containsKey(theme)) {
                    String key = "Replied by CODIX".equalsIgnoreCase(status) ? "LOCAM" : "Codix";
                    stats.get(theme).put(key, stats.get(theme).get(key) + 1);
                }
            }
            startAt += maxResults;
        } while (startAt < total);
        return stats;
    }

    private DashboardMetrics getDashboardMetrics(String projects) {
        DashboardMetrics kpis = new DashboardMetrics();
        LocalDate today = LocalDate.now();
        LocalDate endW = today.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
        LocalDate startW = endW.minusDays(6);
        LocalDate endPrev = startW.minusDays(1);
        LocalDate startPrev = endPrev.minusDays(6);

        String startStr = startW.format(JIRA_DATE_FMT);
        String endStr = endW.format(JIRA_DATE_FMT);
        String startPrevStr = startPrev.format(JIRA_DATE_FMT);
        String endPrevStr = endPrev.format(JIRA_DATE_FMT);

        String userList = "\"" + String.join("\", \"", TARGET_USERS.keySet()) + "\"";

        // 1. Nouveaux tickets
        kpis.stockWeb.current = getCount("project = LOCAMWEB AND type != CRQ AND assignee changed to hotline DURING (\"" + startStr + "\", \"" + endStr + "\")");
        kpis.stockWeb.previous = getCount("project = LOCAMWEB AND type != CRQ AND assignee changed to hotline DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\")");

        // 2. Réponses Codix
        kpis.replies.current = getCount("project in (" + projects + ") AND status changed to \"Replied by CODIX\" DURING (\"" + startStr + "\", \"" + endStr + "\")");
        kpis.replies.previous = getCount("project in (" + projects + ") AND status changed to \"Replied by CODIX\" DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\")");

        // 3. Stock Global assigné à l'équipe
        kpis.stockGlobal.current = getCount("project = LOCAMWEB AND status in (Open, Reopened) AND type != CRQ AND assignee IN (" + userList + ")");
        kpis.stockGlobal.previous = getCount("project = LOCAMWEB AND status was in (Open, Reopened) ON \"" + endPrevStr + "\" AND type != CRQ AND assignee WAS IN (" + userList + ") ON \"" + endPrevStr + "\"");

        // 3. Tickets clos par LOCAM
        kpis.closed.current = getCount("project in (" + projects + ") AND status changed FROM \"Replied by Codix\" to Closed DURING (\"" + startStr + "\", \"" + endStr + "\") AND type != CRQ");
        kpis.closed.previous = getCount("project in (" + projects + ") AND status changed FROM \"Replied by Codix\" to Closed DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\") AND type != CRQ");

        // --- AJOUT DU CALCUL STALE % (Tickets sans réponse > 7 jours ouvrés / 8 jours calendaires) ---
        if (kpis.stockGlobal.current > 0) {
            String jqlStale = "project = LOCAMWEB AND status in (Open, Reopened) "
                    + "AND \"Reopened/Updated by Client\" <= -8d "
                    + "AND type != CRQ AND assignee IN (" + userList + ")";

            double staleCount = getCount(jqlStale);

            kpis.stalePercent.current = (staleCount / kpis.stockGlobal.current) * 100.0;
        }

        // Valeur Semaine Dernière (S-1)
        if (kpis.stockGlobal.previous > 0) {
            // On cherche les tickets qui étaient stale il y a 7 jours (donc updated <= -15d)
            String jqlStalePrev = "project = LOCAMWEB AND status WAS IN (Open, Reopened) ON \"" + endPrevStr + "\" "
                    + "AND \"Reopened/Updated by Client\" <= -15d "
                    + "AND type != CRQ AND assignee WAS IN (" + userList + ") ON \"" + endPrevStr + "\"";
            double staleCountPrev = getCount(jqlStalePrev);
            kpis.stalePercent.previous = (staleCountPrev / kpis.stockGlobal.previous) * 100.0;
        }

        return kpis;
    }

    // Mise à jour de la méthode getPlanningData
    private PlanningData getPlanningData(String projectKeys, int nbWeeks) {
        PlanningData data = new PlanningData();
        LocalDate today = LocalDate.now();

        // 1. Semaines passées (Historique)
        for (int i = nbWeeks - 1; i >= 0; i--) {
            LocalDate endOfWeek = today.minusWeeks(i).with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
            LocalDate startOfWeek = endOfWeek.minusDays(6);
            int weekNum = endOfWeek.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());
            data.weeks.add(weekNum);
            data.weekDates.put(weekNum, new WeekRange(startOfWeek, endOfWeek));
        }

        // 2. NOUVEAU : 4 Prochaines semaines (Prévisionnel)
        for (int i = 1; i <= 4; i++) {
            LocalDate nextEnd = today.plusWeeks(i).with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
            LocalDate nextStart = nextEnd.minusDays(6);
            int weekNum = nextEnd.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());
            data.nextWeeks.add(weekNum);
            data.weekDates.put(weekNum, new WeekRange(nextStart, nextEnd));
        }

        TARGET_USERS.forEach((login, name) -> {
            UserStats u = new UserStats();
            u.login = login;
            u.fullName = name;
            data.userStats.put(login, u);
        });

        analyzeTimeSpent(projectKeys, data);
        analyzeAssignedStock(projectKeys, data);

        return data;
    }

    private void analyzeTimeSpent(String projects, PlanningData data) {
        LocalDate globalStart = data.weekDates.get(data.weeks.get(0)).start;
        String jql = "project in (" + projects + ") AND updated >= \"" + globalStart.format(JIRA_DATE_FMT) + "\"";
        int startAt = 0, total = 0;
        do {
            try {
                JSONObject result = searchJira(jql, startAt, 100, "changelog");
                if (result == null) {
                    break;
                }
                total = result.getInt("total");
                JSONArray issues = result.getJSONArray("issues");
                for (int i = 0; i < issues.length(); i++) {
                    processIssueHistory(issues.getJSONObject(i), data);
                }
                startAt += 100;
            } catch (Exception e) {
                break;
            }
        } while (startAt < total);
    }

    private void processIssueHistory(JSONObject issue, PlanningData data) {
        if (!issue.has("changelog")) {
            return;
        }
        JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
        for (int h = 0; h < histories.length(); h++) {
            JSONObject hist = histories.getJSONObject(h);
            String author = hist.getJSONObject("author").optString("name", "");
            if (!TARGET_USERS.containsKey(author)) {
                continue;
            }

            LocalDate d = LocalDateTime.parse(hist.getString("created"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toLocalDate();
            data.weeks.stream().filter(w -> !d.isBefore(data.weekDates.get(w).start) && !d.isAfter(data.weekDates.get(w).end)).findFirst().ifPresent(w -> {
                JSONArray items = hist.getJSONArray("items");
                for (int k = 0; k < items.length(); k++) {
                    if ("timespent".equalsIgnoreCase(items.getJSONObject(k).optString("field", ""))) {
                        long delta = items.getJSONObject(k).optLong("to", 0) - items.getJSONObject(k).optLong("from", 0);
                        if (delta > 0) {
                            data.addTime(author, w, delta / SECONDS_IN_MD);
                        }
                    }
                }
            });
        }
    }

    private void analyzeAssignedStock(String projects, PlanningData data) {
        for (String login : TARGET_USERS.keySet()) {
            for (Integer week : data.weeks) {
                String date = data.weekDates.get(week).end.format(JIRA_DATE_FMT);
                String jql = "project in (" + projects + ") AND status not in (Closed, Cancel) AND assignee was \"" + login + "\" ON \"" + date + "\"";
                data.setAssigned(login, week, getCount(jql));
            }
        }
    }

    private int getCount(String jql) {
        try {
            JSONObject json = searchJira(jql, 0, 0, null);
            int total = (json != null) ? json.getInt("total") : 0;
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    private JSONObject searchJira(String jql, int startAt, int maxResults, String expand) throws IOException {
        String url = jiraUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&startAt=" + startAt + "&maxResults=" + maxResults + "&fields=status,assignee,timespent,labels";
        if (expand != null) {
            url += "&expand=" + expand;
        }
        Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() ? new JSONObject(response.body().string()) : null;
        }
    }

    // --- INNER CLASSES ---
    public static class ReportData {

        public PlanningData planning;
        public DashboardMetrics dashboard;
        public Map<String, Map<String, Integer>> themeStats;
    }

    public static class DashboardMetrics {

        public KpiMetric stockWeb = new KpiMetric();
        public KpiMetric replies = new KpiMetric();
        public KpiMetric stockGlobal = new KpiMetric();
        public KpiMetric closed = new KpiMetric();
        public KpiMetric stalePercent = new KpiMetric();
    }

    public static class KpiMetric {

        public double current;
        public double previous;
    }

    public static class WeekRange {

        public LocalDate start;
        public LocalDate end;

        public WeekRange(LocalDate s, LocalDate e) {
            this.start = s;
            this.end = e;
        }
    }

    public static class UserStats {

        public String login, fullName;
        public Map<Integer, Double> timePerWeek = new HashMap<>();
        public Map<Integer, Integer> assignedPerWeek = new HashMap<>();
    }

    // À ajouter dans ResourcePlanningService.java
    public static class CapacityAlerts {

        public List<SufferingTheme> underCapacity = new ArrayList<>();
        public List<SufferingTheme> overCapacity = new ArrayList<>();
    }

    public Map<String, Integer> loadHRData(String cookie, PlanningData data) {
        Map<String, Integer> totalAbsences = new HashMap<>();
        // Map<Login, Map<WeekNum, Set<Dates>>> pour garantir l'unicité des jours de présence
        Map<String, Map<Integer, Set<LocalDate>>> uniquePresenceDays = new HashMap<>();

        try {
            // 1. Préparation du répertoire de logs
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            OkHttpClient client = getUnsafeOkHttpClient();
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusWeeks(4);
            Set<String> processedMonths = new HashSet<>();

            // Libellés de présence (issus de ta logique de référence)
            List<String> presenceLabels = Arrays.asList(
                    "Work From Home", "In office", "Workday",
                    "Business trip", "Client meeting at Codix", "Assistance on remote"
            );

            LocalDate cursor = today;
            while (!cursor.isAfter(endDate)) {
                int month = cursor.getMonthValue();
                int year = cursor.getYear();
                String monthKey = String.format("%04d%02d", year, month);

                if (!processedMonths.contains(monthKey)) {
                    System.out.println("[HR] Récupération des données pour " + monthValueToName(month) + " " + year + "...");

                    String html = null;
                    try {
                        html = fetchHrPage(client, cookie, month, year);
                    } catch (Exception e) {
                        System.err.println("⚠️ Impossible de joindre le HR Center : " + e.getMessage());
                        break; // On arrête si le serveur est injoignable
                    }

                    if (html != null) {
                        // Sauvegarde de la page HTML
                        Path logFile = logDir.resolve("hr_" + monthKey + ".html");
                        Files.writeString(logFile, html, StandardCharsets.UTF_8);

                        // LOG DE DÉBOGAGE
                        if (html.length() < 500) {
                            System.err.println("⚠️ [HR] ALERTE : La page reçue pour " + monthKey + " est anormalement courte (" + html.length() + " octets).");
                            System.err.println("    Vérifiez manuellement le fichier " + logFile.toAbsolutePath());
                        } else {
                            System.out.println("[HR] Données reçues (" + html.length() + " octets) sauvegardées.");
                        }

                        if (html.contains("name=\"login\"") || html.contains("type=\"password\"")) {
                            System.err.println("⚠️ [HR] ALERTE : Redirection vers la page de LOGIN détectée. Le cookie est invalide.");
                            break;
                        }

                        if (html.contains("<form") && html.contains("password")) {
                            System.err.println("⚠️ Session expirée (Page de login reçue). Mettez à jour le cookie.");
                            break;
                        }

                        Document doc = Jsoup.parse(html);
                        // Sélection des lignes utilisateurs via la classe CSS spécifique
                        Elements rows = doc.select("tr:has(td.user_name_td)");

                        for (Element row : rows) {
                            String fullNameFromHtml = row.select("td.user_name_td").text().trim().replace('\u00A0', ' ');

                            // Mapping Login <-> Nom Complet
                            String login = TARGET_USERS.entrySet().stream()
                                    .filter(entry -> {
                                        String targetName = entry.getValue();
                                        // Gestion des accents et cas particuliers (Valérie / Wafa)
                                        if (targetName.equalsIgnoreCase("Valérie Robert") && fullNameFromHtml.equalsIgnoreCase("Valerie Robert")) {
                                            return true;
                                        }
                                        if (targetName.equalsIgnoreCase("Wafa Ben Fadhloun") && fullNameFromHtml.equalsIgnoreCase("Wafa Fadhloun")) {
                                            return true;
                                        }
                                        return targetName.equalsIgnoreCase(fullNameFromHtml);
                                    })
                                    .map(Map.Entry::getKey).findFirst().orElse(null);

                            if (login == null) {
                                continue;
                            }

                            // Analyse des cellules de jours (ID commençant par cell_)
                            Elements cells = row.select("td[id^=cell_]");
                            for (Element cell : cells) {
                                String id = cell.id();
                                if (id.length() < 13) {
                                    continue;
                                }

                                String datePart = id.substring(5, 13);
                                LocalDate cellDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));

                                // A. Statistiques d'absences pour le mois en cours (tension globale)
                                if (datePart.startsWith(monthKey)) {
                                    if (cell.hasClass("paid_leave") || cell.hasClass("sick_leave") || cell.hasClass("compensation_leave")) {
                                        totalAbsences.merge(login, 1, Integer::sum);
                                    }
                                }

                                // B. Présences pour le calcul du Delta (4 prochaines semaines)
                                for (Integer weekNum : data.nextWeeks) {
                                    WeekRange range = data.weekDates.get(weekNum);
                                    if (range != null && !cellDate.isBefore(range.start) && !cellDate.isAfter(range.end)) {
                                        String title = cell.attr("title").trim();
                                        if (presenceLabels.stream().anyMatch(l -> l.equalsIgnoreCase(title))) {
                                            uniquePresenceDays.computeIfAbsent(login, k -> new HashMap<>())
                                                    .computeIfAbsent(weekNum, k -> new HashSet<>())
                                                    .add(cellDate);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    processedMonths.add(monthKey);
                }
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors du traitement HR : " + e.getMessage());
        }

        // 3. Calcul des deltas et Affichage de vérification
        System.out.println("\n--- VÉRIFICATION DES PRÉSENCES DÉTECTÉES ---");
        System.out.format("%-22s", "Utilisateur");
        for (Integer wn : data.nextWeeks) {
            System.out.format(" | Sem %-7d", wn);
        }
        System.out.println("\n------------------------------------------------------------------------------------");

        for (String login : TARGET_USERS.keySet()) {
            System.out.format("%-22s", TARGET_USERS.get(login));
            for (Integer weekNum : data.nextWeeks) {
                int count = (uniquePresenceDays.containsKey(login) && uniquePresenceDays.get(login).containsKey(weekNum))
                        ? uniquePresenceDays.get(login).get(weekNum).size() : 0;

                int delta = count - 5;
                data.setWeeklyDelta(login, weekNum, delta);

                System.out.format(" | %dj (Δ%+d) ", count, delta);
            }
            System.out.println();
        }
        System.out.println("------------------------------------------------------------------------------------\n");

        return totalAbsences;
    }

    private String monthValueToName(int month) {
        return java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH);
    }

    /**
     * Helper pour l'appel HTTP POST
     */
    private String fetchHrPage(OkHttpClient client, String cookie, int month, int year) throws Exception {
        // On s'assure que le cookie commence bien par PHPSESSID= si ce n'est pas déjà le cas
        String cookieHeader = cookie.contains("=") ? cookie : "PHPSESSID=" + cookie;

        RequestBody formBody = new FormBody.Builder()
                .add("lv_depts[]", "all_users")
                .add("cm", String.format("%02d", month))
                .add("cy", String.valueOf(year))
                .add("filter_report", "Filter")
                .build();

        Request request = new Request.Builder()
                .url(HR_URL)
                .post(formBody)
                .addHeader("Cookie", cookieHeader)
                // Ajout d'en-têtes pour imiter un vrai navigateur
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .addHeader("Referer", "https://hrcenter.codixfr.private/HR/index.php?tab=6")
                .addHeader("Origin", "https://hrcenter.codixfr.private")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("[HR] Erreur HTTP : " + response.code());
                return null;
            }
            return response.body().string();
        }
    }

    /**
     * Bypass SSL pour l'intranet
     */
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
