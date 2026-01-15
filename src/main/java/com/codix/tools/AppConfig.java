package com.codix.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "config.properties";

    private String jiraBaseUrl;
    private String apiToken;
    private String targetProject;
    private String[] targetLabels;
    
    // IA Settings
    private String aiProvider; // "openai" ou "gemini"
    private String aiLanguage;
    private String openAIApiKey;
    private String openAIModel;
    private String geminiApiKey;
    private String geminiModel;
    
    private String aiPrompt;
    private boolean debugMode;

    // Paramètres HTTP
    private int httpTimeout;
    private int maxRetries;

    public AppConfig() {
        Properties props = new Properties();
        
        // 1. Essai de chargement depuis la racine (ex: lancement via IDE ou 'mvn exec:java')
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            // 2. Si échec, essai depuis le classpath (ex: JAR packagé)
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (is != null) {
                    props.load(is);
                } else {
                    System.err.println("ERREUR CRITIQUE : Fichier " + CONFIG_FILE + " introuvable !");
                    return;
                }
            } catch (IOException ex) {
                System.err.println("ERREUR CRITIQUE : Impossible de lire la configuration.");
                return;
            }
        }

        // --- LECTURE DES PROPRIÉTÉS ---
        
        // Jira
        this.jiraBaseUrl = props.getProperty("jira.url");
        this.apiToken = props.getProperty("jira.token");
        this.targetProject = props.getProperty("jira.target.project");
        
        String labels = props.getProperty("jira.target.labels", "");
        this.targetLabels = labels.isEmpty() ? new String[0] : labels.split(",");

        // IA Provider
        this.aiProvider = props.getProperty("ai.provider", "gemini");
        this.aiLanguage = props.getProperty("ai.language", "french");
        
        // OpenAI
        this.openAIApiKey = props.getProperty("openai.api.key");
        this.openAIModel = props.getProperty("openai.model", "gpt-4o");
        
        // Gemini
        this.geminiApiKey = props.getProperty("gemini.api.key");
        this.geminiModel = props.getProperty("gemini.model", "gemini-1.5-flash");

        // Debug
        this.debugMode = Boolean.parseBoolean(props.getProperty("app.debug", "false"));

        // HTTP Settings
        try {
            this.httpTimeout = Integer.parseInt(props.getProperty("http.timeout", "30"));
        } catch (NumberFormatException e) {
            this.httpTimeout = 30;
        }

        try {
            this.maxRetries = Integer.parseInt(props.getProperty("http.max.retries", "1"));
        } catch (NumberFormatException e) {
            this.maxRetries = 1;
        }

        // --- GESTION DU PROMPT (Correction importante) ---
        String rawPrompt = props.getProperty("gemini.prompt", "Résume ce ticket Jira de manière factuelle.");
        // Le fichier .properties lit les \n comme des caractères littéraux si échappés.
        // On remplace les "\\n" explicites par de vrais sauts de ligne si nécessaire.
        this.aiPrompt = rawPrompt.replace("\\n", "\n");
    }

    // Getters
    public String getJiraBaseUrl() { return jiraBaseUrl; }
    public String getApiToken() { return apiToken; }
    public String getTargetProject() { return targetProject; }
    public String[] getTargetLabels() { return targetLabels; }
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