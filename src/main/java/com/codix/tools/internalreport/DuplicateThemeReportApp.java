package com.codix.tools.internalreport;

import com.codix.tools.AppConfig;
import com.codix.tools.locamreport.JiraService;
import com.codix.tools.btteamreport.ResourcePlanningService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DuplicateThemeReportApp {

    public static void main(String[] args) {
        try {
            // Utilisation de "UTF-8" (String) au lieu de StandardCharsets pour la compatibilité Java 8
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Utilisation du Singleton AppConfig
            AppConfig config = AppConfig.getInstance();
            
            String jiraApiUrl = config.getJiraBaseUrl();
            String jiraToken = config.getApiToken();

            if (jiraToken == null || jiraApiUrl == null) {
                System.err.println("ERREUR : Configuration Jira incomplète (URL ou Token manquant).");
                return;
            }

            // Nettoyage de l'URL pour les liens de navigation
            String jiraBaseUrl = jiraApiUrl.contains("/rest/") ? jiraApiUrl.split("/rest/")[0] : jiraApiUrl;

            JiraService jiraService = new JiraService(jiraApiUrl, jiraToken);

            // JQL et paramètres de sortie
            String jql = "project = LOCAMWEB AND created >= '2025-06-01' and \"Reopened/Updated by Client\" is not EMPTY ORDER BY created DESC";
            String filename = "DASHBOARD_CLEANUP.html";

            System.out.println("Analyse des tickets LOCAMWEB...");
            JSONObject response = jiraService.searchJira(jql, 1000, "summary,labels");

            if (response == null || !response.has("issues")) {
                System.out.println("Aucun ticket trouvé.");
                return;
            }

            JSONArray issues = response.getJSONArray("issues");
            Set<String> allThemesSet = new HashSet<>(Arrays.asList(ResourcePlanningService.ALL_THEMES));

            List<TicketInfo> duplicates = new ArrayList<>();
            List<TicketInfo> noThemes = new ArrayList<>();

            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                String key = issue.getString("key");
                JSONObject fields = issue.optJSONObject("fields");
                if (fields == null) continue;

                String summary = fields.has("summary") && !fields.isNull("summary") ? fields.getString("summary") : "Sans titre";
                
                JSONArray labels = fields.optJSONArray("labels");
                List<String> foundThemes = new ArrayList<>();
                if (labels != null) {
                    for (int j = 0; j < labels.length(); j++) {
                        String label = labels.getString(j);
                        if (allThemesSet.contains(label)) {
                            foundThemes.add(label);
                        }
                    }
                }

                TicketInfo info = new TicketInfo(key, summary, foundThemes);
                if (foundThemes.size() > 1) {
                    duplicates.add(info);
                } else if (foundThemes.isEmpty()) {
                    noThemes.add(info);
                }
            }

            // --- Génération du rendu HTML ---
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
            html.append("<style>");
            html.append("body { font-family: 'Segoe UI', Tahoma, sans-serif; margin: 20px; background-color: #f4f4f9; color: #333; }");
            html.append("h1 { color: #1197D6; border-left: 5px solid #E30613; padding-left: 15px; margin-top: 30px; }");
            html.append("h2 { color: #555; margin-top: 40px; border-bottom: 2px solid #ddd; padding-bottom: 5px; text-transform: uppercase; font-size: 16px; }");
            html.append("table { border-collapse: collapse; width: 100%; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); margin-bottom: 20px; }");
            html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; vertical-align: middle; }");
            html.append("th { background-color: #4a90e2; color: white; }");
            html.append("a { color: #4a90e2; text-decoration: none; font-weight: bold; }");
            html.append("a:visited { color: #4a90e2; }");
            html.append("a:hover { text-decoration: underline; }");
            html.append(".clicked-link { color: #8e44ad !important; }");
            html.append(".badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; margin-right: 5px; display: inline-block; }");
            html.append(".badge-theme { background: #e8f4fd; color: #2b5797; border: 1px solid #d0e1f9; }");
            html.append("</style></head><body>");

            html.append("<h1>Analyse Thématique - LOCAMWEB</h1>");
            html.append("<p>Tickets créés depuis le 01/06/2025 avec mise à jour client identifiée.</p>");

            html.append("<h2>1. Tickets avec Thèmes en Double (").append(duplicates.size()).append(")</h2>");
            renderTable(html, duplicates, jiraBaseUrl, true);

            html.append("<h2>2. Tickets sans Thème identifié (").append(noThemes.size()).append(")</h2>");
            renderTable(html, noThemes, jiraBaseUrl, false);

            html.append("<script>");
            html.append("document.addEventListener('DOMContentLoaded', function() {");
            html.append("  document.querySelectorAll('a.track-click').forEach(function(link) {");
            html.append("    link.addEventListener('click', function() { this.classList.add('clicked-link'); });");
            html.append("  });");
            html.append("});");
            html.append("</script></body></html>");

            // Remplacement compatible Java 8 pour FileWriter avec Charset
            try (java.io.Writer fw = new java.io.OutputStreamWriter(new java.io.FileOutputStream(filename), "UTF-8")) {
                fw.write(html.toString());
                System.out.println("Rapport généré : " + filename + " (" + (duplicates.size() + noThemes.size()) + " tickets)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renderTable(StringBuilder html, List<TicketInfo> list, String baseUrl, boolean showThemes) {
        html.append("<table><thead><tr><th style='width:150px;'>Référence</th><th>Titre</th>");
        if (showThemes) html.append("<th>Thèmes détectés</th>");
        html.append("</tr></thead><tbody>");
        
        if (list.isEmpty()) {
            html.append("<tr><td colspan='").append(showThemes ? 3 : 2).append("' style='text-align:center; color:gray;'>Aucun ticket trouvé dans cette catégorie.</td></tr>");
        } else {
            for (TicketInfo t : list) {
                html.append("<tr>");
                html.append("<td><a href='").append(baseUrl).append("/browse/").append(t.key).append("' target='_blank' class='track-click'>").append(t.key).append("</a></td>");
                html.append("<td>").append(escape(t.summary)).append("</td>");
                if (showThemes) {
                    html.append("<td>");
                    for (String theme : t.themes) {
                        html.append("<span class='badge badge-theme'>").append(theme).append("</span>");
                    }
                    html.append("</td>");
                }
                html.append("</tr>");
            }
        }
        html.append("</tbody></table>");
    }

    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static class TicketInfo {
        String key;
        String summary;
        List<String> themes;
        TicketInfo(String key, String summary, List<String> themes) {
            this.key = key; this.summary = summary; this.themes = themes;
        }
    }
}