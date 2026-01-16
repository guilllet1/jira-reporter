package com.codix.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static AppConfig instance;

    // Jira Settings
    private String jiraBaseUrl;
    private String apiToken;
    private String targetProject;
    private String targetProjectBTTEAM;
    private String jql;
    private String jqlBTTEAM;
    private String[] targetLabels;
    private String ticketPattern;

    // Session & Cookies
    private String ssmtCookie;
    private String hrPhpsessid;

    // IA Settings
    private String aiProvider;
    private String aiLanguage;
    private String openAIApiKey;
    private String openAIModel;
    private String geminiApiKey;
    private String geminiModel;
    private String aiPrompt;

    // App & HTTP Settings
    private boolean debugMode;
    private int httpTimeout;
    private int maxRetries;

    private AppConfig() {
        Properties props = new Properties();
        try (InputStream is = findConfigStream()) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("ERREUR : Fichier " + CONFIG_FILE + " introuvable.");
            }
        } catch (IOException e) {
            System.err.println("ERREUR : Impossible de lire la configuration.");
        }

        // Mapping des propriétés
        this.jiraBaseUrl = props.getProperty("jira.url");
        this.apiToken = props.getProperty("jira.token");
        this.targetProject = props.getProperty("jira.target.project");
        this.targetProjectBTTEAM = props.getProperty("jira.target.projectBTTEAM");
        this.jql = props.getProperty("jql");
        this.jqlBTTEAM = props.getProperty("jqlBTTEAM");
        this.ticketPattern = props.getProperty("ticket.pattern", "[A-Z]+-\\d+");
        
        String labels = props.getProperty("jira.target.labels", "");
        this.targetLabels = labels.isEmpty() ? new String[0] : labels.split(",");

        this.ssmtCookie = props.getProperty("ssmt.session.cookie");
        this.hrPhpsessid = props.getProperty("hr.phpsessid");

        this.aiProvider = props.getProperty("ai.provider", "gemini");
        this.aiLanguage = props.getProperty("ai.language", "french");
        this.openAIApiKey = props.getProperty("openai.api.key");
        this.openAIModel = props.getProperty("openai.model", "gpt-4o");
        this.geminiApiKey = props.getProperty("gemini.api.key");
        this.geminiModel = props.getProperty("gemini.model", "gemini-1.5-flash");
        this.aiPrompt = props.getProperty("gemini.prompt", "Résumé factuel.").replace("\\n", "\n");

        this.debugMode = Boolean.parseBoolean(props.getProperty("app.debug", "false"));
        this.httpTimeout = Integer.parseInt(props.getProperty("http.timeout", "30"));
        this.maxRetries = Integer.parseInt(props.getProperty("http.max.retries", "1"));
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private InputStream findConfigStream() throws IOException {
        try {
            return new FileInputStream(CONFIG_FILE);
        } catch (IOException e) {
            return getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        }
    }

    // Getters
    public String getJiraBaseUrl() { return jiraBaseUrl; }
    public String getApiToken() { return apiToken; }
    public String getTargetProject() { return targetProject; }
    public String getJql() { return jql; }
    public String getTargetProjectBTTEAM() { return targetProjectBTTEAM; }
    public String getJqlBTTEAM() { return jqlBTTEAM; }
    public String[] getTargetLabels() { return targetLabels; }
    public String getTicketPattern() { return ticketPattern; }
    public String getSsmtCookie() { return ssmtCookie; }
    public String getHrPhpsessid() { return hrPhpsessid; }
    public String getAIProvider() { return aiProvider; }
    public String getAiLanguage() { return aiLanguage; }
    public String getOpenAIApiKey() { return openAIApiKey; }
    public String getOpenAIModel() { return openAIModel; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getGeminiModel() { return geminiModel; }
    public String getAIPrompt() { return aiPrompt; }
    public boolean isDebugMode() { return debugMode; }
    public int getHttpTimeout() { return httpTimeout; }
    public int getMaxRetries() { return maxRetries; }
}