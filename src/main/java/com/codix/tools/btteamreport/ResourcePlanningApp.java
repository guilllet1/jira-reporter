package com.codix.tools.btteamreport;

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

    specs.put("amakki", Map.of("AUTRES", 8.1, "TH16_Interfaces", 4.1, "TH2", 1.8));
    specs.put("amirchev", Map.ofEntries(
        Map.entry("TH2", 18.4),
        Map.entry("TH6.2", 15.5),
        Map.entry("TH16_API", 13.1),
        Map.entry("AUTRES", 9.4),
        Map.entry("GED", 8.0),
        Map.entry("TH19", 4.0),
        Map.entry("TH3", 2.9),
        Map.entry("TH6.1", 2.7),
        Map.entry("TH5.1", 1.4),
        Map.entry("TH20", 1.1),
        Map.entry("TH1", 1.1),
        Map.entry("TH10", 1.1)
    ));
    specs.put("angeorgieva", Map.of("TH16_API", 26.1, "AUTRES", 11.7, "TH3", 7.2, "TH14", 2.4, "TH20", 2.3, "TH1", 2.1, "TH19", 1.7));
    specs.put("atsirov", Map.of("TH7", 7.4, "TH3", 5.1, "TH10", 1.9, "AUTRES", 1.1));
    specs.put("ayacoub", Map.of("TH13", 10.1, "TH7", 6.8, "AUTRES", 5.9, "TH3", 5.6, "TH11", 3.9, "TH20", 3.5, "TH10", 2.3, "TH1", 1.6));
    specs.put("bnouaji", Map.of("TH10", 13.1));
    specs.put("eveli", Map.of("TH17_Migration", 11.1, "TH16_Interfaces", 7.4, "TH5.1", 3.0, "TH18", 1.6));
    specs.put("fsouab", Map.of("AUTRES", 3.5, "TH5.2", 1.9, "TH10", 1.3, "TH11", 1.1));
    specs.put("iatanasov", Map.of("TH5.1", 17.3, "TH7", 5.1, "TH5.2", 3.9, "TH14", 3.8, "TH11", 3.8, "TH17_Migration", 2.6, "AUTRES", 1.8, "AD", 1.2));
    specs.put("ikolchev", Map.of("TH6.2", 5.5, "TH16_API", 4.7, "TH3", 4.3, "TH19", 3.7, "TH5.1", 3.3, "AUTRES", 3.1, "TH18", 2.0, "TH6.1", 1.2));
    specs.put("kbachvarova", Map.of("TH11", 45.6, "AUTRES", 20.2, "TH17_Migration", 8.7, "TH6.2", 1.8, "TH6.3", 1.3, "TH16_API", 1.1));
    specs.put("kkomitov", Map.of("AUTRES", 2.4, "TH5.2", 1.5, "AD", 1.4));
    specs.put("kmateeva", Map.of("TH11", 47.6, "AUTRES", 5.5, "TH17_Migration", 5.3, "TH6.3", 5.2, "TH6.2", 4.8, "TH3", 3.2, "TH10", 1.7, "TH16_API", 1.1));
    specs.put("kslavchova", Map.of("TH3", 5.0, "TH5.1", 2.2));
    specs.put("mdaaji", Map.of("TH17_Migration", 27.2, "TH16_Interfaces", 2.4, "GED", 2.0, "AUTRES", 1.5));
    specs.put("mhadji", Map.of("TH20", 17.6, "TH2", 5.5, "TH16_API", 3.7, "TH1", 1.9, "TH16_Interfaces", 1.7, "TH3", 1.6, "TH18", 1.5, "TH5.1", 1.4));
    specs.put("mmrabet", Map.of("TH10", 17.1, "AUTRES", 4.7, "TH7", 2.1));
    specs.put("mniklenov", Map.of("TH7", 2.4, "TH11", 1.7));
    specs.put("msamareva", Map.of("TH11", 19.1));
    specs.put("ndelbecq", Map.ofEntries(
        Map.entry("TH6.2", 27.0),
        Map.entry("AUTRES", 12.5),
        Map.entry("TH5.1", 9.3),
        Map.entry("TH16_API", 8.6),
        Map.entry("TH5.2", 5.5),
        Map.entry("TH2", 4.9),
        Map.entry("TH6.1", 4.8),
        Map.entry("TH20", 3.9),
        Map.entry("TH17_Migration", 3.8),
        Map.entry("TH3", 2.2),
        Map.entry("TH18", 2.0),
        Map.entry("TH11", 1.6),
        Map.entry("TH16_Interfaces", 1.2)
    ));
    specs.put("rbensalem", Map.of("TH17_Migration", 4.9, "GED", 3.2, "TH2", 2.6, "AUTRES", 2.4, "TH16_API", 1.6, "TH6.2", 1.3));
    specs.put("rgospodinova", Map.of("AUTRES", 25.3));
    specs.put("rtkhayat", Map.of("TH10", 13.2, "TH13", 2.4, "TH11", 1.9, "AUTRES", 1.8, "TH6.3", 1.8));
    specs.put("sabbassi", Map.of("AUTRES", 4.5, "TH13", 2.6, "TH5.2", 1.5));
    specs.put("sbraham", Map.of("AUTRES", 11.1, "TH14", 10.9, "TH5.1", 5.0));
    specs.put("vrobert", Map.of("TH11", 4.1));
    specs.put("wfadhloun", Map.of("TH19", 6.4, "TH3", 3.5, "AUTRES", 1.9, "TRANSVERSE", 1.9));
    specs.put("ypetrov", Map.of("TH5.1", 15.4, "TH7", 3.3, "TH17_Migration", 2.4, "TH6.2", 1.8, "TH20", 1.3, "TH16_API", 1.3, "AUTRES", 1.1));

    return specs;
}
}
