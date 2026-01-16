package com.codix.tools.btteamreport;

import com.codix.tools.AppConfig;
import com.codix.tools.btteamreport.ResourcePlanningService.CapacityAlerts;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ResourcePlanningApp {

    public static void main(String[] args) {
        try {
            // Utilisation de "UTF-8" (String) au lieu de StandardCharsets pour la compatibilité Java 8
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
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
     * Version compatible Java 8.
     */
    private static Map<String, Map<String, Double>> getTeamSpecialization() {
    Map<String, Map<String, Double>> specs = new HashMap<>();
    Map<String, Double> s; // Variable temporaire pour alléger le code

        s = new HashMap<>(); s.put("AUTRES", 8.2); s.put("TH16_Interfaces", 4.0); s.put("TH2", 1.8);
        specs.put("amakki", s);

        s = new HashMap<>(); s.put("TH2", 18.0); s.put("TH16_API", 15.4); s.put("TH6.2", 13.9); s.put("AUTRES", 9.9); s.put("GED", 5.9); s.put("TH19", 3.0); s.put("TH3", 2.9); s.put("TH6.1", 2.7); s.put("TH5.1", 2.2); s.put("TH17_Migration", 1.3); s.put("TH1", 1.3); s.put("TH20", 1.1); s.put("TH10", 1.0);
        specs.put("amirchev", s);

        s = new HashMap<>(); s.put("TH16_API", 22.9); s.put("AUTRES", 11.3); s.put("TH3", 7.2); s.put("TH14", 2.3); s.put("TH19", 1.6); s.put("TH20", 1.4); s.put("TH1", 1.1);
        specs.put("angeorgieva", s);

        s = new HashMap<>(); s.put("TH7", 7.4); s.put("TH3", 5.1); s.put("AUTRES", 1.1); s.put("TH5.1", 1.0);
        specs.put("atsirov", s);

        s = new HashMap<>(); s.put("TH13", 10.8); s.put("TH7", 7.2); s.put("AUTRES", 6.3); s.put("TH3", 5.5); s.put("TH11", 4.2); s.put("TH20", 3.7); s.put("TH10", 2.3); s.put("TH1", 1.8); s.put("TH17_Migration", 1.0);
        specs.put("ayacoub", s);

        s = new HashMap<>(); s.put("TH10", 7.0);
        specs.put("bnouaji", s);

        s = new HashMap<>(); s.put("TH17_Migration", 13.5); s.put("TH16_Interfaces", 9.0); s.put("TH5.1", 3.3); s.put("TH18", 1.8);
        specs.put("eveli", s);

        s = new HashMap<>(); s.put("AUTRES", 3.5); s.put("TH10", 1.3); s.put("TH11", 1.1);
        specs.put("fsouab", s);

        s = new HashMap<>(); s.put("TH5.1", 18.2); s.put("TH5.2", 4.9); s.put("TH11", 3.9); s.put("TH14", 3.8); s.put("TH17_Migration", 3.0); s.put("AUTRES", 2.1); s.put("TH7", 1.3);
        specs.put("iatanasov", s);

        s = new HashMap<>(); s.put("TH6.2", 5.6); s.put("TH3", 4.9); s.put("TH16_API", 4.2); s.put("TH5.1", 3.4); s.put("AUTRES", 3.1); s.put("TH19", 2.8); s.put("TH18", 2.6); s.put("TH6.1", 1.3);
        specs.put("ikolchev", s);

        s = new HashMap<>(); s.put("TH11", 47.2); s.put("AUTRES", 22.9); s.put("TH17_Migration", 10.4); s.put("TH6.2", 2.7); s.put("TH6.3", 1.4); s.put("TH10", 1.0);
        specs.put("kbachvarova", s);

        s = new HashMap<>(); s.put("AUTRES", 2.4); s.put("AD", 1.5); s.put("TH5.2", 1.5);
        specs.put("kkomitov", s);

        s = new HashMap<>(); s.put("TH11", 49.6); s.put("AUTRES", 5.9); s.put("TH17_Migration", 5.4); s.put("TH6.3", 5.3); s.put("TH6.2", 4.9); s.put("TH3", 3.4); s.put("TH10", 3.0);
        specs.put("kmateeva", s);

        s = new HashMap<>(); s.put("TH3", 6.9); s.put("TH5.1", 2.0);
        specs.put("kslavchova", s);

        s = new HashMap<>(); s.put("TH17_Migration", 25.8); s.put("TH16_Interfaces", 4.0); s.put("GED", 2.2); s.put("AUTRES", 1.5); s.put("TH13", 1.4);
        specs.put("mdaaji", s);

        s = new HashMap<>(); s.put("TH20", 20.6); s.put("TH2", 5.3); s.put("TH16_API", 4.3); s.put("TH16_Interfaces", 2.5); s.put("TH1", 1.9); s.put("TH18", 1.8); s.put("TH3", 1.6); s.put("TH5.1", 1.3);
        specs.put("mhadji", s);

        s = new HashMap<>(); s.put("TH10", 9.4); s.put("TH7", 2.8); s.put("AUTRES", 1.9);
        specs.put("mmrabet", s);

        s = new HashMap<>(); s.put("TH7", 2.8); s.put("TH11", 1.9);
        specs.put("mniklenov", s);

        s = new HashMap<>(); s.put("TH11", 16.6);
        specs.put("msamareva", s);

        s = new HashMap<>(); s.put("TH6.2", 26.2); s.put("AUTRES", 12.4); s.put("TH16_API", 8.6); s.put("TH5.1", 7.5); s.put("TH5.2", 5.9); s.put("TH6.1", 5.2); s.put("TH2", 5.2); s.put("TH20", 3.9); s.put("TH17_Migration", 3.5); s.put("TH18", 3.0); s.put("TH3", 2.2); s.put("TH11", 1.6); s.put("TH16_Interfaces", 1.2);
        specs.put("ndelbecq", s);

        s = new HashMap<>(); s.put("TH17_Migration", 4.4); s.put("GED", 3.2); s.put("TH2", 2.8); s.put("AUTRES", 2.4); s.put("TH16_API", 1.6); s.put("TH6.2", 1.3);
        specs.put("rbensalem", s);

        s = new HashMap<>(); s.put("AUTRES", 25.7);
        specs.put("rgospodinova", s);

        s = new HashMap<>(); s.put("TH10", 11.3); s.put("TH13", 2.5); s.put("AUTRES", 2.0); s.put("TH6.3", 1.4);
        specs.put("rtkhayat", s);

        s = new HashMap<>(); s.put("TH5.2", 4.9); s.put("AUTRES", 4.2); s.put("TH13", 2.7); s.put("TH17_Migration", 1.0);
        specs.put("sabbassi", s);

        s = new HashMap<>(); s.put("TH14", 9.8); s.put("AUTRES", 6.0); s.put("TH5.1", 5.5);
        specs.put("sbraham", s);

        s = new HashMap<>(); s.put("TH11", 4.0);
        specs.put("vrobert", s);

        s = new HashMap<>(); s.put("TH19", 5.5); s.put("TH3", 4.1); s.put("AUTRES", 2.1); s.put("TRANSVERSE", 2.0);
        specs.put("wfadhloun", s);

        s = new HashMap<>(); s.put("TH5.1", 14.5); s.put("TH7", 3.1); s.put("TH6.2", 1.3); s.put("TH20", 1.2); s.put("TH16_API", 1.2); s.put("TH18", 1.1);
        specs.put("ypetrov", s);

        return specs;
}
}