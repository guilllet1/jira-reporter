package com.codix.tools;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ResourcePlanningApp {

    public static void main(String[] args) {
        try {
            // Configuration obligatoire pour le support UTF-8 et les logs Codix
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("=== DÉMARRAGE DE L'ANALYSE DE PLANIFICATION LOCAM ===");

            System.out.println("[1/7] Chargement de la configuration...");
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }

            String jiraUrl = props.getProperty("jira.url");
            String token = props.getProperty("jira.token");
            String projects = "LOCAMWEB, LOCAMDEV";
            String outputFile = "Rapport_Planning_LOCAM.html";
            String hrFile = "HR Center - PROD.html";
            int nbWeeks = 8;

            ResourcePlanningService service = new ResourcePlanningService(jiraUrl, token);
            ResourcePlanningRenderer renderer = new ResourcePlanningRenderer();

            System.out.println("[2/7] Récupération des données globales Jira (" + nbWeeks + " semaines)...");
            ResourcePlanningService.ReportData data = service.getReportData(projects, nbWeeks);

            System.out.println("[3/7] Chargement du profil de spécialisation de l'équipe...");
            Map<String, Map<String, Double>> teamSpecs = getTeamSpecialization();

            System.out.println("[4/7] Analyse du stock de tickets ouverts...");
            // Filtre JQL excluant les tickets déjà répondus par Codix et les CRQ
            String jqlOpen = "project = LOCAMWEB AND status NOT IN (Closed, Cancel, Pending, \"Replied by CODIX\") AND type != CRQ";

            System.out.println("[5/7] Lecture du fichier HR (Absences Janvier 2026)...");
            Map<String, Integer> absencesJanvier = service.parseHRAbsences(hrFile, "202601");

            System.out.println("[6/7] Calcul de la tension et identification des thèmes en souffrance...");
            // Remplace l'ancien appel par celui-ci
    System.out.println("[6/7] Calcul de la tension et identification des alertes de capacité...");
ResourcePlanningService.CapacityAlerts capacityAlerts = service.calculateCapacityAlerts(
        jqlOpen,
        teamSpecs,
        absencesJanvier,
        22.0
);

            System.out.println("[6.1/7] Analyse des absences futures pour le tableau de bord...");
            // Récupération des absences pour le mois en cours et le mois suivant
            Map<String, Integer> nextMonthAbs = service.parseHRAbsences(hrFile, "202601");

            // Marquage des semaines futures si un collaborateur est absent
            // On utilise data.planning pour accéder aux listes de PlanningData
            data.planning.nextWeeks.forEach(wNum -> {
                data.planning.userStats.keySet().forEach(login -> {
                    // Si le collaborateur a plus de 2 jours d'absence dans le fichier HR pour Janvier
                    if (nextMonthAbs.getOrDefault(login, 0) > 2) {
                        data.planning.markAbsent(login, wNum);
                    }
                });
            });

            System.out.println("[7/7] Génération du rapport HTML final...");
            renderer.generate(data, capacityAlerts, outputFile);

            System.out.println("\n✅ ANALYSE TERMINÉE AVEC SUCCÈS");
            System.out.println("Rapport disponible : " + outputFile);

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE :");
            e.printStackTrace();
        }
    }

    /**
     * Spécialisation de l'équipe basée sur l'historique des worklogs (Analyse Top 3).
     */
    private static Map<String, Map<String, Double>> getTeamSpecialization() {
        Map<String, Map<String, Double>> specs = new HashMap<>();

        specs.put("amirchev", Map.of("TH16_API", 24.0, "TH2", 17.0, "TH6.2", 12.0));
        specs.put("amakki", Map.of("TH16_Interfaces", 58.0, "TH2", 25.0, "TH5.1", 8.0));
        specs.put("angeorgieva", Map.of("TH16_API", 66.0, "TH3", 9.0, "TH19", 8.0));
        specs.put("eveli", Map.of("TH17_Migration", 100.0));
        specs.put("fsouab", Map.of("TH5.2", 73.0, "TH11", 23.0, "TH7", 4.0));
        specs.put("iatanasov", Map.of("TH7", 20.0, "TH14", 20.0, "TH11", 14.0));
        specs.put("ikolchev", Map.of("TH16_API", 23.0, "TH5.1", 19.0, "TH3", 17.0));
        specs.put("kmateeva", Map.of("TH11", 69.0, "TH6.2", 12.0, "TH6.3", 5.0));
        specs.put("msamareva", Map.of("TH6.3", 61.0, "TH11", 30.0, "TH17_Migration", 9.0));
        specs.put("mniklenov", Map.of("TH7", 47.0, "TH11", 15.0, "TH17_Migration", 14.0));
        specs.put("mmrabet", Map.of("TH10", 51.0, "TH5.2", 38.0, "TH7", 11.0));
        specs.put("rgospodinova", Map.of("TH11", 14.0, "TH5.1", 10.0, "TH10", 9.0));
        specs.put("valmaleh", Map.of("TH19", 55.0, "TH16_Interfaces", 27.0, "TH5.2", 16.0));
        specs.put("ayacoub", Map.of("TH7", 47.0, "TH10", 10.0, "TH2", 10.0));
        specs.put("atsirov", Map.of("TH7", 62.0, "TH5.1", 22.0, "AD", 5.0));
        specs.put("bnouaji", Map.of("TH10", 86.0, "AD", 13.0, "TH18", 1.0));
        specs.put("kslavchova", Map.of("TH3", 65.0, "TH5.1", 25.0, "TH13", 10.0));
        specs.put("kkomitov", Map.of("TH5.2", 34.0, "AD", 29.0, "TH16_API", 18.0));
        specs.put("kbachvarova", Map.of("TH11", 65.0, "TH17_Migration", 16.0, "TH6.2", 9.0));
        specs.put("mdaaji", Map.of("TH17_Migration", 80.0, "GED", 5.0, "TH16_Interfaces", 5.0));
        specs.put("mhadji", Map.of("TH2", 24.0, "TH16_API", 23.0, "TH5.1", 12.0));
        specs.put("ndelbecq", Map.of("TH5.1", 23.0, "TH6.2", 21.0, "TH2", 10.0));
        specs.put("rbensalem", Map.of("TH17_Migration", 44.0, "TH2", 9.0, "TH5.1", 8.0));
        specs.put("rtkhayat", Map.of("TH10", 74.0, "TH6.3", 15.0, "TH16_Interfaces", 6.0));
        specs.put("sabbassi", Map.of("TH5.2", 54.0, "TH14", 9.0, "TH17_Migration", 8.0));
        specs.put("sbraham", Map.of("TH14", 40.0, "TH16_Interfaces", 22.0, "TH6.3", 17.0));
        specs.put("vrobert", Map.of("TH11", 92.0, "TH17_Migration", 5.0, "TH6.3", 2.0));
        specs.put("wfadhloun", Map.of("TH19", 61.0, "TH3", 23.0, "TH20", 7.0));
        specs.put("ypetrov", Map.of("TH2", 26.0, "TH7", 21.0, "TH6.2", 16.0));

        return specs;
    }
}