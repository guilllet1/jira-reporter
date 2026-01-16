package com.codix.tools.locamreport;

import com.codix.tools.AppConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class JiraReportApp {

    public static void main(String[] args) {
        try {
            // Utilisation de "UTF-8" (String) au lieu de StandardCharsets pour la compatibilité Java 8
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            AppConfig config = AppConfig.getInstance();
            JiraService jiraService = new JiraService(config.getJiraBaseUrl(), config.getApiToken());
            String jql = config.getJql();

            HtmlRenderer renderer = new HtmlRenderer();

            // Récupération des données
            Map<String, Map<String, Integer>> domainStats = jiraService.getCurrentDomainStats(jql);
            Map<String, Map<String, Integer>> functionalStats = jiraService.getCurrentFunctionalStats(jql);
            Map<String, Map<String, Integer>> themeStats = jiraService.getCurrentThemeStats(jql);

            // Calcul des temps moyens d'assignation
            Map<String, Map<String, Double>> avgTimeStats = jiraService.getMedianAssignmentTimeByDomain(jql);

            JiraService.HistoryData historyStats = jiraService.getHistoryMetrics(jql);
            JiraService.CategoryHistoryData categoryStats = jiraService.getCodixCategoryHistory(jql);

            Map<String, Double> globalDelays = jiraService.getGlobalAverageDelays(jql);
            renderer.generate(domainStats, functionalStats, themeStats, avgTimeStats, globalDelays, historyStats, categoryStats, "DASHBOARD_COPROJ.html");

            System.out.println("Succès ! Rapport généré : DASHBOARD_COPROJ.html");

        } catch (IOException ex) {
            System.getLogger(JiraReportApp.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }
}
