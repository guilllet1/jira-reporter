// VERSION: V27 - JIRA UTILS - PARTIE 1
package com.codix.tools.management;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraUtils {

    public static final String STATE_FILE = "tickets_state.json";
    public static final List<String> IGNORED_AUTHORS = Arrays.asList("MMPI Auto", "Mail Distributor", "hotline_client", "System", "JIRA");
    public static final Set<String> THEMES = Set.of(
            "AD", "ELLISPHERE", "GED", "TH1", "TH10", "TH11", "TH12", "TH13", "TH14",
            "TH16_API", "TH16_Interfaces", "TH17_Migration", "TH18", "TH19", "TH2",
            "TH20", "TH3", "TH4", "TH5.1", "TH5.2", "TH6.1", "TH6.2", "TH6.3",
            "TH7", "TH8", "TRANSVERSE"
    );

    static long totalInputTokens = 0;
    static long totalOutputTokens = 0;

    // --- API JIRA ---

    public static Set<String> searchJiraTickets(HttpClient client, AppConfig config) {
        Set<String> tickets = new HashSet<>();
        int startAt = 0; 
        boolean hasMore = true;
        
        String baseUrl = config.getJiraBaseUrl(); 
        if (baseUrl == null) { System.err.println("URL Jira manquante"); return tickets; }
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        
        // Construction dynamique du nom de projet : "LOCAM" -> "LOCAMWEB"
        String targetProjectWeb = config.getTargetProject() + "WEB";
        
        String jql = "project = " + targetProjectWeb + " AND status in (Open, Reopened)";
        jql += " AND ((\"Statut Codix\" != \"ATT Livraison\" or \"Statut Codix\" is EMPTY) or (\"Codix status\" != \"For Delivery\" or \"Codix status\" is EMPTY))";

        if (config.getTargetLabels() != null && config.getTargetLabels().length > 0 && !config.getTargetLabels()[0].isEmpty()) {
            jql += " AND labels in (\"" + String.join("\",\"", config.getTargetLabels()) + "\")";
        }
        
        System.out.println("JQL : " + jql);

        while (hasMore) {
            try {
                String url = baseUrl + "search?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&startAt=" + startAt + "&maxResults=50&fields=key";
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + config.getApiToken()).header("Accept", "application/json").timeout(Duration.ofSeconds(config.getHttpTimeout())).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    JSONArray issues = json.getJSONArray("issues");
                    for (int i = 0; i < issues.length(); i++) tickets.add(issues.getJSONObject(i).getString("key"));
                    if ((startAt += issues.length()) >= json.getInt("total")) hasMore = false;
                    System.out.print(".");
                } else { hasMore = false; }
            } catch (Exception e) { hasMore = false; }
        }
        System.out.println();
        return tickets;
    }
    
    // Notez l'ajout du paramètre 'config'
    public static Set<String> extractLinksToSet(String jsonBody, AppConfig config) {
        Set<String> targetSet = new HashSet<>();
        
        // Construction dynamique des préfixes : "LOCAM" -> "LOCAMWEB-" et "LOCAMDEV-"
        String prefixWeb = config.getTargetProject() + "WEB-";
        String prefixDev = config.getTargetProject() + "DEV-";
        
        try {
            JSONArray links = new JSONObject(jsonBody).getJSONObject("fields").getJSONArray("issuelinks");
            for (int i = 0; i < links.length(); i++) {
                JSONObject l = links.getJSONObject(i);
                String k = l.has("inwardIssue") ? l.getJSONObject("inwardIssue").getString("key") : (l.has("outwardIssue") ? l.getJSONObject("outwardIssue").getString("key") : null);
                
                // Vérification dynamique
                if (k != null && (k.startsWith(prefixWeb) || k.startsWith(prefixDev))) {
                    targetSet.add(k);
                }
            }
        } catch (Exception e) {}
        return targetSet;
    }

    public static String getOrFetchJson(HttpClient client, AppConfig config, String ticketRef, Map<String, String> cache) {
        if (cache.containsKey(ticketRef)) return cache.get(ticketRef);
        try {
            String baseUrl = config.getJiraBaseUrl(); if (!baseUrl.endsWith("/")) baseUrl += "/";
            
            // --- MODIFICATION ICI : Ajout de customfield_10601 dans la liste des champs ---
            String url = baseUrl + "issue/" + ticketRef + "?fields=customfield_12000,customfield_10602,customfield_10601,customfield_10606,customfield_10220,customfield_12718,duedate,description,customfield_12701,labels,summary,comment,assignee,issuelinks,status,updated,created,priority,issuetype&expand=changelog";
            
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + config.getApiToken()).header("Accept", "application/json").timeout(Duration.ofSeconds(config.getHttpTimeout())).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                cache.put(ticketRef, response.body());
                return response.body();
            }
        } catch (Exception e) { }
        return null;
    }

    // --- PARSING ---

    public static void parseTicketData(String key, String jsonBody, JiraDownloader.TicketData data, boolean isMain) {
        try {
            JSONObject fields = new JSONObject(jsonBody).getJSONObject("fields");
            if (isMain) {
                data.summary = fixEncoding(removeHtmlTags(fields.optString("summary", "Sans titre")));
                data.description = extensiveCleaning(fixEncoding(fields.optString("description", "")));
                data.status = fields.optJSONObject("status") != null ? fixEncoding(fields.getJSONObject("status").getString("name")) : "Inconnu";
                data.priority = fields.optJSONObject("priority") != null ? fixEncoding(fields.getJSONObject("priority").getString("name")) : "Non définie";
                data.issueType = fields.optJSONObject("issuetype") != null ? fixEncoding(fields.getJSONObject("issuetype").getString("name")) : "";

                if (fields.has("customfield_10220")) data.codixCategory = fixEncoding(fields.optJSONObject("customfield_10220") != null ? fields.getJSONObject("customfield_10220").optString("value") : fields.optString("customfield_10220"));
                
                // --- LOGIQUE CLIENT STATUS (10602 OU 10601) ---
                String valClientStatus = "";
                
                // 1. Essai avec customfield_10602
                if (fields.has("customfield_10602")) {
                    valClientStatus = fields.optJSONObject("customfield_10602") != null ? 
                                      fields.getJSONObject("customfield_10602").optString("value") : 
                                      fields.optString("customfield_10602");
                }

                // 2. Si vide, essai avec customfield_10601
                if ((valClientStatus == null || valClientStatus.isEmpty() || "null".equals(valClientStatus)) && fields.has("customfield_10601")) {
                     valClientStatus = fields.optJSONObject("customfield_10601") != null ? 
                                       fields.getJSONObject("customfield_10601").optString("value") : 
                                       fields.optString("customfield_10601");
                }
                
                data.clientStatus = fixEncoding(valClientStatus);
                // ----------------------------------------------

                // --- LAST KNOWN STATE (12000) ---
                if (fields.has("customfield_12000")) {
                    String raw = fixEncoding(fields.optString("customfield_12000", ""));
                    data.lastKnownState = extensiveCleaning(raw);
                }

                if (fields.has("customfield_10606")) data.ddca = fields.optString("customfield_10606");
                if (fields.has("duedate")) data.dueDate = fields.optString("duedate");

                data.lastUpdatedJira = fields.getString("updated");
                data.updatedToday = data.lastUpdatedJira.substring(0, 10).equals(LocalDate.now().toString());

                String dateAssign = fields.optString("created", "2000-01-01").substring(0, 10);
                if (!fields.isNull("customfield_12701") && fields.getString("customfield_12701").length() >= 10) {
                    dateAssign = fields.getString("customfield_12701").substring(0, 10);
                }
                data.clientAssignationDate = dateAssign;

                data.theme = "Aucun";
                if (fields.has("labels")) {
                    JSONArray labels = fields.getJSONArray("labels");
                    for (int i = 0; i < labels.length(); i++) {
                        String lbl = fixEncoding(labels.getString(i));
                        if (THEMES.contains(lbl)) data.theme = lbl;
                    }
                }
                data.historyRaw = "";
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static JiraDownloader.DevLink parseDevLink(String linkedKey, String linkedJson, String mainJson) {
        try {
            JSONObject fields = new JSONObject(linkedJson).getJSONObject("fields");
            JiraDownloader.DevLink link = new JiraDownloader.DevLink();
            link.key = linkedKey;
            link.summary = fixEncoding(removeHtmlTags(fields.optString("summary", "")));
            link.status = fields.optJSONObject("status") != null ? fixEncoding(fields.getJSONObject("status").getString("name")) : "Inconnu";
            link.priority = fields.optJSONObject("priority") != null ? fixEncoding(fields.getJSONObject("priority").getString("name")) : "Non définie";
            link.type = fields.optJSONObject("issuetype") != null ? fixEncoding(fields.getJSONObject("issuetype").getString("name")) : "";
            link.linkType = "unknown";

            JSONArray links = new JSONObject(mainJson).getJSONObject("fields").getJSONArray("issuelinks");
            for (int i = 0; i < links.length(); i++) {
                JSONObject l = links.getJSONObject(i);
                if (l.has("inwardIssue") && l.getJSONObject("inwardIssue").getString("key").equals(linkedKey)) {
                    link.linkType = l.getJSONObject("type").getString("inward"); break;
                } else if (l.has("outwardIssue") && l.getJSONObject("outwardIssue").getString("key").equals(linkedKey)) {
                    link.linkType = l.getJSONObject("type").getString("outward"); break;
                }
            }
            return link;
        } catch (Exception e) { return null; }
    }

    public static void extractHistoryRaw(JiraDownloader.TicketData data, String mainJson, Set<String> linkedRefs, HttpClient client, AppConfig config, Map<String, String> cache) {
        List<JSONObject> allComments = new ArrayList<>();
        collectRawComments(mainJson, data.key, allComments);
        for (String linkedRef : linkedRefs) {
            if (!linkedRef.equals(data.key)) {
                String linkedJson = getOrFetchJson(client, config, linkedRef, cache);
                if (linkedJson != null) collectRawComments(linkedJson, linkedRef, allComments);
            }
        }
        allComments.sort((c1, c2) -> {
            OffsetDateTime d1 = parseJiraDate(c1.getString("created"));
            OffsetDateTime d2 = parseJiraDate(c2.getString("created"));
            return d1 != null && d2 != null ? d1.compareTo(d2) : 0;
        });

        StringBuilder historyBuilder = new StringBuilder();
        for (JSONObject c : allComments) {
            String author = fixEncoding(c.getJSONObject("author").getString("displayName"));
            if (IGNORED_AUTHORS.contains(author)) continue;
            String rawBody = fixEncoding(c.getString("body"));
            if ("Dimitare Ivanov".equalsIgnoreCase(author)) rawBody = rawBody.replaceAll("(?s)<pre>.*?</pre>", " [LOGS] ");
            String cleanedBody = extensiveCleaning(rawBody);
            if (!cleanedBody.isEmpty()) historyBuilder.append(String.format("[%s] [%s] %s:\n%s\n----------------\n", c.getString("created").substring(0, 16).replace("T", " "), c.getString("sourceKey"), author, cleanedBody));
        }
        data.historyRaw += historyBuilder.toString();
    }

    public static void collectRawComments(String json, String key, List<JSONObject> collector) {
        try {
            JSONObject fields = new JSONObject(json).getJSONObject("fields");
            if (!fields.isNull("comment")) {
                JSONArray arr = fields.getJSONObject("comment").getJSONArray("comments");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.getJSONObject(i);
                    c.put("sourceKey", key);
                    collector.add(c);
                }
            }
        } catch (Exception e) {}
    }

    // --- XML & AI ---

    // VERSION SÉCURISÉE (JiraUtils.java)
    public static String generateXsdXml(JiraDownloader.TicketData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ticket>\n");
        sb.append("  <key>").append(data.key).append("</key>\n");
        sb.append("  <summary><![CDATA[").append(data.summary.replace("]]>", "]]&gt;")).append("]]></summary>\n");
        
        if (data.description != null && !data.description.isEmpty()) {
            sb.append("  <description><![CDATA[").append(data.description.replace("]]>", "]]&gt;")).append("]]></description>\n");
        }
        
        if (data.dueDate != null) sb.append("  <dueDate>").append(data.dueDate).append("</dueDate>\n");
        sb.append("  <issueType>").append(escapeXML(data.issueType)).append("</issueType>\n");
        sb.append("  <priority>").append(escapeXML(data.priority)).append("</priority>\n");
        
        if (data.codixCategory != null) sb.append("  <codixCategory>").append(escapeXML(data.codixCategory)).append("</codixCategory>\n");
        sb.append("  <status>").append(escapeXML(data.status)).append("</status>\n");
        
        // --- CHAMPS CUSTOM ---
        if (data.clientStatus != null) sb.append("  <clientStatus>").append(escapeXML(data.clientStatus)).append("</clientStatus>\n");
        
        // UTILISATION DE last_known_state (Sécurisé contre le NullPointerException)
        // Si null, on met une chaine vide "" pour éviter le crash sur .replace()
        String safeLastKnown = (data.lastKnownState != null) ? data.lastKnownState.replace("]]>", "]]&gt;") : "";
        sb.append("  <last_known_state><![CDATA[").append(safeLastKnown).append("]]></last_known_state>\n");
        
        // ---------------------

        if (!data.devLinks.isEmpty()) {
            sb.append("  <devLinks>\n");
            for (JiraDownloader.DevLink link : data.devLinks) {
                sb.append("    <dev>\n");
                sb.append("      <key>").append(link.key).append("</key>\n");
                sb.append("      <summary><![CDATA[").append(link.summary.replace("]]>", "]]&gt;")).append("]]></summary>\n");
                sb.append("      <status>").append(escapeXML(link.status)).append("</status>\n");
                sb.append("      <priority>").append(escapeXML(link.priority)).append("</priority>\n");
                sb.append("      <type>").append(escapeXML(link.type)).append("</type>\n");
                sb.append("      <linkType>").append(escapeXML(link.linkType)).append("</linkType>\n");
                sb.append("    </dev>\n");
            }
            sb.append("  </devLinks>\n");
        }
        sb.append("  <history_raw><![CDATA[\n");
        if (data.historyRaw.length() > 100000) {
            sb.append("... [TRONQUÉ] ...\n").append(data.historyRaw.substring(data.historyRaw.length() - 100000));
        } else {
            sb.append(data.historyRaw);
        }
        sb.append("\n  ]]></history_raw>\n");
        sb.append("</ticket>");
        return sb.toString();
    }

    public static String callAI(HttpClient client, AppConfig config, String historyText) {
        // 1. Définition de la langue (défaut : français)
        String langInstruction = "english".equalsIgnoreCase(config.getAiLanguage())
                ? "\nIMPORTANT: You MUST answer in ENGLISH."
                : "\nIMPORTANT: Tu DOIS répondre en FRANÇAIS.";

        String provider = config.getAIProvider();

        if ("openai".equalsIgnoreCase(provider)) {
            // Pour OpenAI, on ajoute l'instruction de langue à l'historique utilisateur.
            // (Le Prompt Système est déjà ajouté à l'intérieur de callOpenAI via config.getAIPrompt())
            return callOpenAI(client, config, langInstruction + "\n\n" + historyText);
        } else {
            // Pour Gemini, on assemble TOUT ici (System Prompt + Langue + Historique)
            // car callGemini ci-dessous envoie le texte brut sans modification.
            String finalPrompt = config.getAIPrompt() + langInstruction + "\n\n" + historyText;
            return callGemini(client, config, finalPrompt);
        }
    }

    private static String callGemini(HttpClient client, AppConfig config, String fullTextToSend) {
        int maxRetries = config.getMaxRetries();
        int timeoutSec = config.getHttpTimeout();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Modification : On utilise 'fullTextToSend' directement (il contient déjà tout le contexte)
                JSONObject textPart = new JSONObject().put("text", fullTextToSend);
                JSONObject jsonBodyObj = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(textPart))));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + config.getGeminiModel() + ":generateContent?key=" + config.getGeminiApiKey()))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBodyObj.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    if (json.has("usageMetadata")) {
                        totalInputTokens += json.getJSONObject("usageMetadata").optLong("promptTokenCount", 0);
                        totalOutputTokens += json.getJSONObject("usageMetadata").optLong("candidatesTokenCount", 0);
                    }
                    return json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                } 
                else if (response.statusCode() >= 500 || response.statusCode() == 429) {
                    System.out.print("(Retry " + response.statusCode() + ") ");
                    Thread.sleep(2000 * attempt);
                } 
                else {
                    return "[API_ERROR] Gemini (" + response.statusCode() + "): " + response.body();
                }
            } catch (Exception e) {
                if (attempt == maxRetries) return "[API_ERROR] " + e.getMessage();
                try { Thread.sleep(2000 * attempt); } catch (InterruptedException ie) {}
            }
        }
        return "[API_ERROR] Timeout";
    }

    private static String callOpenAI(HttpClient client, AppConfig config, String historyText) {
        int maxRetries = config.getMaxRetries();
        int timeoutSec = config.getHttpTimeout();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                JSONObject jsonBody = new JSONObject().put("model", config.getOpenAIModel()).put("messages", new JSONArray().put(new JSONObject().put("role", "system").put("content", config.getAIPrompt())).put(new JSONObject().put("role", "user").put("content", historyText)));
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .header("Content-Type", "application/json").header("Authorization", "Bearer " + config.getOpenAIApiKey())
                        .timeout(Duration.ofSeconds(timeoutSec)).POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString(), StandardCharsets.UTF_8)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    if (json.has("usage")) {
                        totalInputTokens += json.getJSONObject("usage").optLong("prompt_tokens", 0);
                        totalOutputTokens += json.getJSONObject("usage").optLong("completion_tokens", 0);
                    }
                    return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                }
                if (response.statusCode() >= 500 || response.statusCode() == 429) Thread.sleep(2000 * attempt);
                else return "[API_ERROR] OpenAI (" + response.statusCode() + "): " + response.body();
            } catch (Exception e) { if (attempt == maxRetries) return "[API_ERROR] " + e.getMessage(); }
        }
        return "[API_ERROR] Timeout";
    }

    // --- UTILS & STATE ---

    public static void printCostEstimation(String provider, String model) {
        double priceInput = 0.0, priceOutput = 0.0;
        model = model.toLowerCase();
        if (provider.equalsIgnoreCase("openai")) {
            if (model.contains("gpt-4o-mini")) { priceInput = 0.15; priceOutput = 0.60; }
            else if (model.contains("gpt-4o")) { priceInput = 2.50; priceOutput = 10.00; }
            else { priceInput = 2.50; priceOutput = 10.00; }
        } else {
            if (model.contains("flash")) { priceInput = 0.075; priceOutput = 0.30; }
            else { priceInput = 2.00; priceOutput = 12.00; }
        }
        double cost = ((totalInputTokens / 1e6) * priceInput) + ((totalOutputTokens / 1e6) * priceOutput);
        System.out.println("\n--- ESTIMATION COÛT (" + model + ") ---");
        System.out.println("Input: " + totalInputTokens + " | Output: " + totalOutputTokens);
        System.out.println(String.format("TOTAL: $%.5f (~ %.4f €)", cost, cost * 0.95));
        System.out.println("-------------------------------------");
    }

    public static void saveToState(JSONObject state, String key, String updatedDate, String analysis, String status) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("lastUpdated", updatedDate);
            entry.put("analysis", analysis);
            entry.put("lastStatus", status);
            state.put(key, entry);
            writeStateToDisk(state);
        } catch (Exception e) {}
    }

    public static void writeStateToDisk(JSONObject state) {
        try { Files.writeString(Paths.get(STATE_FILE), state.toString(2), StandardCharsets.UTF_8); } catch (Exception e) {}
    }

    public static JSONObject loadState() {
        try {
            if (Files.exists(Paths.get(STATE_FILE))) return new JSONObject(Files.readString(Paths.get(STATE_FILE), StandardCharsets.UTF_8));
        } catch (Exception e) {}
        return new JSONObject();
    }

    public static String extensiveCleaning(String input) {
        if (input == null || input.isEmpty()) return "";
        String text = input.replaceAll("(?s)<pre.*?>.*?</pre>", " ").replaceAll("<img[^>]+>", "");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>", "\n").replaceAll("(?i)</div>", "\n").replaceAll("(?i)</h1>|</h2>|</h3>|</h4>|</h5>|</h6>", "\n");
        text = removeHtmlTags(text);
        text = text.replaceAll("https?://\\S*?([A-Z0-9]+-\\d+)\\S*", "$1").replaceAll("https?://\\S+", "");
        text = text.replaceAll("(?im)^\\s*(bonjour|salut|hello|merci(\\sd’avance|\\spour\\s(votre|ton)\\sretour)?|cordialement|br|best regards|regards|crdl)\\b[^\\n]*\\n?", "");
        StringBuilder cleanBuilder = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.matches(".*\"[\\w-]+\"\\s*:.*") || trimmed.matches("^[\\{\\}\\[\\]\\,]+$")) continue;
            if (trimmed.matches("(?i)^(POST|PUT|GET|DELETE|PATCH).*") || trimmed.startsWith("//")) continue;
            cleanBuilder.append(trimmed).append("\n");
        }
        return cleanBuilder.toString().trim();
    }

    public static String removeHtmlTags(String input) {
        if (input == null || input.isEmpty()) return "";
        String text = input.replaceAll("<[^>]+>", "");
        int passes = 0; String previous;
        do {
            previous = text; passes++;
            Pattern p = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
            Matcher m = p.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                try {
                    int codePoint = !m.group(1).isEmpty() ? Integer.parseInt(m.group(2), 16) : Integer.parseInt(m.group(2));
                    m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
                } catch (Exception e) { m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0))); }
            }
            m.appendTail(sb);
            text = sb.toString().replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'")
                    .replace("&lt;", "<").replace("&gt;", ">").replace("&euro;", "€").replace("&pound;", "£")
                    .replace("&copy;", "©").replace("&reg;", "®");
        } while (!text.equals(previous) && passes < 3);
        return text.replace('\u00A0', ' ');
    }

    public static String escapeXML(String str) {
        return str == null ? "" : str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String fixEncoding(String text) {
        try { return (text != null && (text.contains("Ã©") || text.contains("Ã¨"))) ? new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8) : (text == null ? "" : text); } catch (Exception e) { return text; }
    }

    public static String extractUpdatedDate(String jsonBody) {
        try { return new JSONObject(jsonBody).getJSONObject("fields").getString("updated"); } catch (Exception e) { return null; }
    }

    public static OffsetDateTime parseJiraDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return OffsetDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        } catch (Exception e) {
            try { return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME); } catch (Exception ex) { return null; }
        }
    }
}