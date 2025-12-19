package com.codix.tools;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class JiraReportApp {

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

            String jql = props.getProperty("jql");
            JiraService jiraService = new JiraService(props.getProperty("jira.url"), props.getProperty("jira.token"));
            HtmlRenderer renderer = new HtmlRenderer();

            // 2. Exécution de la logique métier
            
            // Tableau 1 : Domaines (Existant)
            Map<String, Map<String, Integer>> domainStats = jiraService.getCurrentDomainStats(jql);

            // Tableau 1bis : Domaines Fonctionnels (Nouveau)
            Map<String, Map<String, Integer>> functionalStats = jiraService.getCurrentFunctionalStats(jql);
            
            Map<String, Map<String, Integer>> themeStats = jiraService.getCurrentThemeStats(jql); // Nouveau
            
            // Tableau 2 : Historique Général
            JiraService.HistoryData historyStats = jiraService.getHistoryMetrics(jql);

            // Tableau 3 : Historique Catégories
            JiraService.CategoryHistoryData categoryStats = jiraService.getCodixCategoryHistory(jql);

            // 3. Génération du rendu (Passage des 4 datasets)
            renderer.generate(domainStats, functionalStats, themeStats, historyStats, categoryStats, props.getProperty("output.file"));

            System.out.println("Succès ! Rapport généré : " + props.getProperty("output.file"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}