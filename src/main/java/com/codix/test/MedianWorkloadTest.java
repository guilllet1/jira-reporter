package com.codix.test;

import com.codix.tools.btteamreport.ResourcePlanningService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MedianWorkloadTest {

    private static final double SECONDS_IN_MD = 25200.0; // 7h par jour

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) { e.printStackTrace(); }

        try {
            // 1. Configuration
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            String jiraUrl = props.getProperty("jira.url");
            String token = props.getProperty("jira.token");
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();

            ResourcePlanningService service = new ResourcePlanningService(jiraUrl, token);
            Map<String, List<Double>> themeWorkloads = new HashMap<>();

            System.out.println("=== ANALYSE DU WORKLOAD MÉDIAN PAR THEME (8 DERNIERES SEMAINES) ===");

            // JQL : Tickets modifiés ces 8 dernières semaines avec du temps loggé
            String jql = "project in (LOCAMWEB, LOCAMDEV) AND updated >= \"-56d\" AND timespent > 0";
            int startAt = 0, total = 0;

            do {
                JSONObject result = searchJira(client, jiraUrl, token, jql, startAt);
                if (result == null) break;
                total = result.getInt("total");
                JSONArray issues = result.getJSONArray("issues");

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    String theme = service.identifyTheme(issue.getJSONObject("fields").optJSONArray("labels"));
                    
                    // Calcul du workload total cumulé par la TARGET_USERS sur ce ticket
                    double issueTime = calculateIssueWorkload(issue);
                    
                    if (issueTime > 0) {
                        themeWorkloads.computeIfAbsent(theme, k -> new ArrayList<>()).add(issueTime);
                    }
                }
                startAt += 100;
                System.out.print(".");
            } while (startAt < total);

            // 2. Affichage des résultats au format "Code Java"
            System.out.println("\n\n--- CODE GÉNÉRÉ POUR THEME_MEDIANS ---");
            System.out.println("private static final Map<String, Double> THEME_MEDIANS = new HashMap<>();\n");
            System.out.println("    static {");

            themeWorkloads.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    double median = calculateMedian(entry.getValue());
                    // Utilisation de Locale.US pour forcer le séparateur décimal "."
                    System.out.format(Locale.US, "        THEME_MEDIANS.put(\"%s\", %.2f);%n", 
                        entry.getKey(), median);
                });

            System.out.println("    }");

        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Analyse le changelog d'un ticket pour sommer le temps loggé par la TARGET_USERS.
     */
    private static double calculateIssueWorkload(JSONObject issue) {
        if (!issue.has("changelog")) return 0.0;
        double days = 0.0;
        JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
        
        for (int h = 0; h < histories.length(); h++) {
            JSONObject hist = histories.getJSONObject(h);
            JSONObject authorObj = hist.optJSONObject("author");
            if (authorObj == null) continue;
            
            String author = authorObj.optString("name", "");
            
            // On ne filtre que les membres de l'équipe
            if (!ResourcePlanningService.TARGET_USERS.containsKey(author)) continue;

            JSONArray items = hist.getJSONArray("items");
            for (int k = 0; k < items.length(); k++) {
                JSONObject item = items.getJSONObject(k);
                if ("timespent".equalsIgnoreCase(item.optString("field", ""))) {
                    long delta = item.optLong("to", 0) - item.optLong("from", 0);
                    if (delta > 0) days += delta / SECONDS_IN_MD;
                }
            }
        }
        return days;
    }

    private static double calculateMedian(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        Collections.sort(values);
        int n = values.size();
        return (n % 2 == 0) ? (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0 : values.get(n / 2);
    }

    private static JSONObject searchJira(OkHttpClient client, String url, String token, String jql, int startAt) throws Exception {
        String fullUrl = url + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) 
                + "&startAt=" + startAt + "&maxResults=100&expand=changelog&fields=labels,timespent";
        Request request = new Request.Builder().url(fullUrl).addHeader("Authorization", "Bearer " + token).build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() ? new JSONObject(response.body().string()) : null;
        }
    }
}