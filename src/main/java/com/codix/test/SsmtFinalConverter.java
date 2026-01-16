package com.codix.test;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SsmtFinalConverter {

    private static final String INPUT_FOLDER = "pages_html_ssmt";
    private static final String OUTPUT_FILE = "SSMT_Documentation_Final.xlsx";
    private static final String LINK_TEMPLATE = "https://ssmt.codixfr.private/ssmt/src/index.php?getfile&docid=%s&lng=EN";

    public static void main(String[] args) {
        try {
            // Utilisation de "UTF-8" (String) au lieu de StandardCharsets pour la compatibilité Java 8
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=== DÉMARRAGE SsmtFinalConverter (Ciblage Colonnes 11, 12, 13) ===");

        File folder = new File(INPUT_FOLDER);
        if (!folder.exists()) {
            System.err.println("ERREUR : Dossier '" + INPUT_FOLDER + "' introuvable.");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".html"));
        if (files == null || files.length == 0) {
            System.err.println("ERREUR : Aucun fichier HTML trouvé.");
            return;
        }

        Arrays.sort(files, Comparator.comparingInt(SsmtFinalConverter::getPageNumber));
        System.out.println("-> " + files.length + " fichiers à traiter.");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Documentation");
            createHeader(sheet);

            int rowIndex = 1;
            int totalLines = 0;

            for (File file : files) {
                Document doc = Jsoup.parse(file, "UTF-8");
                Elements rows = doc.select("tr");

                for (Element row : rows) {
                    Elements cols = row.select("td");
                    
                    // Sécurité : on ignore les lignes trop courtes
                    if (cols.size() < 14) continue;

                    String rawName = cols.get(1).text();
                    if (shouldIgnoreRow(rawName)) continue;

                    String name = clean(rawName);

                    // --- 1. LIEN ---
                    String link = "";
                    Element linkElement = row.selectFirst("a[onclick*='manage_doc']");
                    if (linkElement != null) {
                        String docId = extractBigId(linkElement.attr("onclick"));
                        if (!docId.isEmpty()) {
                            link = String.format(LINK_TEMPLATE, docId);
                        }
                    }

                    // --- 2. CIBLAGE PRÉCIS DES COLONNES ---
                    int sz = cols.size();
                    
                    // Basé sur ton debug (Total=18) :
                    // Col 11 (Dev)   = sz - 7  (18 - 7 = 11)
                    // Col 12 (Start) = sz - 6  (18 - 6 = 12)
                    // Col 13 (Only)  = sz - 5  (18 - 5 = 13)
                    
                    String devV9 = "";
                    String startV9 = "";
                    String onlyV9 = "";

                    if (sz >= 7) {
                        devV9 = clean(cols.get(sz - 7).text());
                        startV9 = clean(cols.get(sz - 6).text());
                        onlyV9 = clean(cols.get(sz - 5).text());
                    }

                    // DEBUG LÉGER pour vérifier la première ligne "bandeau_icones"
                    if (name.contains("bandeau_icones")) {
                        System.out.println(">>> VÉRIFICATION CIBLAGE <<<");
                        System.out.println("Nom      : " + name);
                        System.out.println("Dev V9   : " + devV9 + " (Attendu: Yes)");
                        System.out.println("Start V9 : " + startV9 + " (Attendu: Vide)");
                        System.out.println("Only V9  : " + onlyV9 + " (Attendu: Vide)");
                        System.out.println("----------------------------");
                    }

                    // Écriture Excel
                    Row excelRow = sheet.createRow(rowIndex++);
                    excelRow.createCell(0).setCellValue(name);
                    excelRow.createCell(1).setCellValue(link);
                    excelRow.createCell(2).setCellValue(devV9);
                    excelRow.createCell(3).setCellValue(startV9);
                    excelRow.createCell(4).setCellValue(onlyV9);

                    totalLines++;
                }
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(OUTPUT_FILE)) {
                workbook.write(out);
            }
            System.out.println("\nSUCCÈS : " + totalLines + " lignes enregistrées dans " + OUTPUT_FILE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean shouldIgnoreRow(String name) {
        if (name == null) return true;
        String n = name.trim();
        return n.isEmpty() || n.equalsIgnoreCase("Name") || n.contains("Started in V9");
    }

    private static String clean(String text) {
        if (text == null) return "";
        return text.replace("\u00a0", " ")
                   .replaceAll("[\\t\\n\\r]+", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private static String extractBigId(String text) {
        if (text == null) return "";
        Pattern p = Pattern.compile("(\\d{3,})");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    private static int getPageNumber(File f) {
        try {
            return Integer.parseInt(f.getName().replaceAll("\\D", ""));
        } catch (Exception e) { return 0; }
    }

    private static void createHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        String[] h = {"Name", "Link", "Developed in V9", "Started in V9", "Only in V9"};
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < h.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(h[i]);
            c.setCellStyle(style);
        }
    }
}