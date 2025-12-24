package com.codix.tools;

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

    // --- ANALYSE DES ABSENCES (HR) ---
    public Map<String, Integer> parseHRAbsences(String path, String monthPrefix) throws Exception {
        Map<String, Integer> absences = new HashMap<>();
        File input = new File(path);
        if (!input.exists()) {
            return absences;
        }

        Document doc = Jsoup.parse(input, "UTF-8");
        Elements rows = doc.select("tr:has(td.user_name_td)");

        for (Element row : rows) {
            String fullName = row.select("td.user_name_td").text().trim();

            // Trouver la clé login correspondant au nom complet
            String login = TARGET_USERS.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(fullName))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            if (login != null) {
                // Compter les cellules de congés (paid, sick, compensation) pour le mois
                long count = row.select("td[id^=cell_" + monthPrefix + "]").stream()
                        .filter(td -> td.hasClass("paid_leave") || td.hasClass("sick_leave") || td.hasClass("compensation_leave"))
                        .count();
                absences.put(login, (int) count);
            }
        }
        return absences;
    }

    public List<SufferingTheme> calculateSufferingThemes(String jql, Map<String, Map<String, Double>> specs, Map<String, Integer> absences, double workingDays) throws IOException {
        Map<String, Integer> themeStock = new HashMap<>();
        List<String> autresTickets = new ArrayList<>(); // Pour loguer les clés des tickets "AUTRES"

        JSONObject result = searchJira(jql, 0, 1000, null);
        if (result != null && result.has("issues")) {
            JSONArray issues = result.getJSONArray("issues");
            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                String theme = identifyTheme(issue.getJSONObject("fields").optJSONArray("labels"));
                themeStock.put(theme, themeStock.getOrDefault(theme, 0) + 1);

                // Log des tickets "AUTRES"
                if ("AUTRES".equals(theme)) {
                    autresTickets.add(issue.getString("key"));
                }
            }
        }

        if (!autresTickets.isEmpty()) {
            System.out.println("Tickets identifiés dans le thème 'AUTRES' : " + String.join(", ", autresTickets));
        }

        // Calcul de la capacité
        Map<String, Double> themeCapacity = new HashMap<>();
        specs.forEach((login, userSpecs) -> {
            double daysPresent = workingDays - absences.getOrDefault(login, 0);
            userSpecs.forEach((theme, pct) -> {
                themeCapacity.merge(theme, daysPresent * (pct / 100.0), Double::sum);
            });
        });

        List<SufferingTheme> suffering = new ArrayList<>();
        System.out.println("\n--- DÉTAILS DU CALCUL DE TENSION (CHARGE vs CAPACITÉ) ---");

        themeStock.keySet().stream().sorted()
                .filter(t -> !"TH18".equals(t)) // EXCLUSION TH18
                .forEach(theme -> {
                    int count = themeStock.get(theme);
                    double chargeEstimee = count * 2.0; // 2 jours par ticket
                    double capacite = themeCapacity.getOrDefault(theme, 0.0);
                    double tension = (capacite > 0) ? chargeEstimee / capacite : (chargeEstimee > 0 ? 99.0 : 0.0);

                    // Calcul du besoin en ressources supplémentaires (Le Gap)
                    double extraResources = Math.max(0, (chargeEstimee - capacite) / workingDays);

                    System.out.format("Thème: %-15s | Stock: %2d | Charge: %4.1fj | Capa: %4.1fj | Coef: %4.2fx | Extra: +%.2f res%n",
                            theme, count, chargeEstimee, capacite, tension, extraResources);

                    if (count >= 3 && tension > 1.1) {
                        suffering.add(new SufferingTheme(theme, tension, count, extraResources));
                    }
                });

        return suffering.stream()
                .sorted(Comparator.comparingDouble(SufferingTheme::getTension).reversed())
                .limit(3)
                .collect(Collectors.toList());
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

        // --- AJOUT DU CALCUL STALE % (Tickets sans réponse > 7 jours ouvrés / 8 jours calendaires) ---
        if (kpis.stockGlobal.current > 0) {
            String jqlStale = "project = LOCAMWEB AND status in (Open, Reopened) "
                    + "AND \"Reopened/Updated by Client\" <= -8d "
                    + "AND type != CRQ AND assignee IN (" + userList + ")";

            double staleCount = getCount(jqlStale);

            // Log de debug pour vérifier tes 17 tickets
            System.out.println("  [DEBUG KPI] Tickets Stale trouvés : " + (int) staleCount + " sur un stock de " + (int) kpis.stockGlobal.current);

            kpis.stalePercent.current = (staleCount / kpis.stockGlobal.current) * 100.0;
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
        UserStats u = new UserStats(); u.login = login; u.fullName = name;
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
                String jql = "project in (" + projects + ") AND status not in (Closed, Done) AND assignee was \"" + login + "\" ON \"" + date + "\"";
                data.setAssigned(login, week, getCount(jql));
            }
        }
    }

    private int getCount(String jql) {
        try {
            JSONObject json = searchJira(jql, 0, 0, null);
            int total = (json != null) ? json.getInt("total") : 0;

            // LOG DE DEBUG : À supprimer après correction
            if (jql.contains("-8d")) {
                System.out.println("  [DEBUG JQL STALE] : " + jql);
                System.out.println("  [DEBUG RESULT] : " + total + " tickets trouvés");
            }

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

}
