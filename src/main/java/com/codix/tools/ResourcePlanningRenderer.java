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

    public void generate(ResourcePlanningService.ReportData data, String filename) {
        StringBuilder html = new StringBuilder();
        String dateGeneration = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        html.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        // Inclusion de html2canvas pour la capture d'écran
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

        html.append(".dashboard { display: flex; gap: 20px; margin-bottom: 40px; justify-content: space-between; }");
        html.append(".kpi-card { flex: 1; background: #fff; border: 1px solid #e1e4e8; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); text-align: center; position: relative; border-top: 4px solid ").append(COL_BLUE).append("; }");
        html.append(".kpi-title { font-size: 13px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; font-weight: 600; height: 35px; display: flex; align-items: center; justify-content: center; }");
        html.append(".kpi-value { font-size: 32px; font-weight: 800; color: #2c3e50; margin-bottom: 5px; }");
        html.append(".kpi-trend { font-size: 12px; display: flex; align-items: center; justify-content: center; gap: 5px; font-weight: 600; }");
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
        
        // Header avec bouton (Exclu de la capture)
        html.append("<div class='header-row'>");
        html.append("<h1>BT TEAM DASHBOARD FOR LOCAM</h1>");
        html.append("<button class='email-btn' onclick='copyReportForEmail()'>📧 Copier pour Outlook</button>");
        html.append("</div>");

        // Zone à copier (Dashboard + Tableau)
        html.append("<div id='capture-area' style='background:white; padding:15px; border-radius:8px;'>");
        html.append("<div class='meta'>Généré le : ").append(dateGeneration).append("</div>");

        appendDashboard(html, data.dashboard);

        html.append("<h2>Focus on BT Team Members</h2>");
        appendDetailTable(html, data.planning);
        html.append("</div>"); // Fin capture-area

        // Script de capture
        html.append("<script>");
        html.append("function copyReportForEmail() {");
        html.append("  const btn = document.querySelector('.email-btn');");
        html.append("  const originalText = btn.innerText;");
        html.append("  btn.innerText = 'Capture en cours...';");
        html.append("  html2canvas(document.getElementById('capture-area'), { scale: 2, useCORS: true, logging: false }).then(canvas => {");
        html.append("    canvas.toBlob(blob => {");
        html.append("      const item = new ClipboardItem({ 'image/png': blob });");
        html.append("      navigator.clipboard.write([item]).then(() => {");
        html.append("        btn.innerText = 'Copié !';");
        html.append("        btn.style.backgroundColor = '#27ae60';");
        html.append("        setTimeout(() => { btn.innerText = originalText; btn.style.backgroundColor = '").append(COL_OUTLOOK).append("'; }, 2000);");
        html.append("      }).catch(err => {");
        html.append("        console.error('Erreur :', err);");
        html.append("        btn.innerText = 'Erreur';");
        html.append("        setTimeout(() => { btn.innerText = originalText; }, 2000);");
        html.append("      });");
        html.append("    }, 'image/png');");
        html.append("  });");
        html.append("}");
        html.append("</script>");
        
        html.append("</div></body></html>");

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(html.toString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void appendDashboard(StringBuilder html, ResourcePlanningService.DashboardMetrics kpis) {
        html.append("<div class='dashboard'>");
        appendKpiCard(html, "New tickets assigned to Codix", kpis.stockWeb, false, false);
        appendKpiCard(html, "Answers sent to LOCAM", kpis.replies, true, false);
        appendKpiCard(html, "Tickets closed by LOCAM", kpis.closed, true, false);
        appendKpiCard(html, "Number of tickets assigned to Codix", kpis.stockGlobal, false, false);
        appendKpiCard(html, "% without answers > 7d", kpis.stalePercent, false, true);
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
            trendHtml = "<span class='trend-neutral'>&#9654; Stable</span>";
        } else {
            boolean isGood = higherIsBetter ? (diff > 0) : (diff < 0);
            String arrow = diff > 0 ? "&#9650;" : "&#9660;";
            String classCss = isGood ? "trend-good" : "trend-bad";
            String diffStr = isPercent ? String.format("%+.1f%%", diff) : String.format("%+.0f", diff);
            trendHtml = "<span class='" + classCss + "'>" + arrow + " " + diffStr + " vs S-1</span>";
        }
        html.append("<div class='kpi-trend'>").append(trendHtml).append("</div>");
        html.append("</div>");
    }

    private void appendDetailTable(StringBuilder html, ResourcePlanningService.PlanningData data) {
        Map<Integer, Double> totalTimeWeek = new HashMap<>();
        Map<Integer, Integer> totalAssignedWeek = new HashMap<>();
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

        html.append("<table><thead><tr>");
        html.append("<th rowspan='2' style='background:white; border:none;'></th>");
        html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border'>Time spent on LOCAM (Jours/Homme)</th>");
        html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border'>Number of DEV + WEB tickets assigned</th>");
        html.append("</tr><tr>");
        for (Integer w : data.weeks) html.append("<th class='sep-border'>W").append(w).append("</th>");
        for (Integer w : data.weeks) html.append("<th class='sep-border'>W").append(w).append("</th>");
        html.append("</tr></thead><tbody>");

        for (String login : ResourcePlanningService.TARGET_USERS.keySet()) {
            ResourcePlanningService.UserStats user = data.userStats.get(login);
            if (user == null) continue;
            html.append("<tr>");
            html.append("<td class='row-name'>").append(user.fullName).append("</td>");
            for (int i = 0; i < data.weeks.size(); i++) {
                Integer w = data.weeks.get(i);
                double val = user.timePerWeek.getOrDefault(w, 0.0);
                String cssClass = (i == 0) ? "sep-border" : ""; 
                html.append("<td class='").append(cssClass).append("' style='background-color:").append(getGreenHeatmap(val)).append("'>");
                html.append(val > 0.05 ? String.format("%.1f", val) : "0,0");
                html.append("</td>");
            }
            for (int i = 0; i < data.weeks.size(); i++) {
                Integer w = data.weeks.get(i);
                int val = user.assignedPerWeek.getOrDefault(w, 0);
                String cssClass = (i == 0) ? "sep-border" : "";
                html.append("<td class='").append(cssClass).append("' style='background-color:").append(getRedHeatmap(val)).append("'>");
                html.append(val);
                html.append("</td>");
            }
            html.append("</tr>");
        }
        
        html.append("<tr class='total-row'><td style='text-align:right; padding-right:15px;'>TOTAL</td>");
        for (int i = 0; i < data.weeks.size(); i++) {
            Integer w = data.weeks.get(i);
            String cssClass = (i == 0) ? "sep-border" : "";
            html.append("<td class='").append(cssClass).append("'>").append(String.format("%.1f", totalTimeWeek.get(w))).append("</td>");
        }
        for (int i = 0; i < data.weeks.size(); i++) {
            Integer w = data.weeks.get(i);
            String cssClass = (i == 0) ? "sep-border" : "";
            html.append("<td class='").append(cssClass).append("'>").append(totalAssignedWeek.get(w)).append("</td>");
        }
        html.append("</tr></tbody></table>");
    }

    private String getGreenHeatmap(double val) {
        if (val <= 0.05) return "#ffffff";
        double ratio = Math.min(val / 5.0, 1.0); 
        int r = (int) (255 + (46 - 255) * ratio);
        int g = (int) (255 + (204 - 255) * ratio);
        int b = (int) (255 + (113 - 255) * ratio);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private String getRedHeatmap(int val) {
        if (val == 0) return "#ffffff";
        double ratio = Math.min((double)val / 20.0, 1.0);
        int r = (int) (255 + (204 - 255) * ratio);
        int g = (int) (255 + (50 - 255) * ratio);
        int b = (int) (255 + (90 - 255) * ratio);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}