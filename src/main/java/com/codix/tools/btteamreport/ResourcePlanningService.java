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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ResourcePlanningService {

    private final String jiraUrl;
    private final String token;
    private final OkHttpClient client;
    private static final DateTimeFormatter JIRA_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double SECONDS_IN_MD = 25200.0;

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

    private static final String[] ALL_THEMES = {
        "AD", "ELLISPHERE", "GED", "TH1", "TH10", "TH11", "TH12", "TH13", "TH14", "TH16_API",
        "TH16_Interfaces", "TH17_Migration", "TH18", "TH19", "TH2", "TH20", "TH3", "TH4",
        "TH5.1", "TH5.2", "TH6.1", "TH6.2", "TH6.3", "TH7", "TH8", "TRANSVERSE"
    };

    private static final Map<String, Double> THEME_MEDIANS = new HashMap<>();

    static {
        THEME_MEDIANS.put("AD", 0.08);
        THEME_MEDIANS.put("AUTRES", 0.21);
        THEME_MEDIANS.put("ELLISPHERE", 0.48);
        THEME_MEDIANS.put("GED", 0.55);
        THEME_MEDIANS.put("TH1", 0.38);
        THEME_MEDIANS.put("TH10", 0.73);
        THEME_MEDIANS.put("TH11", 0.43);
        THEME_MEDIANS.put("TH12", 0.13);
        THEME_MEDIANS.put("TH13", 0.23);
        THEME_MEDIANS.put("TH14", 0.32);
        THEME_MEDIANS.put("TH16_API", 0.49);
        THEME_MEDIANS.put("TH16_Interfaces", 0.61);
        THEME_MEDIANS.put("TH17_Migration", 0.33);
        THEME_MEDIANS.put("TH18", 0.18);
        THEME_MEDIANS.put("TH19", 0.42);
        THEME_MEDIANS.put("TH2", 0.46);
        THEME_MEDIANS.put("TH20", 0.31);
        THEME_MEDIANS.put("TH3", 0.55);
        THEME_MEDIANS.put("TH5.1", 0.24);
        THEME_MEDIANS.put("TH5.2", 0.25);
        THEME_MEDIANS.put("TH6.1", 0.26);
        THEME_MEDIANS.put("TH6.2", 0.36);
        THEME_MEDIANS.put("TH6.3", 0.24);
        THEME_MEDIANS.put("TH7", 0.52);
        THEME_MEDIANS.put("TH8", 0.30);
        THEME_MEDIANS.put("TRANSVERSE", 1.55);
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

    /**
     * Analyse consolidée du répertoire 'absence' (Absences + Présences). Gère
     * les correspondances de noms spécifiques pour le HR Center.
     */
    public Map<String, Integer> loadHRData(String directoryPath, String monthPrefix, PlanningData data) throws Exception {
        Map<String, Integer> totalAbsences = new HashMap<>();
        File folder = new File(directoryPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return totalAbsences;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".html"));
        if (files == null) {
            return totalAbsences;
        }

        // Collecte des jours de présence uniques par utilisateur et par semaine
        Map<String, Map<Integer, Set<LocalDate>>> uniquePresenceDays = new HashMap<>();

        // Liste des libellés considérés comme une présence (incluant les activités pro)
        List<String> presenceLabels = Arrays.asList(
                "Work From Home", "In office", "Workday",
                "Business trip", "Client meeting at Codix", "Assistance on remote"
        );

        for (File file : files) {
            Document doc = Jsoup.parse(file, "UTF-8");
            Elements rows = doc.select("tr:has(td.user_name_td)");

            for (Element row : rows) {
                // Nettoyage du nom (gestion des espaces insécables U+00A0)
                String fullNameFromHtml = row.select("td.user_name_td").text().trim().replace('\u00A0', ' ');

                // Identification du login avec gestion des cas spécifiques
                String login = TARGET_USERS.entrySet().stream()
                        .filter(entry -> {
                            String targetName = entry.getValue(); // ex: "Valérie Robert"

                            // Correspondance : "Valérie Robert" -> "Valerie Robert"
                            if (targetName.equalsIgnoreCase("Valérie Robert") && fullNameFromHtml.equalsIgnoreCase("Valerie Robert")) {
                                return true;
                            }

                            // Correspondance : "Wafa Ben Fadhloun" -> "Wafa Fadhloun"
                            if (targetName.equalsIgnoreCase("Wafa Ben Fadhloun") && fullNameFromHtml.equalsIgnoreCase("Wafa Fadhloun")) {
                                return true;
                            }

                            return targetName.equalsIgnoreCase(fullNameFromHtml);
                        })
                        .map(Map.Entry::getKey).findFirst().orElse(null);

                if (login == null) {
                    continue;
                }

                Elements cells = row.select("td[id^=cell_]");
                for (Element cell : cells) {
                    String id = cell.id(); // format cell_YYYYMMDD_...
                    if (id.length() < 13) {
                        continue;
                    }
                    String datePart = id.substring(5, 13);

                    // 1. Absences du mois (pour le calcul de tension)
                    if (datePart.startsWith(monthPrefix)) {
                        if (cell.hasClass("paid_leave") || cell.hasClass("sick_leave") || cell.hasClass("compensation_leave")) {
                            totalAbsences.merge(login, 1, Integer::sum);
                        }
                    }

                    // 2. Présences hebdomadaires (pour le calcul du delta)
                    try {
                        LocalDate cellDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));
                        for (Integer weekNum : data.nextWeeks) {
                            WeekRange range = data.weekDates.get(weekNum);
                            if (!cellDate.isBefore(range.start) && !cellDate.isAfter(range.end)) {
                                String title = cell.attr("title").trim();
                                if (presenceLabels.stream().anyMatch(l -> l.equalsIgnoreCase(title))) {
                                    uniquePresenceDays.computeIfAbsent(login, k -> new HashMap<>())
                                            .computeIfAbsent(weekNum, k -> new HashSet<>())
                                            .add(cellDate);
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Calcul final des deltas (Présence réelle - 5 jours)
        for (String login : TARGET_USERS.keySet()) {
            for (Integer weekNum : data.nextWeeks) {
                int count = (uniquePresenceDays.containsKey(login) && uniquePresenceDays.get(login).containsKey(weekNum))
                        ? uniquePresenceDays.get(login).get(weekNum).size() : 0;
                data.setWeeklyDelta(login, weekNum, count - 5);
            }
        }
        return totalAbsences;
    }

}
