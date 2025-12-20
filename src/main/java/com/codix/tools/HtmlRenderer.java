package com.codix.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HtmlRenderer {

    private static final String COL_BLUE = "#1197D6";
    private static final String COL_RED = "#CC325A";
    private static final String COL_GREEN = "#2ecc71"; 

    public void generate(Map<String, Map<String, Integer>> domainStats, 
                     Map<String, Map<String, Integer>> functionalStats,
                     Map<String, Map<String, Integer>> themeStats, // Nouveau paramètre
                     JiraService.HistoryData history, 
                     JiraService.CategoryHistoryData categoryHistory, 
                     String filename) {
        StringBuilder html = new StringBuilder();
        String dateGeneration = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String dateFile = new SimpleDateFormat("yyyyMMdd").format(new Date());

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
        html.append("body { font-family: 'Open Sans', 'Segoe UI', Helvetica, Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }");
        html.append(".dashboard-container { max-width: 1600px; margin: 0 auto; background-color: #f4f7f6; padding: 10px; }"); 
        html.append("#capture-zone { background-color: #f4f7f6; padding: 10px; }");
        
        html.append(".header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 2px solid #ddd; padding-bottom: 10px; }");
        html.append("h1 { color: ").append(COL_BLUE).append("; margin: 0; font-size: 24px; font-weight: 700; }");
        html.append(".meta { font-size: 14px; color: #888; }");

        html.append(".ppt-btn { background-color: #E30613; color: white; border: none; padding: 10px 20px; border-radius: 5px; font-weight: bold; cursor: pointer; display: flex; align-items: center; gap: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.2); transition: background 0.3s; }");
        html.append(".ppt-btn:hover { background-color: #b9050f; }");

        html.append(".grid-row { display: flex; gap: 20px; margin-bottom: 20px; }");
        html.append(".card { background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); padding: 20px; flex: 1; border-top: 4px solid ").append(COL_BLUE).append("; position: relative; }");
        html.append(".card-title { font-size: 16px; font-weight: 600; color: #555; margin-bottom: 15px; text-transform: uppercase; letter-spacing: 0.5px; }");
        
        html.append(".kpi-widget { display: flex; justify-content: space-around; align-items: center; height: 100%; text-align: center; }");
        html.append(".kpi-item { flex: 1; }");
        html.append(".kpi-value { font-size: 32px; font-weight: 800; display: block; }");
        html.append(".kpi-label { font-size: 12px; color: #777; text-transform: uppercase; margin-top: 5px; }");
        html.append(".kpi-locam { color: #e74c3c; }"); 
        html.append(".kpi-codix { color: ").append(COL_BLUE).append("; }");
        
        html.append("table { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 13px; margin-bottom: 5px; }");
        html.append("th { background-color: ").append(COL_BLUE).append("; color: white; padding: 8px; text-align: center; border-bottom: 3px solid #0d7ab0; }");
        html.append("td { padding: 8px; border-bottom: 1px solid #eee; text-align: center; color: #444; }");
        html.append(".row-header { text-align: left; font-weight: 700; color: ").append(COL_BLUE).append("; background-color: #fcfcfc; border-right: 1px solid #eee; width: 140px; }");
        html.append(".grand-total { background-color: #095c85 !important; color: white !important; font-weight: bold; }");
        
        html.append(".trend-up-bad { color: ").append(COL_RED).append("; } .trend-down-good { color: ").append(COL_GREEN).append("; }");
        html.append(".trend-up-good { color: ").append(COL_GREEN).append("; } .trend-down-bad { color: ").append(COL_RED).append("; }");
        html.append(".trend-neutral { color: #ccc; }");

        html.append(".table-wrapper { position: relative; margin-top: 10px; }");
        html.append(".copy-btn { float: right; background-color: #f8f9fa; border: 1px solid #ddd; color: #555; padding: 4px 8px; font-size: 11px; border-radius: 4px; cursor: pointer; }");
        html.append(".copy-btn:hover { background-color: #e2e6ea; }");

        html.append("</style></head><body>");
        html.append("<div id='dashboard' class='dashboard-container'>");
        
        // --- ZONE DE CAPTURE ---
        html.append("<div id='capture-zone'>");
        html.append("<div class='header'>");
        html.append("<div>");
        html.append("<h1>EXECUTIVE DASHBOARD - LOCAM / CODIX</h1>");
        html.append("<div class='meta'>Mise à jour : ").append(dateGeneration).append("</div>");
        html.append("</div>");
        html.append("<button class='ppt-btn' onclick='exportToPPT()' data-html2canvas-ignore='true'>📥 Exporter en PPT</button>");
        html.append("</div>");

        html.append("<div class='grid-row'>");
        html.append("<div class='card' style='flex: 0 0 300px;'>"); 
        html.append("<div class='card-title'>Répartition Stock</div>");
        html.append("<div class='kpi-widget'>");
        html.append("<div class='kpi-item'><span class='kpi-value kpi-locam'>").append(pctLocam).append("%</span><span class='kpi-label'>LOCAM</span></div>");
        html.append("<div class='kpi-item' style='font-size:20px; color:#ddd;'>/</div>");
        html.append("<div class='kpi-item'><span class='kpi-value kpi-codix'>").append(pctCodix).append("%</span><span class='kpi-label'>CODIX</span></div>");
        html.append("</div>");
        html.append("</div>");
        html.append("<div class='card' style='flex: 1;'>");
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
        html.append("</div>"); // FIN CAPTURE ZONE

        // --- DETAILS ZONE ---
        html.append("<div id='details-zone'>");
        html.append("<div class='card' style='margin-top: 20px;'>");
        html.append("<div class='card-title'>Données Détaillées (Non incluses dans l'export PPT)</div>");
        
        // 1. Domaine Classique (Horizontal)
        appendDomainTable(html, domainStats);
        html.append("<hr style='border: 0; border-top: 1px solid #eee; margin: 30px 0;'>");
        appendThemeTable(html, themeStats); // Ajout du nouveau tableau
        // 2. Domaine Fonctionnel (VERTICAL - Transposé pour l'harmonie)
        appendFunctionalDomainTableVertical(html, functionalStats);
        html.append("<hr style='border: 0; border-top: 1px solid #eee; margin: 30px 0;'>");

        appendHistoryTable(html, history);
        html.append("<hr style='border: 0; border-top: 1px solid #eee; margin: 30px 0;'>");
        appendCategoryTable(html, categoryHistory);
        
        html.append("</div>");
        html.append("</div>");

        html.append("</div>"); 

        appendScripts(html, domainStats, history, categoryHistory, dateFile);
        html.append("</body></html>");

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(html.toString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void appendScripts(StringBuilder html, 
                               Map<String, Map<String, Integer>> domainStats, 
                               JiraService.HistoryData history, 
                               JiraService.CategoryHistoryData catHistory,
                               String dateFile) {
        html.append("<script>");
        html.append("Chart.register(ChartDataLabels);");
        String labelsWeek = toJsonArrayString(history.labels);
        String dataStock = getDataSeries(history, JiraService.MetricType.STOCK);
        String dataCreated = getDataSeries(history, JiraService.MetricType.CREATED);
        String dataClosed = getDataSeries(history, JiraService.MetricType.CLOSED);
        String dataDelivered = getDataSeries(history, JiraService.MetricType.DELIVERED);
        List<String> domains = new ArrayList<>(domainStats.keySet());
        String labelsDomain = toJsonArrayString(domains);
        List<Integer> dataLocam = new ArrayList<>();
        List<Integer> dataCodix = new ArrayList<>();
        for(String d : domains) {
            dataLocam.add(domainStats.get(d).get("LOCAM"));
            dataCodix.add(domainStats.get(d).get("Codix"));
        }
        Integer lastWeek = history.order.get(history.order.size() - 1);
        List<String> catLabels = new ArrayList<>();
        List<Integer> catValues = new ArrayList<>();
        if(catHistory.stats.containsKey(lastWeek)) {
            for(Map.Entry<String, Integer> entry : catHistory.stats.get(lastWeek).entrySet()) {
                if(entry.getValue() > 0) {
                    catLabels.add(entry.getKey());
                    catValues.add(entry.getValue());
                }
            }
        }
        html.append("new Chart(document.getElementById('chartEvolution'), { type: 'bar', data: { labels: ").append(labelsWeek).append(", datasets: [ { type: 'line', label: 'Créés', data: ").append(dataCreated).append(", borderColor: '").append(COL_RED).append("', borderWidth: 2, tension: 0.3, datalabels: {display: false} }, { type: 'line', label: 'Clos', data: ").append(dataClosed).append(", borderColor: '").append(COL_GREEN).append("', borderWidth: 2, tension: 0.3, datalabels: {display: false} }, { type: 'line', label: 'Livrés', data: ").append(dataDelivered).append(", borderColor: '#f1c40f', borderWidth: 2, tension: 0.3, borderDash: [5, 5], datalabels: {display: false} }, { type: 'bar', label: 'Stock', data: ").append(dataStock).append(", backgroundColor: 'rgba(17, 151, 214, 0.2)', borderColor: '").append(COL_BLUE).append("', borderWidth: 1, datalabels: {display: false} } ] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' }, datalabels: { display: false } } } });");
        html.append("new Chart(document.getElementById('chartCategory'), { type: 'doughnut', data: { labels: ").append(toJsonArrayString(catLabels)).append(", datasets: [{ data: ").append(dataValues(catValues)).append(", backgroundColor: ['#1197D6', '#CC325A', '#2ecc71', '#f1c40f', '#9b59b6', '#34495e', '#e67e22', '#95a5a6'], borderWidth: 1, borderColor: '#fff' }] }, options: { responsive: true, maintainAspectRatio: false, layout: { padding: 20 }, plugins: { legend: { display: true, position: 'right', labels: { boxWidth: 10, font: {size: 11} } }, datalabels: { color: '#fff', font: { weight: 'bold', size: 12 }, formatter: (value) => value, display: 'auto' } } } });");
        html.append("new Chart(document.getElementById('chartDomain'), { type: 'bar', data: { labels: ").append(labelsDomain).append(", datasets: [ { label: 'LOCAM', data: ").append(dataValues(dataLocam)).append(", backgroundColor: '#e74c3c' }, { label: 'CODIX', data: ").append(dataValues(dataCodix)).append(", backgroundColor: '").append(COL_BLUE).append("' } ] }, options: { responsive: true, maintainAspectRatio: false, scales: { x: { stacked: true }, y: { stacked: true } }, plugins: { legend: { position: 'bottom' }, datalabels: { display: false } } } });");
        html.append("function copyTable(tableId, btnId) { var range = document.createRange(); range.selectNode(document.getElementById(tableId)); window.getSelection().removeAllRanges(); window.getSelection().addRange(range); document.execCommand('copy'); window.getSelection().removeAllRanges(); var btn = document.getElementById(btnId); var originalText = btn.innerText; btn.innerText = 'Copié !'; setTimeout(function() { btn.innerText = originalText; }, 2000); }");
        html.append("function exportToPPT() { var btn = document.querySelector('.ppt-btn'); btn.innerText = 'Génération en cours...'; btn.disabled = true; html2canvas(document.getElementById('capture-zone'), { scale: 2, useCORS: true }).then(canvas => { var imgData = canvas.toDataURL('image/png'); let pptx = new PptxGenJS(); pptx.layout = 'LAYOUT_16x9'; let slide = pptx.addSlide(); slide.addImage({ data: imgData, x: 0, y: 0, w: '100%', h: '100%', sizing: { type: 'contain', w: '100%', h: '100%' } }); pptx.writeFile({ fileName: 'Dashboard_Codix_").append(dateFile).append(".pptx' }); btn.innerText = '📥 Exporter en PPT'; btn.disabled = false; }); }");
        html.append("</script>");
    }

    // --- Helpers (Inchangés) ---
    private String toJsonArrayString(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) { sb.append("'").append(list.get(i)).append("'"); if (i < list.size() - 1) sb.append(","); }
        sb.append("]"); return sb.toString();
    }
    private String dataValues(List<Integer> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) { sb.append(list.get(i)); if (i < list.size() - 1) sb.append(","); }
        sb.append("]"); return sb.toString();
    }
    private String getDataSeries(JiraService.HistoryData history, JiraService.MetricType metric) {
        List<Integer> values = new ArrayList<>();
        for (Integer week : history.order) { values.add(history.stats.get(week).get(metric)); }
        return dataValues(values);
    }

    private void appendDomainTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        html.append("<div class='table-wrapper'><h3>Répartition par Domaine</h3>");
        html.append("<table id='tab-domain'><thead><tr><th></th>");
        for (String d : stats.keySet()) html.append("<th>").append(d).append("</th>");
        html.append("<th>TOTAL</th></tr></thead><tbody>");
        
        int gtLocam=0, gtCodix=0, maxVal=0;
        Map<String, Integer> colTotals = new HashMap<>();
        for (String d : stats.keySet()) {
            int l=stats.get(d).get("LOCAM"), c=stats.get(d).get("Codix");
            gtLocam+=l; gtCodix+=c; colTotals.merge(d, l+c, Integer::sum);
            maxVal = Math.max(maxVal, Math.max(l,c));
        }
        html.append("<tr><td class='row-header'>LOCAM</td>");
        for (String d : stats.keySet()) html.append(cell(stats.get(d).get("LOCAM"), maxVal));
        html.append("<td class='grand-total'>").append(gtLocam).append("</td></tr>");
        html.append("<tr><td class='row-header'>Codix</td>");
        for (String d : stats.keySet()) html.append(cell(stats.get(d).get("Codix"), maxVal));
        html.append("<td class='grand-total'>").append(gtCodix).append("</td></tr>");
        html.append("<tr class='total-row'><td class='row-header'>TOTAL</td>");
        int finalTotal = 0;
        int maxCol = Collections.max(colTotals.values());
        for(String d : stats.keySet()){ int val = colTotals.get(d); finalTotal+=val; html.append(cell(val, maxCol)); }
        html.append("<td class='grand-total'>").append(finalTotal).append("</td></tr>");
        html.append("</tbody></table>");
        html.append("<button id='btn-domain' class='copy-btn' onclick=\"copyTable('tab-domain', 'btn-domain')\">Copier</button></div>");
    }

    // --- NOUVELLE METHODE VERTICALE POUR DOMAINE FONCTIONNEL ---
    // Cette méthode transpose le tableau : Lignes = Domaines, Colonnes = Acteurs
    private void appendFunctionalDomainTableVertical(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        String tableId = "tab-func";
        String btnId = "btn-func";
        
        html.append("<div class='table-wrapper'><h3>Répartition par Domaine Fonctionnel</h3>");
        html.append("<table id='").append(tableId).append("'><thead><tr>");
        html.append("<th style='width: 40%; text-align:left; padding-left:15px;'>Domaine Fonctionnel</th>");
        html.append("<th>LOCAM</th><th>CODIX</th><th>TOTAL</th>");
        html.append("</tr></thead><tbody>");

        // Calcul du MAX global pour la Heatmap
        int maxVal = 0;
        int sumLocamTotal = 0;
        int sumCodixTotal = 0;
        
        for (String d : stats.keySet()) {
            int l = stats.get(d).get("LOCAM");
            int c = stats.get(d).get("Codix");
            sumLocamTotal += l;
            sumCodixTotal += c;
            maxVal = Math.max(maxVal, Math.max(l, c));
        }

        // Itération sur les lignes (Domaines)
        for (String d : stats.keySet()) {
            int l = stats.get(d).get("LOCAM");
            int c = stats.get(d).get("Codix");
            int total = l + c;
            
            html.append("<tr>");
            // Row Header à gauche, aligné à gauche pour la lisibilité
            html.append("<td class='row-header' style='width: 40%; text-align:left; padding-left:15px;'>").append(d).append("</td>");
            
            // Valeurs avec Heatmap
            html.append(cell(l, maxVal));
            html.append(cell(c, maxVal));
            
            // Total ligne (Grisé ou gras léger)
            html.append("<td style='background-color:#eef2f5; font-weight:bold;'>").append(total).append("</td>");
            html.append("</tr>");
        }
        
        // Ligne de Totaux Globaux
        html.append("<tr class='total-row'>");
        html.append("<td class='row-header' style='text-align:right; padding-right:15px;'>TOTAL</td>");
        html.append("<td class='grand-total'>").append(sumLocamTotal).append("</td>");
        html.append("<td class='grand-total'>").append(sumCodixTotal).append("</td>");
        html.append("<td class='grand-total'>").append(sumLocamTotal + sumCodixTotal).append("</td>");
        html.append("</tr>");

        html.append("</tbody></table>");
        html.append("<button id='").append(btnId).append("' class='copy-btn' onclick=\"copyTable('").append(tableId).append("', '").append(btnId).append("')\">Copier</button></div>");
    }

    private void appendHistoryTable(StringBuilder html, JiraService.HistoryData history) {
        html.append("<div class='table-wrapper'><h3>Historique Hebdo</h3>");
        html.append("<table id='tab-history'><thead><tr><th>Indicateur</th>");
        for (String label : history.labels) html.append("<th>").append(label).append("</th>");
        html.append("</tr></thead><tbody>");
        for (JiraService.MetricType metric : JiraService.MetricType.values()) {
            html.append("<tr><td class='row-header'>").append(metric.label).append("</td>");
            Integer prev = null;
            for (Integer week : history.order) {
                Map<JiraService.MetricType, Integer> weekData = history.stats.get(week);
                // On vérifie si la semaine existe ET si la métrique existe, sinon on met 0
                int curr = (weekData != null) ? weekData.getOrDefault(metric, 0) : 0;
                
                html.append("<td>").append(curr).append(getTrendArrow(curr, prev, metric.higherIsBetter)).append("</td>");
                prev = curr;
            }            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<button id='btn-history' class='copy-btn' onclick=\"copyTable('tab-history', 'btn-history')\">Copier</button></div>");
    }

    private void appendCategoryTable(StringBuilder html, JiraService.CategoryHistoryData data) {
        html.append("<div class='table-wrapper'><h3>Historique Catégories</h3>");
        html.append("<table id='tab-cat'><thead><tr><th>Catégorie</th>");
        for (String label : data.labels) html.append("<th>").append(label).append("</th>");
        html.append("</tr></thead><tbody>");
        for (String category : JiraService.CODIX_CATEGORIES) {
            html.append("<tr><td class='row-header'>").append(category).append("</td>");
            Integer prev = null;
            for (Integer week : data.order) {
                Map<String, Integer> weekData = data.stats.get(week);
                int curr = weekData != null ? weekData.getOrDefault(category, 0) : 0;
                html.append("<td>").append(curr).append(getTrendArrow(curr, prev, false)).append("</td>");
                prev = curr;
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<button id='btn-cat' class='copy-btn' onclick=\"copyTable('tab-cat', 'btn-cat')\">Copier</button></div>");
    }

    private String getTrendArrow(int curr, Integer prev, boolean higherIsBetter) {
        if (prev == null) return "";
        if (curr > prev) return "<span class='" + (higherIsBetter ? "trend-up-good" : "trend-up-bad") + "'>&#9650;</span>";
        if (curr < prev) return "<span class='" + (higherIsBetter ? "trend-down-bad" : "trend-down-good") + "'>&#9660;</span>";
        return "<span class='trend-neutral'>&#8212;</span>";
    }

    private String cell(int val, int max) {
        return "<td style='background-color:" + getCodixHeatmapColor(val, max) + "'>" + val + "</td>";
    }

    private String getCodixHeatmapColor(int val, int max) {
        if (val == 0 || max == 0) return "#ffffff";
        double ratio = (double) val / max;
        int r = (int) (255 + (204 - 255) * ratio);
        int g = (int) (255 + (50 - 255) * ratio);
        int b = (int) (255 + (90 - 255) * ratio);
        return String.format("#%02x%02x%02x", r, g, b);
    }
    
    private void appendThemeTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
    html.append("<div class='table-wrapper'><h3>Répartition par Thème</h3>");
    html.append("<table id='tab-theme'><thead><tr><th></th>");
    for (String t : stats.keySet()) html.append("<th>").append(t).append("</th>");
    html.append("<th>TOTAL</th></tr></thead><tbody>");
    
    int gtLocam=0, gtCodix=0, maxVal=0;
    Map<String, Integer> colTotals = new HashMap<>();
    for (String t : stats.keySet()) {
        int l = stats.get(t).get("LOCAM"), c = stats.get(t).get("Codix");
        gtLocam += l; gtCodix += c; colTotals.merge(t, l + c, Integer::sum);
        maxVal = Math.max(maxVal, Math.max(l, c));
    }

    html.append("<tr><td class='row-header'>LOCAM</td>");
    for (String t : stats.keySet()) html.append(cell(stats.get(t).get("LOCAM"), maxVal));
    html.append("<td class='grand-total'>").append(gtLocam).append("</td></tr>");

    html.append("<tr><td class='row-header'>Codix</td>");
    for (String t : stats.keySet()) html.append(cell(stats.get(t).get("Codix"), maxVal));
    html.append("<td class='grand-total'>").append(gtCodix).append("</td></tr>");

    html.append("<tr class='total-row'><td class='row-header'>TOTAL</td>");
    int finalTotal = 0;
    int maxCol = colTotals.isEmpty() ? 0 : Collections.max(colTotals.values());
    for(String t : stats.keySet()){ 
        int val = colTotals.get(t); finalTotal += val; html.append(cell(val, maxCol)); 
    }
    html.append("<td class='grand-total'>").append(finalTotal).append("</td></tr>");
    html.append("</tbody></table>");
    html.append("<button id='btn-theme' class='copy-btn' onclick=\"copyTable('tab-theme', 'btn-theme')\">Copier</button></div>");
}
}