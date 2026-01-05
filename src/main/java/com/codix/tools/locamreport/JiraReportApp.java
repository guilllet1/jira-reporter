package com.codix.tools.locamreport;

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            String jql = props.getProperty("jql");
            JiraService jiraService = new JiraService(props.getProperty("jira.url"), props.getProperty("jira.token"));
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
renderer.generate(domainStats, functionalStats, themeStats, avgTimeStats, globalDelays, historyStats, categoryStats, props.getProperty("output.file"));

            System.out.println("Succès ! Rapport généré : " + props.getProperty("output.file"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
