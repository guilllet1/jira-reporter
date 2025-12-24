package com.codix.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResourcePlanningRenderer {

    private static final String COL_BLUE = "#1197D6";
    private static final String COL_RED_CODIX = "#CC325A";
    private static final String COL_GREEN = "#27ae60";
    private static final String COL_RED_KPI = "#e74c3c";
    private static final String COL_OUTLOOK = "#0078d4";

    public void generate(ResourcePlanningService.ReportData data, List<SufferingTheme> suffering, String filename) {
        StringBuilder html = new StringBuilder();
        String dateGeneration = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        html.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        html.append("<script src='https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js'></script>");
        html.append("<link href='https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;600;700&display=swap' rel='stylesheet'>");
        html.append("<style>");
        html.append("body { font-family: 'Open Sans', sans-serif; background-color: #f4f7f6; padding: 20px; color: #333; margin: 0; }");
        html.append(".container { max-width: 1800px; margin: 0 auto; background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }");
        html.append(".header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }");
        html.append("h1 { color: ").append(COL_BLUE).append("; border-left: 6px solid ").append(COL_RED_CODIX).append("; padding-left: 15px; margin: 0; font-size: 24px; }");
        html.append(".email-btn { background-color: ").append(COL_OUTLOOK).append("; color: white; border: none; padding: 10px 20px; border-radius: 5px; font-weight: bold; cursor: pointer; display: flex; align-items: center; gap: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.2); transition: background 0.3s; font-size: 14px; }");
        html.append(".email-btn:hover { background-color: #005a9e; }");
        html.append(".meta { text-align:right; color:#888; font-size:12px; margin-bottom: 25px; }");
        html.append(".dashboard { display: flex; gap: 15px; margin-bottom: 40px; justify-content: space-between; align-items: stretch; }");
        html.append(".kpi-card { flex: 1; background: #fff; border: 1px solid #e1e4e8; border-radius: 8px; padding: 15px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); text-align: center; position: relative; border-top: 4px solid ").append(COL_BLUE).append("; display: flex; flex-direction: column; justify-content: center; }");
        html.append(".kpi-title { font-size: 12px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; font-weight: 600; min-height: 30px; display: flex; align-items: center; justify-content: center; }");
        html.append(".kpi-value { font-size: 28px; font-weight: 800; color: #2c3e50; margin-bottom: 5px; }");
        html.append(".kpi-trend { font-size: 11px; display: flex; align-items: center; justify-content: center; gap: 5px; font-weight: 600; }");
        html.append(".trend-good { color: ").append(COL_GREEN).append("; background: rgba(39, 174, 96, 0.1); padding: 2px 8px; border-radius: 12px; }");
        html.append(".trend-bad { color: ").append(COL_RED_KPI).append("; background: rgba(231, 76, 60, 0.1); padding: 2px 8px; border-radius: 12px; }");
        html.append(".trend-neutral { color: #95a5a6; background: rgba(149, 165, 166, 0.1); padding: 2px 8px; border-radius: 12px; }");
        html.append("h2 { font-size: 18px; color: ").append(COL_BLUE).append("; margin-bottom: 15px; border-bottom: 2px solid #eee; padding-bottom: 10px; }");
        html.append("table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 0; }");
        html.append("th { background-color: ").append(COL_BLUE).append("; color: white; padding: 12px 6px; text-align: center; border: 1px solid #0d7ab0; }");
        html.append("td { padding: 8px 6px; text-align: center; border: 1px solid #ddd; color: #444; }");
        html.append(".sep-border { border-left: 3px solid #555 !important; }");
        html.append(".row-name { text-align: left; font-weight: 700; color: ").append(COL_BLUE).append("; width: 220px; padding-left: 15px; background-color:#fff;}");
        html.append(".total-row td { font-weight: bold; background-color: #eee; border-top: 2px solid #333; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");

        // Header
        html.append("<div class='header-row'>");
        html.append("<h1>BT TEAM DASHBOARD FOR LOCAM</h1>");
        html.append("<button class='email-btn' onclick='copyReportForEmail()'>📧 Copier pour Outlook</button>");
        html.append("</div>");

        // Zone de capture
        html.append("<div id='capture-area' style='background:white; padding:15px; border-radius:8px;'>");
        html.append("<div class='meta'>Généré le : ").append(dateGeneration).append("</div>");

        // 1. Dashboard KPI (incluant l'alerte Suffering)
        appendDashboard(html, data.dashboard, suffering);

        // 2. Tableau par thèmes
        html.append("<h2>Number of tickets MEP1+MEP2 by theme</h2>");
        appendThemeTable(html, data.themeStats);

        // 3. Tableau détail Planning
        html.append("<h2>Focus on BT Team Members</h2>");
        appendDetailTable(html, data.planning);

        html.append("</div>");

        // Script JS
        appendCopyScript(html);

        html.append("</div></body></html>");

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(html.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendDashboard(StringBuilder html, ResourcePlanningService.DashboardMetrics kpis, List<SufferingTheme> suffering) {
        html.append("<div class='dashboard'>");
        appendKpiCard(html, "New tickets assigned to Codix", kpis.stockWeb, false, false);
        appendKpiCard(html, "Answers sent from Codix to LOCAM", kpis.replies, true, false);
        appendKpiCard(html, "Tickets closed by LOCAM", kpis.closed, true, false);
        appendKpiCard(html, "Number of web tickets assigned to BTTEAM", kpis.stockGlobal, false, false);
        appendKpiCard(html, "% without answers > 7d assigned to BTTEAM", kpis.stalePercent, false, true);

        // Ajout de l'encart de tension si nécessaire
        if (suffering != null && !suffering.isEmpty()) {
            appendSufferingKpiCard(html, suffering);
        }

        html.append("</div>");
    }

    private void appendSufferingKpiCard(StringBuilder html, List<SufferingTheme> suffering) {
        html.append("<div class='kpi-card' style='border-top: 4px solid ").append(COL_RED_KPI).append("; text-align: left;'>");
        html.append("<div class='kpi-title' style='color:").append(COL_RED_KPI).append("; justify-content: flex-start;'>UNDER-CAPACITY THEMES (JANUARY FORECAST)</div>");

        html.append("<div style='padding-top: 5px;'>");
        for (SufferingTheme st : suffering) {
            html.append("<div style='margin-bottom: 8px; font-size: 13px; border-bottom: 1px ghostwhite solid; padding-bottom: 3px;'>");
            html.append("<b style='color:#333;'>").append(st.getName()).append("</b> : ");
            html.append("<span style='color:").append(COL_RED_KPI).append("; font-weight:bold;'>+").append(String.format("%.1f", st.getExtraResources())).append(" resources needed</span>");
            html.append("</div>");
        }
        html.append("</div>");
        html.append("</div>");
    }

    private void appendKpiCard(StringBuilder html, String title, ResourcePlanningService.KpiMetric metric, boolean higherIsBetter, boolean isPercent) {
        html.append("<div class='kpi-card'>");
        html.append("<div class='kpi-title'>").append(title).append("</div>");
        String valStr = isPercent ? String.format("%.1f%%", metric.current) : String.format("%.0f", metric.current);
        html.append("<div class='kpi-value'>").append(valStr).append("</div>");

        double diff = metric.current - metric.previous;
        String trendHtml;
        if (metric.previous == 0 && !isPercent) {
            trendHtml = "<span class='trend-neutral'>-</span>";
        } else if (Math.abs(diff) < 0.1) {
            trendHtml = "<span class='trend-neutral'>▶ Stable</span>";
        } else {
            boolean isGood = higherIsBetter ? (diff > 0) : (diff < 0);
            String arrow = diff > 0 ? "▲" : "▼";
            String classCss = isGood ? "trend-good" : "trend-bad";
            String diffStr = isPercent ? String.format("%+.1f%%", diff) : String.format("%+.0f", diff);
            trendHtml = "<span class='" + classCss + "'>" + arrow + " " + diffStr + " vs S-1</span>";
        }
        html.append("<div class='kpi-trend'>").append(trendHtml).append("</div>");
        html.append("</div>");
    }

    private void appendThemeTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }

        html.append("<table><thead><tr><th></th>");
        for (String theme : stats.keySet()) {
            html.append("<th>").append(theme).append("</th>");
        }
        html.append("<th>TOTAL</th></tr></thead><tbody>");

        int gtLocam = 0, gtCodix = 0, maxVal = 0;
        Map<String, Integer> colTotals = new LinkedHashMap<>();
        for (String t : stats.keySet()) {
            int l = stats.get(t).get("LOCAM"), c = stats.get(t).get("Codix");
            gtLocam += l;
            gtCodix += c;
            colTotals.put(t, l + c);
            maxVal = Math.max(maxVal, Math.max(l, c));
        }

        html.append("<tr><td class='row-name'>LOCAM</td>");
        for (String t : stats.keySet()) {
            html.append(cell(stats.get(t).get("LOCAM"), maxVal));
        }
        html.append("<td style='font-weight:bold; background:#eee;'>").append(gtLocam).append("</td></tr>");

        html.append("<tr><td class='row-name'>Codix</td>");
        for (String t : stats.keySet()) {
            html.append(cell(stats.get(t).get("Codix"), maxVal));
        }
        html.append("<td style='font-weight:bold; background:#eee;'>").append(gtCodix).append("</td></tr>");

        html.append("<tr class='total-row'><td style='text-align:right; padding-right:15px;'>TOTAL</td>");
        int totalMax = colTotals.values().stream().max(Integer::compare).orElse(0);
        for (String t : stats.keySet()) {
            html.append(cell(colTotals.get(t), totalMax));
        }
        html.append("<td class='sep-border'>").append(gtLocam + gtCodix).append("</td></tr>");
        html.append("</tbody></table><br>");
    }

    private String cell(int val, int max) {
        String color = "#ffffff";
        if (val > 0 && max > 0) {
            double ratio = Math.min((double) val / (double) max, 1.0);
            int r = (int) (255 + (204 - 255) * ratio), g = (int) (255 + (50 - 255) * ratio), b = (int) (255 + (90 - 255) * ratio);
            color = String.format("#%02x%02x%02x", r, g, b);
        }
        return "<td style='background-color:" + color + "'>" + val + "</td>";
    }

    private void appendDetailTable(StringBuilder html, PlanningData data) {
    // Calcul des totaux pour l'activité passée et le stock
    Map<Integer, Double> totalTimeWeek = new LinkedHashMap<>();
    Map<Integer, Integer> totalAssignedWeek = new LinkedHashMap<>();
    
    for (Integer w : data.weeks) {
        totalTimeWeek.put(w, 0.0);
        totalAssignedWeek.put(w, 0);
    }

    for (ResourcePlanningService.UserStats u : data.userStats.values()) {
        for (Integer w : data.weeks) {
            totalTimeWeek.merge(w, u.timePerWeek.getOrDefault(w, 0.0), Double::sum);
            totalAssignedWeek.merge(w, u.assignedPerWeek.getOrDefault(w, 0), Integer::sum);
        }
    }

    html.append("<table><thead>");

    // Ligne d'en-tête 1 : Groupement des catégories
    html.append("<tr>");
    html.append("<th rowspan='2' style='background:white; border:none;'></th>");
    html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border'>Past Activity (Days)</th>");
    html.append("<th colspan='").append(data.nextWeeks.size()).append("' class='sep-border' style='background-color:#f39c12'>Upcoming Absences (Next 4 Weeks)</th>");
    html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border'>Current Stock (Assigned)</th>");
    html.append("</tr>");

    // Ligne d'en-tête 2 : Numéros de semaines
    html.append("<tr>");
    for (Integer w : data.weeks) html.append("<th class='sep-border'>W").append(w).append("</th>");
    for (Integer w : data.nextWeeks) html.append("<th style='background-color:#e67e22'>W").append(w).append("</th>");
    for (Integer w : data.weeks) html.append("<th class='sep-border'>W").append(w).append("</th>");
    html.append("</tr></thead><tbody>");

    // Lignes de données par collaborateur
    for (String login : ResourcePlanningService.TARGET_USERS.keySet()) {
        ResourcePlanningService.UserStats user = data.userStats.get(login);
        if (user == null) continue;

        html.append("<tr>");
        html.append("<td class='row-name'>").append(user.fullName).append("</td>");

        // 1. Bloc Activité Passée (Heatmap verte)
        for (Integer w : data.weeks) {
            double val = user.timePerWeek.getOrDefault(w, 0.0);
            html.append("<td style='background-color:").append(getGreenHeatmap(val)).append("'>");
            html.append(val > 0.05 ? String.format("%.1f", val) : "0,0").append("</td>");
        }

        // 2. Bloc Absences Futures (Indicateur rouge "ABS")
        for (Integer w : data.nextWeeks) {
            boolean isAbsent = data.userAbsences.containsKey(login) && data.userAbsences.get(login).contains(w);
            String bgColor = isAbsent ? "#e74c3c" : "#ecf0f1"; // Rouge si absent, gris clair sinon
            String text = isAbsent ? "<b style='color:white'>ABS</b>" : "";
            html.append("<td style='background-color:").append(bgColor).append("'>").append(text).append("</td>");
        }

        // 3. Bloc Stock Actuel (Heatmap rouge)
        for (Integer w : data.weeks) {
            int val = user.assignedPerWeek.getOrDefault(w, 0);
            html.append("<td class='sep-border' style='background-color:").append(getRedHeatmap(val)).append("'>");
            html.append(val).append("</td>");
        }
        html.append("</tr>");
    }

    // Ligne de TOTAL au bas du tableau
    html.append("<tr class='total-row'><td style='text-align:right; padding-right:15px;'>TOTAL</td>");
    
    // Totaux Activité
    for (Integer w : data.weeks) {
        html.append("<td class='sep-border'>").append(String.format("%.1f", totalTimeWeek.get(w))).append("</td>");
    }
    // Cases vides pour les absences futures (Pas de total possible)
    for (Integer w : data.nextWeeks) {
        html.append("<td style='background-color:#eee;'></td>");
    }
    // Totaux Stock
    for (Integer w : data.weeks) {
        html.append("<td class='sep-border'>").append(totalAssignedWeek.get(w)).append("</td>");
    }
    
    html.append("</tr></tbody></table>");
}

    private String getGreenHeatmap(double val) {
        if (val <= 0.05) {
            return "#ffffff";
        }
        double ratio = Math.min(val / 5.0, 1.0);
        return String.format("#%02x%02x%02x", (int) (255 - 209 * ratio), (int) (255 - 51 * ratio), (int) (255 - 142 * ratio));
    }

    private String getRedHeatmap(int val) {
        if (val == 0) {
            return "#ffffff";
        }
        double ratio = Math.min((double) val / 20.0, 1.0);
        return String.format("#%02x%02x%02x", (int) (255 - 51 * ratio), (int) (255 - 205 * ratio), (int) (255 - 165 * ratio));
    }

    private void appendCopyScript(StringBuilder html) {
        html.append("<script>");
        html.append("function copyReportForEmail() {");
        html.append("  const btn = document.querySelector('.email-btn'); const originalText = btn.innerText;");
        html.append("  btn.innerText = 'Capture...';");
        html.append("  html2canvas(document.getElementById('capture-area'), { scale: 2, useCORS: true }).then(canvas => {");
        html.append("    canvas.toBlob(blob => {");
        html.append("      const item = new ClipboardItem({ 'image/png': blob });");
        html.append("      navigator.clipboard.write([item]).then(() => {");
        html.append("        btn.innerText = 'Copié !'; btn.style.backgroundColor = '#27ae60';");
        html.append("        setTimeout(() => { btn.innerText = originalText; btn.style.backgroundColor = '").append(COL_OUTLOOK).append("'; }, 2000);");
        html.append("      });");
        html.append("    });");
        html.append("  });");
        html.append("}");
        html.append("</script>");
    }
}
