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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TicketAnalyzerTest {

    private static String jiraUrl;
    private static String token;
    private static OkHttpClient client;

    public static void main(String[] args) {
        try { 
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); 
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8)); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) { 
                props.load(fis); 
            }
            jiraUrl = props.getProperty("jira.url");
            token = props.getProperty("jira.token");
            client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();

            List<String> ticketKeys = Arrays.asList(
                "LOCAMWEB-13865","LOCAMWEB-14154","LOCAMWEB-14173","LOCAMWEB-14166","LOCAMWEB-12894",
                "LOCAMWEB-13869","LOCAMWEB-14213","LOCAMWEB-14180","LOCAMWEB-14113","LOCAMWEB-14124",
                "LOCAMWEB-14123","LOCAMWEB-14181","LOCAMWEB-14136","LOCAMWEB-13145","LOCAMWEB-12407",
                "LOCAMWEB-13658","LOCAMWEB-14196","LOCAMWEB-14168","LOCAMWEB-14133","LOCAMWEB-14096",
                "LOCAMWEB-14202","LOCAMWEB-14137","LOCAMWEB-14126","LOCAMWEB-13983","LOCAMWEB-12123",
                "LOCAMWEB-12142","LOCAMWEB-12795","LOCAMWEB-13811","LOCAMWEB-14207","LOCAMWEB-13811", // Doublon corrigé
                "LOCAMWEB-14207","LOCAMWEB-13874","LOCAMWEB-14016","LOCAMWEB-14132","LOCAMWEB-14052",
                "LOCAMWEB-13977","LOCAMWEB-13254"
            );

            System.out.println("=== ANALYSE DES TRANSITIONS DIRECTES VERS 'LIVRÉE' ===\n");

            String jql = "key in (" + String.join(",", ticketKeys) + ")";
            JSONObject result = searchJira(jql);

            if (result != null && result.has("issues")) {
                JSONArray issues = result.getJSONArray("issues");
                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    String key = issue.getString("key");
                    JSONObject fields = issue.getJSONObject("fields");
                    String status = fields.getJSONObject("status").getString("name");

                    // Condition : Status not in (Open, Reopened)
                    if (!status.equalsIgnoreCase("Open") && !status.equalsIgnoreCase("Reopened")) {
                        
                        // Analyse du Changelog pour la transition directe
                        if (isDirectlyDelivered(issue)) {
                            System.out.println("[ALERTE] " + key + " est passé à 'Livrée' sans passer par 'ATT livraison' (Statut actuel: " + status + ")");
                        }
                    }
                }
            }

            System.out.println("\nAnalyse terminée.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si le ticket est passé à 'Livrée' sans jamais avoir eu le statut 'ATT livraison'.
     */
    private static boolean isDirectlyDelivered(JSONObject issue) {
        if (!issue.has("changelog")) return false;

        JSONArray histories = issue.getJSONObject("changelog").getJSONArray("histories");
        boolean hasBeenInAttLivraison = false;
        boolean hasReachedLivree = false;

        for (int i = 0; i < histories.length(); i++) {
            JSONArray items = histories.getJSONObject(i).getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                
                if ("Statut Codix".equalsIgnoreCase(item.optString("field", ""))) {
                    String toString = item.optString("toString", "");
                    
                    if ("ATT livraison".equalsIgnoreCase(toString)) {
                        hasBeenInAttLivraison = true;
                    }
                    if ("Livrée".equalsIgnoreCase(toString)) {
                        hasReachedLivree = true;
                    }
                }
            }
        }
        
        // On retourne vrai si le ticket a atteint 'Livrée' MAIS n'est JAMAIS passé par 'ATT livraison'
        return hasReachedLivree && !hasBeenInAttLivraison;
    }

    private static JSONObject searchJira(String jql) throws IOException {
        String url = jiraUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) 
                   + "&fields=status&expand=changelog&maxResults=100";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return new JSONObject(response.body().string());
        }
    }
}