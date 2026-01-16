package com.codix.tools.btteamreport;

import com.codix.tools.AppConfig;
import com.codix.tools.btteamreport.ResourcePlanningService.CapacityAlerts;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ResourcePlanningApp {

    public static void main(String[] args) {
        // Initialisation du bloc d'encodage Java par défaut
        try { 
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); 
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8)); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        try {
            System.out.println("=== DÉMARRAGE DE L'ANALYSE DE PLANIFICATION LOCAM ===");

            System.out.println("[1/6] Chargement de la configuration via AppConfig...");
            AppConfig config = AppConfig.getInstance();

            String jiraUrl = config.getJiraBaseUrl();
            String token = config.getApiToken();
            
            // Récupération sécurisée du cookie HR Center via la configuration uniformisée
            String hrCookie = "PHPSESSID=" + config.getHrPhpsessid();

            // Utilisation des paramètres de projet et JQL définis dans la config
            String projects = config.getTargetProjectBTTEAM(); 
            if (projects == null || projects.isEmpty()) {
                projects = "LOCAMWEB, LOCAMDEV"; // Fallback si non défini
            }
            
            String jqlOpen = config.getJqlBTTEAM();
            if (jqlOpen == null || jqlOpen.isEmpty()) {
                jqlOpen = "project = LOCAMWEB AND status NOT IN (Closed, Cancel, Pending) AND type != CRQ";
            }

            String outputFile = "DASHBOARD_BTTEAM.html";
            int nbWeeks = 8; 

            ResourcePlanningService service = new ResourcePlanningService(jiraUrl, token);
            ResourcePlanningRenderer renderer = new ResourcePlanningRenderer();

            System.out.println("[2/6] Récupération des données globales Jira (" + nbWeeks + " semaines)...");
            ResourcePlanningService.ReportData data = service.getReportData(projects, nbWeeks);

            System.out.println("[3/6] Chargement du profil de spécialisation de l'équipe...");
            Map<String, Map<String, Double>> teamSpecs = getTeamSpecialization();

            System.out.println("[4/6] Connexion dynamique au HR Center & Calcul des absences/présences...");
            Map<String, Integer> absences = service.loadHRData(hrCookie, data.planning);

            System.out.println("[5/6] Calcul de la tension et identification des alertes de capacité...");
            // Calcul basé sur 20 jours ouvrés par défaut
            CapacityAlerts capacityAlerts = service.calculateCapacityAlerts(jqlOpen, teamSpecs, data.planning);

            System.out.println("[6/6] Génération du rapport HTML final...");
            renderer.generate(data, capacityAlerts, outputFile);

            System.out.println("\n✅ ANALYSE TERMINÉE AVEC SUCCÈS");
            System.out.println("Rapport disponible : " + outputFile);

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE :");
            e.printStackTrace();
        }
    }

    /**
     * Retourne la cartographie des spécialisations par collaborateur.
     * Les valeurs représentent le poids (en jours ou % de focus) par thème.
     */
    private static Map<String, Map<String, Double>> getTeamSpecialization() {
    Map<String, Map<String, Double>> specs = new HashMap<>();

    specs.put("amakki", Map.of("AUTRES", 8.2, "TH16_Interfaces", 4.0, "TH2", 1.8));
    specs.put("amirchev", Map.ofEntries(
        Map.entry("TH2", 18.0),
        Map.entry("TH16_API", 15.4),
        Map.entry("TH6.2", 13.9),
        Map.entry("AUTRES", 9.9),
        Map.entry("GED", 5.9),
        Map.entry("TH19", 3.0),
        Map.entry("TH3", 2.9),
        Map.entry("TH6.1", 2.7),
        Map.entry("TH5.1", 2.2),
        Map.entry("TH17_Migration", 1.3),
        Map.entry("TH1", 1.3),
        Map.entry("TH20", 1.1)
    ));
    specs.put("angeorgieva", Map.of("TH16_API", 22.4, "AUTRES", 11.4, "TH3", 7.2, "TH14", 2.3, "TH19", 1.6, "TH20", 1.4, "TH1", 1.1));
    specs.put("atsirov", Map.of("TH7", 7.4, "TH3", 5.1, "AUTRES", 1.1));
    specs.put("ayacoub", Map.of("TH13", 10.8, "TH7", 7.2, "AUTRES", 6.3, "TH3", 5.5, "TH11", 4.2, "TH20", 3.7, "TH10", 2.3, "TH1", 1.8, "TH17_Migration", 1.0));
    specs.put("bnouaji", Map.of("TH10", 7.0));
    specs.put("eveli", Map.of("TH17_Migration", 13.5, "TH16_Interfaces", 9.0, "TH5.1", 3.3, "TH18", 1.8));
    specs.put("fsouab", Map.of("AUTRES", 3.5, "TH10", 1.3, "TH11", 1.1));
    specs.put("iatanasov", Map.of("TH5.1", 18.2, "TH5.2", 4.9, "TH11", 3.9, "TH14", 3.8, "TH17_Migration", 3.0, "AUTRES", 2.1, "TH7", 1.3));
    specs.put("ikolchev", Map.of("TH6.2", 5.6, "TH3", 5.0, "TH16_API", 3.9, "TH5.1", 3.4, "AUTRES", 3.1, "TH19", 2.8, "TH18", 2.6, "TH6.1", 1.3));
    specs.put("kbachvarova", Map.of("TH11", 47.2, "AUTRES", 23.0, "TH17_Migration", 10.4, "TH6.2", 2.7, "TH6.3", 1.4));
    specs.put("kkomitov", Map.of("AUTRES", 2.4, "TH5.2", 1.5, "AD", 1.5));
    specs.put("kmateeva", Map.of("TH11", 49.6, "AUTRES", 5.9, "TH17_Migration", 5.4, "TH6.3", 5.3, "TH6.2", 4.9, "TH3", 3.4, "TH10", 2.7));
    specs.put("kslavchova", Map.of("TH3", 6.9, "TH5.1", 2.0));
    specs.put("mdaaji", Map.of("TH17_Migration", 25.8, "TH16_Interfaces", 4.0, "GED", 2.2, "AUTRES", 1.5, "TH13", 1.4));
    specs.put("mhadji", Map.of("TH20", 20.6, "TH2", 5.3, "TH16_API", 4.3, "TH16_Interfaces", 2.5, "TH1", 1.9, "TH18", 1.8, "TH3", 1.6, "TH5.1", 1.3));
    specs.put("mmrabet", Map.of("TH10", 9.2, "TH7", 2.8, "AUTRES", 1.9));
    specs.put("mniklenov", Map.of("TH7", 2.8, "TH11", 1.9));
    specs.put("msamareva", Map.of("TH11", 16.8));
    specs.put("ndelbecq", Map.ofEntries(
        Map.entry("TH6.2", 26.2),
        Map.entry("AUTRES", 12.4),
        Map.entry("TH16_API", 8.6),
        Map.entry("TH5.1", 7.5),
        Map.entry("TH5.2", 5.9),
        Map.entry("TH6.1", 5.2),
        Map.entry("TH2", 5.2),
        Map.entry("TH20", 3.9),
        Map.entry("TH17_Migration", 3.5),
        Map.entry("TH18", 3.0),
        Map.entry("TH3", 2.2),
        Map.entry("TH11", 1.6),
        Map.entry("TH16_Interfaces", 1.2)
    ));
    specs.put("rbensalem", Map.of("TH17_Migration", 4.4, "GED", 3.2, "TH2", 2.8, "AUTRES", 2.4, "TH16_API", 1.6, "TH6.2", 1.3));
    specs.put("rgospodinova", Map.of("AUTRES", 25.7));
    specs.put("rtkhayat", Map.of("TH10", 11.3, "TH13", 2.5, "AUTRES", 2.0, "TH6.3", 1.4));
    specs.put("sabbassi", Map.of("TH5.2", 4.9, "AUTRES", 4.2, "TH13", 2.7));
    specs.put("sbraham", Map.of("TH14", 9.8, "AUTRES", 6.0, "TH5.1", 5.5));
    specs.put("vrobert", Map.of("TH11", 4.2));
    specs.put("wfadhloun", Map.of("TH19", 5.5, "TH3", 4.1, "AUTRES", 2.1, "TRANSVERSE", 2.0));
    specs.put("ypetrov", Map.of("TH5.1", 14.6, "TH7", 3.1, "TH6.2", 1.3, "TH20", 1.2, "TH16_API", 1.2, "TH18", 1.1));

    return specs;
}
}