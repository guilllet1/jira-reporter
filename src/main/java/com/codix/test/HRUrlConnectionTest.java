package com.codix.test;

import com.codix.tools.btteamreport.ResourcePlanningService;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HRUrlConnectionTest {

    private static final String HR_URL = "https://hrcenter.codixfr.private/HR//index.php?tab=6";

    // Libellés de présence (issus de ta fonction loadHRData)
    private static final List<String> PRESENCE_LABELS = Arrays.asList(
            "Work From Home", "In office", "Workday",
            "Business trip", "Client meeting at Codix", "Assistance on remote"
    );

    public static void main(String[] args) {
        try {
            // Utilisation de "UTF-8" (String) au lieu de StandardCharsets pour la compatibilité Java 8
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // 1. Initialisation
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                props.load(fis);
            }
            String cookie = props.getProperty("hr.cookie");
            if (cookie == null && props.getProperty("hr.phpsessid") != null) {
                cookie = "PHPSESSID=" + props.getProperty("hr.phpsessid");
            }

            OkHttpClient client = getUnsafeOkHttpClient();
            Path logDir = Paths.get("logs");
            Files.createDirectories(logDir);

            // 2. Définition des 4 semaines cibles
            LocalDate today = LocalDate.now();
            List<LocalDate> nextMondays = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                nextMondays.add(today.plusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            }

            // Map<Login, Map<LundiDeLaSemaine, NombreDePresences>>
            Map<String, Map<LocalDate, Integer>> uniquePresenceDays = new HashMap<>();

            // 3. Interrogation des mois nécessaires
            Set<String> processedMonths = new HashSet<>();
            LocalDate endDate = today.plusWeeks(4);
            LocalDate cursor = today;

            while (!cursor.isAfter(endDate)) {
                String monthPrefix = cursor.format(DateTimeFormatter.ofPattern("yyyyMM"));
                if (!processedMonths.contains(monthPrefix)) {
                    System.out.print("Fetching HR " + monthPrefix + "... ");
                    String html = fetchHrPage(client, cookie, cursor.getMonthValue(), cursor.getYear());

                    if (html != null) {
                        // Sauvegarde physique pour vérification
                        Path path = logDir.resolve("hr_" + monthPrefix + ".html");
                        Files.writeString(path, html, StandardCharsets.UTF_8);
                        System.out.println("Enregistré dans " + path.toAbsolutePath());

                        // Analyse avec TA logique
                        processHtmlPresence(html, uniquePresenceDays, nextMondays);
                    } else {
                        System.out.println("Erreur (HTML null)");
                    }
                    processedMonths.add(monthPrefix);
                }
                cursor = cursor.plusMonths(1).withDayOfMonth(1);
            }

            // 4. Calcul du Delta (Présence - 5)
            System.out.println("\n=== RAPPORT DES DELTAS D'ABSENCE (Présence - 5j) ===");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            
            System.out.print(String.format("%-25s", "Utilisateur"));
            for (LocalDate monday : nextMondays) System.out.print(" | Lundi " + monday.format(fmt));
            System.out.println("\n----------------------------------------------------------------------------------");

            // On boucle sur TARGET_USERS pour avoir tout le monde
            ResourcePlanningService.TARGET_USERS.forEach((login, fullName) -> {
                System.out.print(String.format("%-25s", fullName));
                Map<LocalDate, Integer> weeks = uniquePresenceDays.getOrDefault(login, new HashMap<>());
                
                for (LocalDate monday : nextMondays) {
                    int count = weeks.getOrDefault(monday, 0);
                    int delta = count - 5;
                    String status = (delta == 0) ? "  0 " : String.format("%+2d ", delta);
                    System.out.print(" |    " + status);
                }
                System.out.println();
            });

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void processHtmlPresence(String html, Map<String, Map<LocalDate, Integer>> presenceMap, List<LocalDate> targetMondays) {
        Document doc = Jsoup.parse(html);
        // Ta logique : lignes ayant la classe user_name_td
        Elements rows = doc.select("tr:has(td.user_name_td)");

        for (Element row : rows) {
            String fullNameFromHtml = row.select("td.user_name_td").text().trim().replace('\u00A0', ' ');

            // Identification du login via TARGET_USERS
            String login = ResourcePlanningService.TARGET_USERS.entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(fullNameFromHtml) 
                            || (entry.getValue().contains("Valérie") && fullNameFromHtml.contains("Valerie")))
                    .map(Map.Entry::getKey).findFirst().orElse(null);

            if (login == null) continue;

            // Ta logique : cellules dont l'ID commence par cell_
            Elements cells = row.select("td[id^=cell_]");
            for (Element cell : cells) {
                String id = cell.id(); // cell_YYYYMMDD_...
                if (id.length() < 13) continue;

                try {
                    String datePart = id.substring(5, 13);
                    LocalDate cellDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String title = cell.attr("title").trim();

                    // Si c'est un jour de présence
                    if (PRESENCE_LABELS.stream().anyMatch(l -> l.equalsIgnoreCase(title))) {
                        // On trouve à quel lundi de nos 4 semaines cela appartient
                        for (LocalDate monday : targetMondays) {
                            LocalDate sunday = monday.plusDays(6);
                            if (!cellDate.isBefore(monday) && !cellDate.isAfter(sunday)) {
                                presenceMap.computeIfAbsent(login, k -> new HashMap<>())
                                           .merge(monday, 1, Integer::sum);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static String fetchHrPage(OkHttpClient client, String cookie, int month, int year) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("lv_depts[]", "all_users")
                .add("cm", String.format("%02d", month))
                .add("cy", String.valueOf(year))
                .add("filter_report", "Filter")
                .build();

        Request request = new Request.Builder()
                .url(HR_URL).post(formBody)
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return response.body().string();
        }
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{ new X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS).build();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}