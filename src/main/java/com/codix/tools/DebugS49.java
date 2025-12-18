package com.codix.tools;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DebugS49 {

    // CONFIGURATION CIBLE
    private static final String TARGET_USER_LOGIN = "amirchev";
    private static final String TARGET_USER_NAME = "Aleksandar Mirchev"; // Pour recherche textuelle si besoin
    private static final int TARGET_WEEK = 49;
    
    // API
    private static OkHttpClient client;
    private static String jiraUrl;
    private static String token;

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            
            // 1. Chargement Config
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) { props.load(fis); }
            jiraUrl = props.getProperty("jira.url");
            token = props.getProperty("jira.token");
            client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();

            // 2. Définition des dates Semaine 49
            LocalDate today = LocalDate.now(); 
            // On recule jusqu'à trouver la semaine 49
            LocalDate dateInS49 = LocalDate.of(2025, 12, 4); // Un jour au milieu de la S49 pour caler
            LocalDate startS49 = dateInS49.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1); // Lundi 01/12
            LocalDate endS49 = dateInS49.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 7);   // Dimanche 07/12
            
            System.out.println("=== DEBUG ANALYSE SEMAINE " + TARGET_WEEK + " ===");
            System.out.println("Période : Du " + startS49 + " au " + endS49);
            System.out.println("Utilisateur : " + TARGET_USER_LOGIN);
            System.out.println("=========================================\n");

            // 3. JQL Large (On prend large pour être sûr de ne rien rater)
            String jql = "project in (LOCAMWEB, LOCAMDEV) AND updated >= \"2025-11-20\"";
            System.out.println("JQL Global : " + jql + "\n");

            double totalJours = 0.0;
            int startAt = 0;
            int maxResults = 100;
            int total = 0;

            // 4. Boucle de lecture
            do {
                System.out.println("Lecture tickets " + startAt + "...");
                JSONObject json = searchJira(jql, startAt, maxResults);
                if (json == null) break;
                
                total = json.getInt("total");
                JSONArray issues = json.getJSONArray("issues");
                if (issues.length() == 0) break;

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    String key = issue.getString("key");
                    
                    if (!issue.has("changelog")) continue;
                    
                    JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
                    for (int h = 0; h < histories.length(); h++) {
                        JSONObject history = histories.getJSONObject(h);
                        
                        // Check Auteur
                        JSONObject authorObj = history.optJSONObject("author");
                        String author = (authorObj != null) ? authorObj.optString("name", "") : "";
                        String authorDisp = (authorObj != null) ? authorObj.optString("displayName", "") : "";
                        
                        boolean isMatch = TARGET_USER_LOGIN.equalsIgnoreCase(author) || authorDisp.contains(TARGET_USER_NAME);
                        
                        if (isMatch) {
                            // Check Date
                            String dateStr = history.getString("created");
                            LocalDateTime date = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                            
                            // Est-ce dans la semaine 49 ?
                            boolean inWeek = !date.toLocalDate().isBefore(startS49) && !date.toLocalDate().isAfter(endS49);
                            
                            if (inWeek) {
                                // Check Delta Time
                                JSONArray items = history.getJSONArray("items");
                                for (int k = 0; k < items.length(); k++) {
                                    JSONObject item = items.getJSONObject(k);
                                    if ("timespent".equalsIgnoreCase(item.optString("field"))) {
                                        String from = item.optString("from", "0");
                                        String to = item.optString("to", "0");
                                        if (from == null || from.equals("null") || from.isEmpty()) from = "0";
                                        if (to == null || to.equals("null") || to.isEmpty()) to = "0";
                                        
                                        long delta = Long.parseLong(to) - Long.parseLong(from);
                                        
                                        if (delta > 0) {
                                            double jour = delta / 28800.0;
                                            double heures = delta / 3600.0;
                                            
                                            System.out.println(" --> MATCH ! Ticket : " + key);
                                            System.out.println("     Date  : " + dateStr);
                                            System.out.println("     Ajout : " + String.format("%.2f", heures) + " h (" + String.format("%.3f", jour) + " JH)");
                                            
                                            totalJours += jour;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                startAt += maxResults;
                
            } while (startAt < total);

            System.out.println("\n=========================================");
            System.out.println("TOTAL FINAL SEMAINE 49 : " + String.format("%.3f", totalJours) + " Jours/Homme");
            System.out.println("=========================================");

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static JSONObject searchJira(String jql, int startAt, int maxResults) throws IOException {
        String url = jiraUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) 
                   + "&startAt=" + startAt + "&maxResults=" + maxResults + "&fields=timespent&expand=changelog";
        Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return new JSONObject(response.body().string());
        }
    }
}