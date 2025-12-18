package com.codix.tools;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ResourcePlanningApp {

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
            String projects = "LOCAMWEB, LOCAMDEV"; 
            String outputFile = "Rapport_Planning_LOCAM.html";
            int nbWeeks = 8;

            System.out.println("Démarrage de l'analyse Complète...");

            ResourcePlanningService service = new ResourcePlanningService(jiraUrl, token);
            ResourcePlanningRenderer renderer = new ResourcePlanningRenderer();

            // 1. Récupération des données globales
            ResourcePlanningService.ReportData data = service.getReportData(projects, nbWeeks);

            // 2. Génération du HTML
            renderer.generate(data, outputFile);

            System.out.println("\nSuccès ! Rapport généré : " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}