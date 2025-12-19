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

public class ResourcePlanningService {

    private final String jiraUrl;
    private final String token;
    private final OkHttpClient client;
    private static final DateTimeFormatter JIRA_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double SECONDS_IN_MD = 28800.0;

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

    public ResourcePlanningService(String jiraUrl, String token) {
        this.jiraUrl = jiraUrl;
        this.token = token;
        this.client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();
    }

    public ReportData getReportData(String projectKeys, int nbWeeks) {
        ReportData report = new ReportData();
        report.planning = getPlanningData(projectKeys, nbWeeks);
        report.dashboard = getDashboardMetrics(projectKeys);
        return report;
    }

    // --- DASHBOARD (KPI) ---
    private DashboardMetrics getDashboardMetrics(String projects) {
        System.out.println("Calcul des KPIs Dashboard...");
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

        // 1. Nouveaux tickets assignés à Codix cette semaine (WEB)
        String jqlWebCurrent = "project = LOCAMWEB AND type != CRQ AND assignee changed to hotline DURING (\"" + startStr + "\", \"" + endStr + "\") AND NOT assignee changed to hotline BEFORE \"" + startStr + "\"";
        String jqlWebPrev = "project = LOCAMWEB AND type != CRQ AND assignee changed to hotline DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\") AND NOT assignee changed to hotline BEFORE \"" + startPrevStr + "\"";
        kpis.stockWeb.current = getCount(jqlWebCurrent);
        kpis.stockWeb.previous = getCount(jqlWebPrev);

        // 2. Réponses Codix
        String jqlReplyBase = "project in (" + projects + ") AND status changed to \"Replied by CODIX\"";
        kpis.replies.current = getCount(jqlReplyBase + " DURING (\"" + startStr + "\", \"" + endStr + "\")");
        kpis.replies.previous = getCount(jqlReplyBase + " DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\")");

        // 3. Tickets Fermés par LOCAM
        String jqlClosedCurrent = "project = LOCAMWEB AND type != CRQ AND \"Codix reply\" is not EMPTY AND status changed to Closed DURING (\"" + startStr + "\", \"" + endStr + "\")";
        String jqlClosedPrev = "project = LOCAMWEB AND type != CRQ AND \"Codix reply\" is not EMPTY AND status changed to Closed DURING (\"" + startPrevStr + "\", \"" + endPrevStr + "\")";
        kpis.closed.current = getCount(jqlClosedCurrent);
        kpis.closed.previous = getCount(jqlClosedPrev);

        // 4. Stock actuel assigné à Codix (S et S-1)
        String jqlStockCurrent = "project = LOCAMWEB AND status in (Open, Reopened) AND type != CRQ";
        String jqlStockPrev = "project = LOCAMWEB AND status was in (Open, Reopened) ON \"" + endPrevStr + "\" AND type != CRQ";
        kpis.stockGlobal.current = getCount(jqlStockCurrent);
        kpis.stockGlobal.previous = getCount(jqlStockPrev);

        // 5. % Stale (Sans réponse > 5j ouvrés)
        // Valeur Courante (S)
        if (kpis.stockGlobal.current > 0) {
            String jqlStaleCurrent = "project = LOCAMWEB AND status in (Open, Reopened) AND \"Reopened/Updated by Client\" < -8d AND type != CRQ";
            double staleCountCurrent = getCount(jqlStaleCurrent);
            kpis.stalePercent.current = (staleCountCurrent / kpis.stockGlobal.current) * 100.0;
        }

        // Valeur Précédente (S-1 au dimanche soir)
        if (kpis.stockGlobal.previous > 0) {
            // On calcule la date limite à S-1 (Date du dimanche S-1 moins 8 jours)
            String staleLimitPrevStr = endPrev.minusDays(8).format(JIRA_DATE_FMT);
            String jqlStalePrev = "project = LOCAMWEB AND status was in (Open, Reopened) ON \"" + endPrevStr + "\" " +
                                  "AND \"Reopened/Updated by Client\" <= \"" + staleLimitPrevStr + "\" AND type != CRQ";
            
            double staleCountPrev = getCount(jqlStalePrev);
            // Calcul du pourcentage : (Stock Sans réponse S-1 / Stock total S-1) * 100
            kpis.stalePercent.previous = (staleCountPrev / kpis.stockGlobal.previous) * 100.0;
        }
        
        return kpis;
    }

    // --- PLANNING (DETAIL) ---
    private PlanningData getPlanningData(String projectKeys, int nbWeeks) {
        PlanningData data = new PlanningData();
        LocalDate today = LocalDate.now();

        for (int i = nbWeeks - 1; i >= 0; i--) {
            LocalDate endOfWeek = today.minusWeeks(i).with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);
            LocalDate startOfWeek = endOfWeek.minusDays(6);
            int weekNum = endOfWeek.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());
            data.weeks.add(weekNum);
            data.weekDates.put(weekNum, new WeekRange(startOfWeek, endOfWeek));
        }

        for (Map.Entry<String, String> entry : TARGET_USERS.entrySet()) {
            UserStats u = new UserStats();
            u.login = entry.getKey();
            u.fullName = entry.getValue();
            data.userStats.put(entry.getKey(), u);
        }

        analyzeTimeSpent(projectKeys, data);
        analyzeAssignedStock(projectKeys, data);

        return data;
    }

    private void analyzeTimeSpent(String projects, PlanningData data) {
        System.out.print("Récupération du Temps Passé ");
        LocalDate globalStart = data.weekDates.get(data.weeks.get(0)).start;
        String jql = "project in (" + projects + ") AND updated >= \"" + globalStart.format(JIRA_DATE_FMT) + "\" ORDER BY key ASC";
        
        int startAt = 0;
        int maxPerRequest = 100;
        int total = 0;
        Set<String> processedTickets = new HashSet<>();

        do {
            try {
                System.out.print(".");
                JSONObject result = searchJira(jql, startAt, maxPerRequest, "changelog");
                if (result == null) break;
                
                total = result.getInt("total");
                JSONArray issues = result.getJSONArray("issues");
                if (issues.length() == 0) break;

                processIssuesForTime(issues, data, processedTickets);
                
                startAt += maxPerRequest;
            } catch (Exception e) { break; }
        } while (startAt < total);
        System.out.println(" OK (" + processedTickets.size() + " tickets traités)");
    }

    private void processIssuesForTime(JSONArray issues, PlanningData data, Set<String> processedTickets) {
        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            String issueKey = issue.getString("key");

            if (processedTickets.contains(issueKey)) continue;
            processedTickets.add(issueKey);

            if (!issue.has("changelog")) continue;
            JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
            
            for (int h = 0; h < histories.length(); h++) {
                JSONObject history = histories.getJSONObject(h);
                JSONObject authorObj = history.optJSONObject("author");
                if (authorObj == null) continue;
                String authorLogin = authorObj.optString("name", "");
                
                if (!TARGET_USERS.containsKey(authorLogin)) continue;

                String dateStr = history.getString("created");
                LocalDateTime changeDate;
                try { changeDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")); } catch(Exception ex) { continue; }

                int matchingWeek = -1;
                LocalDate d = changeDate.toLocalDate();
                
                for (Integer w : data.weeks) {
                    WeekRange range = data.weekDates.get(w);
                    if (!d.isBefore(range.start) && !d.isAfter(range.end)) {
                        matchingWeek = w;
                        break;
                    }
                }

                if (matchingWeek == -1) continue;

                JSONArray items = history.getJSONArray("items");
                for (int k = 0; k < items.length(); k++) {
                    JSONObject item = items.getJSONObject(k);
                    if ("timespent".equalsIgnoreCase(item.optString("field", ""))) {
                        String from = item.optString("from", "0");
                        String to = item.optString("to", "0");
                        long delta = Long.parseLong(to.isEmpty() || "null".equals(to) ? "0" : to) 
                                   - Long.parseLong(from.isEmpty() || "null".equals(from) ? "0" : from);
                        
                        if (delta > 0) {
                            data.addTime(authorLogin, matchingWeek, delta / SECONDS_IN_MD);
                        }
                    }
                }
            }
        }
    }

    private void analyzeAssignedStock(String projects, PlanningData data) {
        System.out.print("Calcul du Stock (Détail) ");
        for (String login : TARGET_USERS.keySet()) {
            System.out.print(".");
            for (Integer week : data.weeks) {
                WeekRange range = data.weekDates.get(week);
                boolean isCurrentWeek = week.equals(data.weeks.get(data.weeks.size()-1));
                String jql;
                
                if (isCurrentWeek) {
                    jql = "project in (" + projects + ") AND status not in (Closed, Done) AND assignee = \"" + login + "\"";
                } else {
                    String dateCheck = range.end.format(JIRA_DATE_FMT);
                    jql = "project in (" + projects + ") AND status not in (Closed, Done) AND assignee was \"" + login + "\" ON \"" + dateCheck + "\" AND status was not in (Closed, Done) ON \"" + dateCheck + "\"";
                }
                data.setAssigned(login, week, getCount(jql));
            }
        }
        System.out.println(" OK");
    }

    private int getCount(String jql) {
        try {
            JSONObject json = searchJira(jql, 0, 0, null);
            return json != null ? json.getInt("total") : 0;
        } catch (Exception e) { return 0; }
    }

    private JSONObject searchJira(String jql, int startAt, int maxResults, String expand) throws IOException {
        String url = jiraUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&startAt=" + startAt + "&maxResults=" + maxResults + "&fields=status,assignee,timespent";
        if (expand != null) url += "&expand=" + expand;
        Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return new JSONObject(response.body().string());
        }
    }

    public static class ReportData { public PlanningData planning; public DashboardMetrics dashboard; }
    public static class DashboardMetrics { public KpiMetric stockWeb = new KpiMetric(); public KpiMetric replies = new KpiMetric(); public KpiMetric stockGlobal = new KpiMetric(); public KpiMetric closed = new KpiMetric(); public KpiMetric stalePercent = new KpiMetric(); }
    public static class KpiMetric { public double current; public double previous; }
    public static class WeekRange { public LocalDate start; public LocalDate end; public WeekRange(LocalDate s, LocalDate e) { this.start = s; this.end = e; } }
    public static class UserStats { public String login; public String fullName; public Map<Integer, Double> timePerWeek = new HashMap<>(); public Map<Integer, Integer> assignedPerWeek = new HashMap<>(); }
    public static class PlanningData {
        public List<Integer> weeks = new ArrayList<>();
        public Map<Integer, WeekRange> weekDates = new HashMap<>();
        public Map<String, UserStats> userStats = new LinkedHashMap<>();
        public void addTime(String login, int week, double days) { if (userStats.containsKey(login)) userStats.get(login).timePerWeek.merge(week, days, Double::sum); }
        public void setAssigned(String login, int week, int count) { if (userStats.containsKey(login)) userStats.get(login).assignedPerWeek.put(week, count); }
    }
}