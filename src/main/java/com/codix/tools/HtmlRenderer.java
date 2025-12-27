package com.codix.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class HtmlRenderer {

    private static final String COL_BLUE = "#1197D6";
    private static final String COL_RED_LOCAM = "#E30613"; // Rouge officiel LOCAM
    private static final String COL_GREEN = "#27ae60";
    private static final String COL_OUTLOOK = "#0078d4";

    public void generate(Map<String, Map<String, Integer>> domainStats, 
                         Map<String, Map<String, Integer>> functionalStats,
                         Map<String, Map<String, Integer>> themeStats,
                         Map<String, Map<String, Double>> avgTimeStats,
                         Map<String, Double> globalDelays,
                         JiraService.HistoryData history, 
                         JiraService.CategoryHistoryData categoryHistory, 
                         String filename) {
        
        StringBuilder html = new StringBuilder();
        String dateGeneration = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String dateFile = new SimpleDateFormat("yyyyMMdd").format(new Date());

        // Conversion des logos en Base64 pour garantir l'affichage et la capture image
        String logoCodixB64 = getBase64Image("https://www.codix.eu/img/app/codix-business-and-finance-software-solution-company-logo.png");
        String logoLocamB64 = getBase64Image("https://www.locam.fr/wp-content/webp-express/webp-images/doc-root/wp-content/uploads/2024/09/GROUPE-LOCAM-VECTORISE-SEUL.png.webp");

        // Calculs KPI
        int totalLocam = 0;
        int totalCodix = 0;
        for (Map<String, Integer> stats : domainStats.values()) {
            totalLocam += stats.get("LOCAM");
            totalCodix += stats.get("Codix");
        }
        int totalStock = totalLocam + totalCodix;
        int pctLocam = totalStock > 0 ? (int) Math.round(((double) totalLocam / totalStock) * 100) : 0;
        int pctCodix = totalStock > 0 ? 100 - pctLocam : 0;

        html.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0'></script>");
        html.append("<script src='https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js'></script>");
        html.append("<script src='https://cdn.jsdelivr.net/npm/pptxgenjs@3.12.0/dist/pptxgen.bundle.js'></script>");
        html.append("<link href='https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;600;700&display=swap' rel='stylesheet'>");

        html.append("<style>");
        html.append("body { font-family: 'Open Sans', sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }");
        html.append(".dashboard-container { max-width: 1600px; margin: 0 auto; }");
        html.append(".header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }");
        html.append("h1 { color: ").append(COL_BLUE).append("; border-left: 6px solid ").append(COL_RED_LOCAM).append("; padding-left: 15px; margin: 0; font-size: 24px; font-weight: 700; }");
        html.append(".meta { text-align:right; color:#888; font-size:12px; margin-bottom: 25px; }");
        html.append(".btn-group { display: flex; gap: 10px; }");
        html.append(".action-btn { color: white; border: none; padding: 10px 20px; border-radius: 5px; font-weight: bold; cursor: pointer; display: flex; align-items: center; gap: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); transition: background 0.3s; font-size: 13px; }");
        html.append(".ppt-btn { background-color: ").append(COL_RED_LOCAM).append("; }");
        html.append(".copy-img-btn { background-color: ").append(COL_OUTLOOK).append("; }");
        
        html.append(".grid-row { display: flex; gap: 20px; margin-bottom: 20px; align-items: stretch; }");
        html.append(".card { background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); padding: 20px; flex: 1; border-top: 4px solid ").append(COL_BLUE).append("; position: relative; }");
        html.append(".card-title { font-size: 13px; font-weight: 700; color: #666; margin-bottom: 15px; text-transform: uppercase; letter-spacing: 0.5px; }");
        
        html.append(".kpi-widget { display: flex; justify-content: space-around; align-items: center; height: 100%; text-align: center; }");
        html.append(".kpi-value { font-size: 32px; font-weight: 800; display: block; }");
        html.append(".kpi-locam { color: ").append(COL_RED_LOCAM).append("; }");
        html.append(".kpi-codix { color: ").append(COL_BLUE).append("; }");
        
        // --- NOUVEAU STYLE DES TABLEAUX (Liserés Entêtes et Totaux seulement) ---
        html.append("table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 25px; background: white; }");
        html.append("th { background-color: ").append(COL_BLUE).append("; color: white; padding: 10px; text-align: center; border-bottom: 2px solid #000; }");
        html.append("td { padding: 8px; border: none; text-align: center; color: #444; }");
        html.append(".row-header { text-align: left; font-weight: 700; color: ").append(COL_BLUE).append("; background-color: #fcfcfc; padding-left: 15px; width: 180px; }");
        
        // Style pour démarquer la ligne de total par un liseré noir au-dessus
        html.append(".total-row td, .grand-total-cell { border-top: 2px solid #000 !important; font-weight: 800; background-color: #f8f9fa !important; color: #2c3e50; }");
        
        html.append(".table-section-title { font-size: 16px; color: ").append(COL_BLUE).append("; margin: 30px 0 15px 0; border-bottom: 2px solid #eee; padding-bottom: 8px; text-transform: uppercase; font-weight: 700; }");
        html.append(".copy-btn { float: right; background-color: #f8f9fa; border: 1px solid #ddd; color: #555; padding: 4px 10px; font-size: 11px; border-radius: 4px; cursor: pointer; margin-bottom: 5px; }");
        html.append("</style></head><body>");

        html.append("<div id='dashboard' class='dashboard-container'>");

        html.append("<div class='header-row'>");
        html.append("<h1>EXECUTIVE DASHBOARD - LOCAM / CODIX</h1>");
        html.append("<div class='btn-group'>");
        html.append("<button class='action-btn copy-img-btn' onclick='copyDashboardAsImage()'>📋 Copier l'image</button>");
        html.append("<button class='action-btn ppt-btn' onclick='exportToPPT()'>📥 Exporter en PPT</button>");
        html.append("</div>");
        html.append("</div>");
        html.append("<div class='meta'>Mise à jour : ").append(dateGeneration).append("</div>");

        // Zone de capture image
        html.append("<div id='capture-area' style='background-color: #f4f7f6; padding: 15px; border-radius: 8px;'>");
        html.append("<div class='grid-row'>");
        
        // Encart Répartition Stock
        html.append("<div class='card' style='flex: 0 0 380px; display: flex; flex-direction: column;'>");
        html.append("<div class='card-title'>Répartition Stock</div>");
        html.append("<div class='kpi-widget' style='gap: 20px;'>");
        html.append("<div style='flex: 1;'>");
        html.append("<img src='").append(logoLocamB64).append("' style='height:35px; margin-bottom:10px;'><br>");
        html.append("<span class='kpi-value kpi-locam'>").append(pctLocam).append("%</span>");
        html.append("<div style='font-size: 11px; color: #888; margin-top: 5px;'>Age median : <b>").append(String.format("%.1f j", globalDelays.getOrDefault("LOCAM", 0.0))).append("</b></div>");
        html.append("</div>");
        html.append("<div style='width: 1px; height: 80px; background-color: #eee;'></div>");
        html.append("<div style='flex: 1;'>");
        html.append("<img src='").append(logoCodixB64).append("' style='height:25px; margin-bottom:15px;'><br>");
        html.append("<span class='kpi-value kpi-codix'>").append(pctCodix).append("%</span>");
        html.append("<div style='font-size: 11px; color: #888; margin-top: 5px;'>Age median : <b>").append(String.format("%.1f j", globalDelays.getOrDefault("Codix", 0.0))).append("</b></div>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='card'>");
        html.append("<div class='card-title'>Évolution Hebdomadaire (8 semaines)</div>");
        html.append("<div style='height: 250px;'><canvas id='chartEvolution'></canvas></div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='grid-row'>");
        html.append("<div class='card' style='flex: 1;'>");
        html.append("<div class='card-title'>Répartition Actuelle (Catégories)</div>");
        html.append("<div style='height: 300px;'><canvas id='chartCategory'></canvas></div>");
        html.append("</div>");
        html.append("<div class='card' style='flex: 2;'>");
        html.append("<div class='card-title'>Stock par Domaine (Responsabilité)</div>");
        html.append("<div style='height: 300px;'><canvas id='chartDomain'></canvas></div>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Section Details
        html.append("<div id='details-zone' style='background: white; padding: 25px; border-radius: 8px; margin-top: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.05);'>");
        html.append("<div class='table-section-title' style='margin-top:0;'>Données Détaillées d'Exploitation</div>");

        appendDomainTable(html, domainStats);
        appendThemeTable(html, themeStats);
        appendFunctionalDomainTableVertical(html, functionalStats);
        appendAverageTimeTable(html, avgTimeStats);
        appendHistoryTable(html, history);
        appendCategoryTable(html, categoryHistory);
        html.append("</div></div>");

        appendScripts(html, domainStats, history, categoryHistory, dateFile);
        html.append("</body></html>");

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(html.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getBase64Image(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                String mimeType = imageUrl.contains(".svg") ? "image/svg+xml" : "image/png";
                return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) { return imageUrl; }
    }

    private String cell(int val, int max) {
        if (val == 0 || max == 0) return "<td>0</td>";
        double ratio = (double) val / max;
        int r = (int) (255 - (255 - 17) * ratio), g = (int) (255 - (255 - 151) * ratio), b = (int) (255 - (255 - 214) * ratio);
        return "<td style='background-color:rgb("+r+","+g+","+b+"); font-weight:"+(ratio > 0.5 ? "700" : "400")+"'>" + val + "</td>";
    }

    private void appendDomainTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        List<String> visibleKeys = new ArrayList<>();
        for (String d : stats.keySet()) { if (!"AUTRES".equals(d) || (stats.get(d).get("LOCAM") + stats.get(d).get("Codix")) > 0) visibleKeys.add(d); }
        html.append("<div class='table-section-title'>Répartition par Domaine principal</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-domain', this)\">Copier Image</button>");
        html.append("<table id='tab-domain'><thead><tr><th>Acteur</th>");
        for (String d : visibleKeys) html.append("<th>").append(d).append("</th>");
        html.append("<th>TOTAL</th></tr></thead><tbody>");
        int gtLocam = 0, gtCodix = 0, maxVal = 0;
        for (String d : visibleKeys) { int l = stats.get(d).get("LOCAM"); int c = stats.get(d).get("Codix"); gtLocam += l; gtCodix += c; maxVal = Math.max(maxVal, Math.max(l, c)); }
        html.append("<tr><td class='row-header'>LOCAM</td>");
        for (String d : visibleKeys) html.append(cell(stats.get(d).get("LOCAM"), maxVal));
        html.append("<td>").append(gtLocam).append("</td></tr>");
        html.append("<tr><td class='row-header'>CODIX</td>");
        for (String d : visibleKeys) html.append(cell(stats.get(d).get("Codix"), maxVal));
        html.append("<td>").append(gtCodix).append("</td></tr>");
        html.append("<tr class='total-row'><td class='row-header'>TOTAL</td>");
        for (String d : visibleKeys) html.append("<td>").append(stats.get(d).get("LOCAM") + stats.get(d).get("Codix")).append("</td>");
        html.append("<td>").append(gtLocam + gtCodix).append("</td></tr></tbody></table>");
    }

    private void appendThemeTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        List<String> visibleKeys = new ArrayList<>();
        for (String t : stats.keySet()) { if (!"AUTRES".equals(t) || (stats.get(t).get("LOCAM") + stats.get(t).get("Codix")) > 0) visibleKeys.add(t); }
        html.append("<div class='table-section-title'>Répartition par Thème technique</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-theme', this)\">Copier Image</button>");
        html.append("<table id='tab-theme'><thead><tr><th>Acteur</th>");
        for (String t : visibleKeys) html.append("<th>").append(t).append("</th>");
        html.append("<th>TOTAL</th></tr></thead><tbody>");
        int maxVal = 0;
        for (String t : visibleKeys) maxVal = Math.max(maxVal, Math.max(stats.get(t).get("LOCAM"), stats.get(t).get("Codix")));
        html.append("<tr><td class='row-header'>LOCAM</td>");
        for (String t : visibleKeys) html.append(cell(stats.get(t).get("LOCAM"), maxVal));
        html.append("<td>-</td></tr><tr><td class='row-header'>CODIX</td>");
        for (String t : visibleKeys) html.append(cell(stats.get(t).get("Codix"), maxVal));
        html.append("<td>-</td></tr></tbody></table>");
    }

    private void appendFunctionalDomainTableVertical(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        html.append("<div class='table-section-title'>Analyse par Domaine Fonctionnel</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-func', this)\">Copier Image</button>");
        html.append("<table id='tab-func'><thead><tr><th style='text-align:left; padding-left:15px;'>Domaine Fonctionnel</th><th>LOCAM</th><th>CODIX</th><th>TOTAL</th></tr></thead><tbody>");
        int maxVal = 0;
        for (Map<String, Integer> s : stats.values()) maxVal = Math.max(maxVal, Math.max(s.get("LOCAM"), s.get("Codix")));
        for (String d : stats.keySet()) {
            int l = stats.get(d).get("LOCAM"), c = stats.get(d).get("Codix");
            if ("AUTRES".equals(d) && (l + c) == 0) continue;
            html.append("<tr><td class='row-header'>").append(d).append("</td>").append(cell(l, maxVal)).append(cell(c, maxVal)).append("<td style='background:#f8f9fa; font-weight:700;'>").append(l + c).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendAverageTimeTable(StringBuilder html, Map<String, Map<String, Double>> stats) {
        html.append("<div class='table-section-title'>Temps d'assignation moyen (jours)</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-avg', this)\">Copier Image</button>");
        html.append("<table id='tab-avg'><thead><tr><th style='text-align:left; padding-left:15px;'>Domaine</th><th>LOCAM</th><th>CODIX</th></tr></thead><tbody>");
        for (String domain : stats.keySet()) {
            double l = stats.get(domain).get("LOCAM"), c = stats.get(domain).get("Codix");
            if ("AUTRES".equals(domain) && (l + c == 0)) continue;
            html.append("<tr><td class='row-header'>").append(domain).append("</td><td>").append(String.format("%.1f j", l)).append("</td><td>").append(String.format("%.1f j", c)).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendHistoryTable(StringBuilder html, JiraService.HistoryData history) {
        html.append("<div class='table-section-title'>Historique Hebdomadaire</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-history', this)\">Copier Image</button>");
        html.append("<table id='tab-history'><thead><tr><th>Indicateur</th>");
        for (String label : history.labels) html.append("<th>").append(label).append("</th>");
        html.append("</tr></thead><tbody>");
        for (JiraService.MetricType metric : JiraService.MetricType.values()) {
            html.append("<tr><td class='row-header'>").append(metric.label).append("</td>");
            Integer prev = null;
            for (Integer week : history.order) {
                int curr = history.stats.getOrDefault(week, new HashMap<>()).getOrDefault(metric, 0);
                html.append("<td>").append(curr).append(getTrendArrow(curr, prev, metric.higherIsBetter)).append("</td>");
                prev = curr;
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendCategoryTable(StringBuilder html, JiraService.CategoryHistoryData data) {
        html.append("<div class='table-section-title'>Historique par Catégorie</div><button class='copy-btn' onclick=\"copyTableAsImage('tab-cat', this)\">Copier Image</button>");
        html.append("<table id='tab-cat'><thead><tr><th>Catégorie</th>");
        for (String label : data.labels) html.append("<th>").append(label).append("</th>");
        html.append("</tr></thead><tbody>");
        for (String category : JiraService.CODIX_CATEGORIES) {
            html.append("<tr><td class='row-header'>").append(category).append("</td>");
            Integer prev = null;
            for (Integer week : data.order) {
                int curr = data.stats.getOrDefault(week, new HashMap<>()).getOrDefault(category, 0);
                html.append("<td>").append(curr).append(getTrendArrow(curr, prev, false)).append("</td>");
                prev = curr;
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private String getTrendArrow(int curr, Integer prev, boolean higherIsBetter) {
        if (prev == null || curr == prev) return "";
        boolean isGood = higherIsBetter ? (curr > prev) : (curr < prev);
        String arrow = curr > prev ? "&#9650;" : "&#9660;";
        String css = isGood ? (curr > prev ? "trend-up-good" : "trend-down-good") : (curr > prev ? "trend-up-bad" : "trend-down-bad");
        return " <span class='" + css + "'>" + arrow + "</span>";
    }

    private void appendScripts(StringBuilder html, Map<String, Map<String, Integer>> domainStats, JiraService.HistoryData history, JiraService.CategoryHistoryData catHistory, String dateFile) {
        html.append("<script>");
        html.append("Chart.register(ChartDataLabels);");
        
        String labelsWeek = toJsonArrayString(history.labels);
        String dataStock = getDataSeries(history, JiraService.MetricType.STOCK);
        String dataCreated = getDataSeries(history, JiraService.MetricType.CREATED);
        String dataClosed = getDataSeries(history, JiraService.MetricType.CLOSED);
        String dataDelivered = getDataSeries(history, JiraService.MetricType.DELIVERED);

        List<String> domains = new ArrayList<>();
        List<Integer> dLocam = new ArrayList<>();
        List<Integer> dCodix = new ArrayList<>();
        domainStats.forEach((k, v) -> { if (!"AUTRES".equals(k) || (v.get("LOCAM") + v.get("Codix")) > 0) { domains.add(k); dLocam.add(v.get("LOCAM")); dCodix.add(v.get("Codix")); } });

        List<String> catLabels = new ArrayList<>();
        List<Integer> catValues = new ArrayList<>();
        if (!catHistory.order.isEmpty()) {
            int lastWeek = catHistory.order.get(catHistory.order.size() - 1);
            catHistory.stats.getOrDefault(lastWeek, new HashMap<>()).forEach((k, v) -> { if (v > 0) { catLabels.add(k); catValues.add(v); } });
        }

        // Graphiques et capture corrigés
        html.append("new Chart(document.getElementById('chartEvolution'), { type: 'bar', data: { labels: ").append(labelsWeek).append(", datasets: [")
            .append("{ type: 'line', label: 'Créés', data: ").append(dataCreated).append(", borderColor: '").append(COL_RED_LOCAM).append("', borderWidth: 2, tension: 0.3, datalabels: {display: false} },")
            .append("{ type: 'line', label: 'Clos', data: ").append(dataClosed).append(", borderColor: '").append(COL_GREEN).append("', borderWidth: 2, tension: 0.3, datalabels: {display: false} },")
            .append("{ type: 'line', label: 'Livrés', data: ").append(dataDelivered).append(", borderColor: '#f1c40f', borderWidth: 2, tension: 0.3, borderDash: [5, 5], datalabels: {display: false} },")
            .append("{ type: 'bar', label: 'Stock', data: ").append(dataStock).append(", backgroundColor: 'rgba(17, 151, 214, 0.15)', borderColor: '").append(COL_BLUE).append("', borderWidth: 1, datalabels: {display: false} }")
            .append("] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } });");

        html.append("new Chart(document.getElementById('chartCategory'), { type: 'doughnut', data: { labels: ").append(toJsonArrayString(catLabels)).append(", datasets: [{ data: ").append(dataValues(catValues)).append(", backgroundColor: ['#1197D6', '#CC325A', '#2ecc71', '#f1c40f', '#9b59b6', '#34495e', '#e67e22', '#95a5a6'], borderWidth: 1 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right', labels: { boxWidth: 10 } }, datalabels: { color: '#fff', font: { weight: 'bold' }, formatter: Math.round, display: 'auto' } } } });");

        html.append("new Chart(document.getElementById('chartDomain'), { type: 'bar', data: { labels: ").append(toJsonArrayString(domains)).append(", datasets: [{ label: 'LOCAM', data: ").append(dataValues(dLocam)).append(", backgroundColor: '").append(COL_RED_LOCAM).append("' }, { label: 'CODIX', data: ").append(dataValues(dCodix)).append(", backgroundColor: '").append(COL_BLUE).append("' }] }, options: { responsive: true, maintainAspectRatio: false, scales: { x: { stacked: true }, y: { stacked: true } }, plugins: { legend: { position: 'bottom' }, datalabels: { display: false } } } });");

        // Nouvelle fonction de copie image pour les tableaux
        html.append("async function copyTableAsImage(tableId, btn) { ")
            .append("const table = document.getElementById(tableId); ")
            .append("const canvas = await html2canvas(table, { scale: 2, backgroundColor: '#ffffff' }); ")
            .append("canvas.toBlob(async (blob) => { ")
            .append("const data = [new ClipboardItem({ 'image/png': blob })]; ")
            .append("await navigator.clipboard.write(data); ")
            .append("const oldText = btn.innerText; btn.innerText = 'Copié !'; ")
            .append("setTimeout(() => btn.innerText = oldText, 2000); ")
            .append("}); }");
        
        html.append("function copyDashboardAsImage() { const btn = document.querySelector('.copy-img-btn'); const originalText = btn.innerText; btn.innerText = 'Capture...'; html2canvas(document.getElementById('capture-area'), { scale: 2, useCORS: true, allowTaint: true }).then(canvas => { canvas.toBlob(blob => { const item = new ClipboardItem({ 'image/png': blob }); navigator.clipboard.write([item]).then(() => { btn.innerText = 'Copié !'; btn.style.backgroundColor = '#27ae60'; setTimeout(() => { btn.innerText = originalText; btn.style.backgroundColor = '").append(COL_OUTLOOK).append("'; }, 2000); }); }); }); }");

        html.append("function exportToPPT() { var btn = document.querySelector('.ppt-btn'); btn.innerText = 'Génération...'; html2canvas(document.getElementById('capture-area'), { scale: 2, useCORS: true }).then(canvas => { var img = canvas.toDataURL('image/png'); let pptx = new PptxGenJS(); let slide = pptx.addSlide(); slide.addImage({ data: img, x: 0, y: 0, w: '100%', h: '100%', sizing: { type: 'contain' } }); pptx.writeFile({ fileName: 'Dashboard_LOCAM_").append(dateFile).append(".pptx' }); btn.innerText = '📥 Exporter en PPT'; }); }");
        
        html.append("</script>");
    }

    private String toJsonArrayString(List<String> list) {
        if (list.isEmpty()) return "[]";
        return "[" + list.stream().map(s -> "'" + s.replace("'", "\\'") + "'").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    private String dataValues(List<? extends Number> list) {
        if (list.isEmpty()) return "[]";
        return "[" + list.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    private String getDataSeries(JiraService.HistoryData history, JiraService.MetricType metric) {
        List<Integer> values = new ArrayList<>();
        for (Integer week : history.order) values.add(history.stats.getOrDefault(week, new HashMap<>()).getOrDefault(metric, 0));
        return dataValues(values);
    }
}