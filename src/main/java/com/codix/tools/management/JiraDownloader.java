package com.codix.tools.management;

import com.codix.tools.AppConfig;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class JiraDownloader {

    private static final String LOG_DIR = "logs";

    public static class TicketData {
        public String key;
        public String summary;
        public String issueType;
        public String priority;
        public String codixCategory;
        public String status;
        public String clientStatus;
        public String lastKnownState;
        public String dueDate;
        public String description;
        public String theme;
        public String clientAssignationDate;
        public boolean updatedToday;
        public String lastUpdatedJira;
        public String ddca;
        public List<DevLink> devLinks = new ArrayList<>();
        public String historyRaw;
        public String clusterMaxDate;
        public String aiAnalysis;
        public String oldAnalysis;
        public boolean aiCalled;
    }

    public static class DevLink {
        public String key;
        public String summary;
        public String status;
        public String priority;
        public String type;
        public String linkType;
    }

    public static void main(String[] args) {
        // Bloc d'initialisation obligatoire (UTF-8)
        try { 
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); 
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8)); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        // Récupération de l'instance unique de configuration
        AppConfig config = AppConfig.getInstance();
        String provider = config.getAIProvider();

        if (config.getApiToken() == null) {
            System.err.println("ERREUR : Token Jira manquant.");
            return;
        }

        if (config.isDebugMode()) {
            try {
                Path logPath = Paths.get(LOG_DIR);
                if (!Files.exists(logPath)) Files.createDirectories(logPath);
            } catch (Exception e) { System.err.println("Erreur log: " + e.getMessage()); }
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getHttpTimeout()))
                .build();
        
        List<TicketData> processedDataList = new ArrayList<>();
        Map<String, String> jsonCache = new HashMap<>();

        org.json.JSONObject stateMemory = JiraUtils.loadState();
        System.out.println("0. Mémoire chargée (" + stateMemory.length() + " tickets). Mode IA: " + provider.toUpperCase());

        System.out.println("1. Recherche des tickets dans Jira...");
        Set<String> jiraTickets = JiraUtils.searchJiraTickets(client, config);

        if (jiraTickets.isEmpty()) {
            System.out.println("\n[INFO] Aucun ticket trouvé.");
            return;
        }
        System.out.println("   -> " + jiraTickets.size() + " tickets trouvés.");

        // Nettoyage de la mémoire locale (supprime les tickets qui ne sont plus dans le scope JQL)
        List<String> ticketsToRemove = new ArrayList<>();
        Iterator<String> keys = stateMemory.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!jiraTickets.contains(key)) ticketsToRemove.add(key);
        }
        if (!ticketsToRemove.isEmpty()) {
            for (String key : ticketsToRemove) stateMemory.remove(key);
            JiraUtils.writeStateToDisk(stateMemory);
        }

        System.out.println("2. Traitement...");

        for (String ticketRef : jiraTickets) {
            TicketData data = new TicketData();
            data.key = ticketRef;

            String mainJson = JiraUtils.getOrFetchJson(client, config, ticketRef, jsonCache);
            if (mainJson == null) continue;

            Set<String> linkedTicketsRefs = JiraUtils.extractLinksToSet(mainJson, config);
            JiraUtils.parseTicketData(ticketRef, mainJson, data, true);

            String currentMaxDate = data.lastUpdatedJira;

            for (String linkedRef : linkedTicketsRefs) {
                if (!linkedRef.equals(ticketRef)) {
                    String linkedJson = JiraUtils.getOrFetchJson(client, config, linkedRef, jsonCache);
                    if (linkedJson != null) {
                        String linkedDate = JiraUtils.extractUpdatedDate(linkedJson);
                        if (linkedDate != null && linkedDate.compareTo(currentMaxDate) > 0) {
                            currentMaxDate = linkedDate;
                        }
                        DevLink link = JiraUtils.parseDevLink(linkedRef, linkedJson, mainJson);
                        if (link != null) data.devLinks.add(link);
                    }
                }
            }
            data.clusterMaxDate = currentMaxDate;

            boolean needAICall = true;
            if (stateMemory.has(ticketRef)) {
                org.json.JSONObject memory = stateMemory.getJSONObject(ticketRef);
                String storedDate = memory.optString("lastUpdated", null);
                data.oldAnalysis = memory.optString("analysis", "Aucune analyse précédente.");
                
                // On utilise le cache si la date de mise à jour n'a pas bougé
                if (data.clusterMaxDate != null && data.clusterMaxDate.equals(storedDate) && !data.oldAnalysis.isEmpty()) {
                    data.aiAnalysis = data.oldAnalysis;
                    needAICall = false;
                }
            }

            data.aiCalled = needAICall;
            JiraUtils.extractHistoryRaw(data, mainJson, linkedTicketsRefs, client, config, jsonCache);

            if (config.isDebugMode()) {
                try {
                    String debugXmlContent = JiraUtils.generateXsdXml(data);
                    Files.writeString(Paths.get(LOG_DIR, ticketRef + ".xml"), debugXmlContent, StandardCharsets.UTF_8);
                } catch (Exception e) { }
            }

            if (needAICall) {
                System.out.print("   -> " + ticketRef + " : MODIFIÉ -> Appel " + provider + "... ");
                String promptContext = JiraUtils.generateXsdXml(data);
                data.aiAnalysis = JiraUtils.fixEncoding(JiraUtils.callAI(client, config, promptContext));

                if (data.aiAnalysis.startsWith("[API_ERROR]")) {
                    System.out.println("ECHEC");
                    System.err.println("      [DETAIL] : " + data.aiAnalysis);
                } else {
                    System.out.println("OK");
                    JiraUtils.saveToState(stateMemory, ticketRef, data.clusterMaxDate, data.aiAnalysis, data.status);
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
            } else {
                System.out.println("   -> " + ticketRef + " : Cache local.");
                JiraUtils.saveToState(stateMemory, ticketRef, data.clusterMaxDate, data.aiAnalysis, data.status);
            }
            processedDataList.add(data);
        }

        System.out.println("3. Génération du rapport HTML...");
        JiraHtmlReport.generate(processedDataList, "DASHBOARD_MANAGEMENT.html");

        System.out.println("Terminé !");
        
        // Estimation des coûts basée sur le modèle configuré
        String currentModel = "openai".equalsIgnoreCase(provider) ? config.getOpenAIModel() : config.getGeminiModel();
        JiraUtils.printCostEstimation(provider, currentModel);
    }
}