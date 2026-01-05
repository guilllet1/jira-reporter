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
import java.util.stream.Collectors;

public class RefreshSpecializationTool {

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

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) { e.printStackTrace(); }

        try {
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
            Map<String, Map<String, Long>> userActivity = new HashMap<>();
            Set<String> ticketsAutres = new TreeSet<>();

            System.out.println("=== ANALYSE DE SPÉCIALISATION (8 SEMAINES) ===");

            String logins = String.join(",", ResourcePlanningService.TARGET_USERS.keySet());
            String jql = "worklogAuthor in (" + logins + ") AND worklogDate >= \"-56d\"";
            
            int startAt = 0, total = 0;
            do {
                JSONObject result = searchJira(client, jiraUrl, token, jql, startAt);
                if (result == null) break;
                total = result.getInt("total");
                JSONArray issues = result.getJSONArray("issues");

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    JSONObject fields = issue.getJSONObject("fields");
                    String projectKey = fields.getJSONObject("project").getString("key");
                    
                    String context;
                    if (projectKey.startsWith("LOCAM")) {
                        context = identifyThemeExtended(issue, service);
                        if ("AUTRES".equals(context)) {
                            ticketsAutres.add(issue.getString("key") + " : " + fields.optString("summary"));
                        }
                    } else {
                        context = projectKey;
                    }

                    processIssueWorklogs(issue, context, userActivity);
                }
                startAt += 100;
                System.out.print(".");
            } while (startAt < total);

            System.out.println("\n\n=== TICKETS CLASSÉS EN 'AUTRES' ===");
            ticketsAutres.forEach(System.out::println);

            System.out.println("\n=== RÉCAPITULATIF DE LA RÉPARTITION DU TEMPS ===");
            printSummaryTable(userActivity);

            System.out.println("\n--- CODE JAVA GÉNÉRÉ ---\n");
            printGeneratedCode(userActivity);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static String identifyThemeExtended(JSONObject issue, ResourcePlanningService service) {
        JSONObject fields = issue.getJSONObject("fields");
        String theme = service.identifyTheme(fields.optJSONArray("labels"));
        if (!"AUTRES".equals(theme)) return theme;

        if (fields.has("issuelinks")) {
            JSONArray links = fields.getJSONArray("issuelinks");
            for (int j = 0; j < links.length(); j++) {
                JSONObject link = links.getJSONObject(j);
                JSONObject linkedIssue = link.has("outwardIssue") ? link.getJSONObject("outwardIssue") : (link.has("inwardIssue") ? link.getJSONObject("inwardIssue") : null);
                if (linkedIssue != null && linkedIssue.has("fields")) {
                    String linkedTheme = service.identifyTheme(linkedIssue.getJSONObject("fields").optJSONArray("labels"));
                    if (!"AUTRES".equals(linkedTheme)) return linkedTheme;
                }
            }
        }

        String summary = fields.optString("summary", "").toUpperCase();
        for (String t : THEMES_LIST) {
            if (summary.contains(t.toUpperCase())) return t;
        }
        for (Map.Entry<String, String> entry : SPECIAL_MAPPINGS.entrySet()) {
            if (summary.contains(entry.getKey())) return entry.getValue();
        }

        return "AUTRES";
    }

    private static void processIssueWorklogs(JSONObject issue, String context, Map<String, Map<String, Long>> userActivity) {
        if (!issue.has("changelog")) return;
        JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
        for (int h = 0; h < histories.length(); h++) {
            JSONObject hist = histories.getJSONObject(h);
            String author = hist.getJSONObject("author").optString("name", "");
            if (ResourcePlanningService.TARGET_USERS.containsKey(author)) {
                JSONArray items = hist.getJSONArray("items");
                for (int k = 0; k < items.length(); k++) {
                    JSONObject item = items.getJSONObject(k);
                    if ("timespent".equalsIgnoreCase(item.optString("field", ""))) {
                        long delta = item.optLong("to", 0) - item.optLong("from", 0);
                        if (delta > 0) {
                            userActivity.computeIfAbsent(author, k2 -> new HashMap<>()).merge(context, delta, Long::sum);
                        }
                    }
                }
            }
        }
    }

    private static void printGeneratedCode(Map<String, Map<String, Long>> userActivity) {
        System.out.println("private static Map<String, Map<String, Double>> getTeamSpecialization() {");
        System.out.println("    Map<String, Map<String, Double>> specs = new HashMap<>();\n");

        userActivity.keySet().stream().sorted().forEach(login -> {
            Map<String, Long> activities = userActivity.get(login);
            long totalTime = activities.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Double> locamSpecs = new LinkedHashMap<>();
            activities.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("TH") || e.getKey().equals("AD") || e.getKey().equals("GED") || e.getKey().equals("ELLISPHERE") || e.getKey().equals("AUTRES") || e.getKey().equals("TRANSVERSE"))
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        double pct = (e.getValue() * 100.0) / totalTime;
                        if (pct >= 1.0) locamSpecs.put(e.getKey(), Math.round(pct * 10.0) / 10.0);
                    });

            if (!locamSpecs.isEmpty()) {
                if (locamSpecs.size() > 10) {
                    System.out.println("    specs.put(\"" + login + "\", Map.ofEntries(");
                    String entries = locamSpecs.entrySet().stream()
                            .map(e -> "        Map.entry(\"" + e.getKey() + "\", " + e.getValue() + ")")
                            .collect(Collectors.joining(",\n"));
                    System.out.println(entries + "\n    ));");
                } else {
                    String entries = locamSpecs.entrySet().stream().map(e -> "\"" + e.getKey() + "\", " + e.getValue()).collect(Collectors.joining(", "));
                    System.out.println("    specs.put(\"" + login + "\", Map.of(" + entries + "));");
                }
            }
        });
        System.out.println("\n    return specs;");
        System.out.println("}");
    }

    private static void printSummaryTable(Map<String, Map<String, Long>> userActivity) {
        System.out.format("%-15s | %-12s | %-15s | %-20s%n", "LOGIN", "% LOCAM", "% EXTERNE", "PROJETS EXT. MAJEURS");
        System.out.println("--------------------------------------------------------------------------------");
        userActivity.keySet().stream().sorted().forEach(login -> {
            Map<String, Long> acts = userActivity.get(login);
            long total = acts.values().stream().mapToLong(Long::longValue).sum();
            long locamTotal = acts.entrySet().stream().filter(e -> e.getKey().startsWith("TH") || e.getKey().equals("AD") || e.getKey().equals("GED") || e.getKey().equals("ELLISPHERE") || e.getKey().equals("AUTRES") || e.getKey().equals("TRANSVERSE")).mapToLong(Map.Entry::getValue).sum();
            long extTotal = total - locamTotal;
            String extProjs = acts.entrySet().stream().filter(e -> !e.getKey().startsWith("TH") && !e.getKey().equals("AD") && !e.getKey().equals("GED") && !e.getKey().equals("ELLISPHERE") && !e.getKey().equals("AUTRES") && !e.getKey().equals("TRANSVERSE")).sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(2).map(e -> e.getKey() + "(" + Math.round((e.getValue() * 100.0) / total) + "%)").collect(Collectors.joining(", "));
            System.out.format("%-15s | %-12.1f | %-15.1f | %-20s%n", login, (locamTotal * 100.0 / total), (extTotal * 100.0 / total), extProjs);
        });
    }

    private static JSONObject searchJira(OkHttpClient client, String url, String token, String jql, int startAt) throws Exception {
        String fullUrl = url + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&startAt=" + startAt + "&maxResults=100&expand=changelog&fields=labels,project,summary,issuelinks";
        Request request = new Request.Builder().url(fullUrl).addHeader("Authorization", "Bearer " + token).build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() ? new JSONObject(response.body().string()) : null;
        }
    }
}