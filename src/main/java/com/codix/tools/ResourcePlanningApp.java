package com.codix.tools;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ResourcePlanningApp {

    public static void main(String[] args) {
        try {
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
            String hrDir = "absence";
            int nbWeeks = 8;

            ResourcePlanningService service = new ResourcePlanningService(jiraUrl, token);
            ResourcePlanningRenderer renderer = new ResourcePlanningRenderer();

            System.out.println("[2/7] Récupération des données globales Jira (" + nbWeeks + " semaines)...");
            ResourcePlanningService.ReportData data = service.getReportData(projects, nbWeeks);

            System.out.println("[3/7] Chargement du profil de spécialisation de l'équipe...");
            Map<String, Map<String, Double>> teamSpecs = getTeamSpecialization();

            System.out.println("[4/7] Analyse du stock de tickets ouverts...");
            String jqlOpen = "project = LOCAMWEB AND status NOT IN (Closed, Cancel, Pending) AND type != CRQ";

            System.out.println("[5/7] Analyse consolidée du répertoire HR (Absences Janvier + Présences futures)...");
            // Analyse optimisée du répertoire en une seule passe
            Map<String, Integer> absencesJanvier = service.loadHRData(hrDir, "202601", data.planning);

            System.out.println("[6/7] Calcul de la tension et identification des alertes de capacité...");
            ResourcePlanningService.CapacityAlerts capacityAlerts = service.calculateCapacityAlerts(
                    jqlOpen,
                    teamSpecs,
                    absencesJanvier,
                    22.0
            );

            System.out.println("[7/7] Génération du rapport HTML final...");
            renderer.generate(data, capacityAlerts, outputFile);

            System.out.println("\n✅ ANALYSE TERMINÉE AVEC SUCCÈS");
            System.out.println("Rapport disponible : " + outputFile);

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE :");
            e.printStackTrace();
        }
    }

    private static Map<String, Map<String, Double>> getTeamSpecialization() {
        Map<String, Map<String, Double>> specs = new HashMap<>();

        specs.put("amakki", Map.of("AUTRES", 8.4, "TH16_Interfaces", 6.4, "TH2", 1.6));
        specs.put("amirchev", Map.ofEntries(
                Map.entry("AUTRES", 20.0),
                Map.entry("TH2", 15.5),
                Map.entry("TH6.2", 13.1),
                Map.entry("TH16_API", 9.6),
                Map.entry("GED", 6.6),
                Map.entry("TH16_Interfaces", 6.0),
                Map.entry("TH19", 3.4),
                Map.entry("TH3", 1.8),
                Map.entry("TH6.1", 1.3),
                Map.entry("TH7", 1.2),
                Map.entry("TH17_Migration", 1.1)
        ));
        specs.put("angeorgieva", Map.of("TH16_API", 29.4, "AUTRES", 11.6, "TH3", 6.6, "TH19", 4.2, "TH14", 2.3, "TH1", 1.4, "AD", 1.2, "TH20", 1.2));
        specs.put("atsirov", Map.of("TH7", 8.9, "TH3", 4.3, "AUTRES", 2.9, "TH10", 1.6, "TH5.1", 1.4));
        specs.put("ayacoub", Map.of("TH13", 12.4, "TH7", 9.5, "AUTRES", 6.0, "TH3", 5.2, "TH11", 3.6, "TH20", 3.2, "TH10", 2.1, "AD", 2.0, "TH1", 1.7));
        specs.put("bnouaji", Map.of("TH10", 11.7, "AUTRES", 3.0));
        specs.put("eveli", Map.of("TH17_Migration", 18.7, "TH16_Interfaces", 5.9, "TH5.1", 2.4, "TH18", 2.4));
        specs.put("fsouab", Map.of("TH5.2", 5.9, "TH16_API", 5.2, "AUTRES", 4.2, "TH10", 1.2, "TH11", 1.1));
        specs.put("iatanasov", Map.of("TH5.1", 14.9, "AUTRES", 5.2, "TH7", 5.0, "TH13", 4.4, "TH14", 4.3, "TH11", 3.8, "TH5.2", 3.8, "TH17_Migration", 3.0, "AD", 1.2));
        specs.put("ikolchev", Map.of("TH16_API", 6.1, "TH6.2", 4.9, "TH3", 3.9, "TH5.1", 3.8, "AUTRES", 3.7, "TH19", 3.3, "TH18", 1.4, "TH6.1", 1.3));
        specs.put("kbachvarova", Map.of("TH11", 48.0, "AUTRES", 17.3, "TH17_Migration", 8.1, "TH6.2", 3.1, "TH16_API", 1.2, "TH6.3", 1.0));
        specs.put("kkomitov", Map.of("AUTRES", 2.3, "TH5.2", 1.5));
        specs.put("kmateeva", Map.of("TH11", 48.4, "TH6.2", 7.1, "TH6.3", 5.0, "AUTRES", 5.0, "TH17_Migration", 4.3, "TH3", 2.9, "TH10", 1.7, "TH7", 1.2, "TH16_API", 1.1));
        specs.put("kslavchova", Map.of("TH3", 5.4, "TH5.1", 1.7, "TH7", 1.3));
        specs.put("mdaaji", Map.of("TH17_Migration", 32.1, "TH16_Interfaces", 2.7, "GED", 1.4, "AUTRES", 1.4));
        specs.put("mhadji", Map.of("TH20", 14.2, "TH2", 6.2, "TH16_Interfaces", 5.9, "TH16_API", 2.8, "TH1", 1.9, "AUTRES", 1.8, "TH5.1", 1.4, "TH18", 1.3, "TH3", 1.1));
        specs.put("mmrabet", Map.of("TH10", 16.2, "AUTRES", 5.6, "TH7", 1.3));
        specs.put("mniklenov", Map.of("AUTRES", 3.4, "TH7", 1.9, "TH11", 1.7, "TH17_Migration", 1.2));
        specs.put("msamareva", Map.of("TH11", 16.5, "AUTRES", 2.2));
        specs.put("ndelbecq", Map.ofEntries(
                Map.entry("TH5.1", 33.5),
                Map.entry("TH6.2", 19.1),
                Map.entry("AUTRES", 7.6),
                Map.entry("TH16_API", 6.3),
                Map.entry("TH2", 5.2),
                Map.entry("TH5.2", 4.1),
                Map.entry("TH17_Migration", 3.7),
                Map.entry("TH6.1", 2.6),
                Map.entry("TH20", 2.5),
                Map.entry("TH3", 1.7),
                Map.entry("TH18", 1.5)
        ));
        specs.put("rbensalem", Map.of("TH17_Migration", 8.5, "GED", 3.3, "AUTRES", 3.0, "TH2", 2.7, "TH16_API", 2.4, "TH6.2", 1.2));
        specs.put("rgospodinova", Map.of("AUTRES", 25.5, "TH11", 1.0));
        specs.put("rtkhayat", Map.of("TH10", 13.3, "TH13", 2.4, "TH11", 2.2, "TH6.3", 1.8, "AUTRES", 1.3));
        specs.put("sabbassi", Map.of("AUTRES", 4.8, "TH13", 2.8, "TH5.2", 2.2));
        specs.put("sbraham", Map.of("AUTRES", 11.1, "TH14", 10.1, "TH5.1", 4.8));
        specs.put("valmaleh", Map.of("AUTRES", 3.3, "TH17_Migration", 2.4));
        specs.put("vrobert", Map.of("TH11", 4.4));
        specs.put("wfadhloun", Map.of("TH19", 6.9, "TH3", 3.0, "TRANSVERSE", 1.8, "AUTRES", 1.1));
        specs.put("ypetrov", Map.of("TH5.1", 14.9, "TH17_Migration", 3.0, "TH7", 2.7, "TH6.2", 1.7, "TH20", 1.3, "AD", 1.2, "TH16_API", 1.2, "AUTRES", 1.1));

        return specs;
    }
}
