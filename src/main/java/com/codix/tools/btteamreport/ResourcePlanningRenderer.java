package com.codix.tools.btteamreport;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResourcePlanningRenderer {

    private static final String COL_BLUE = "#1197D6";
    private static final String COL_RED_CODIX = "#CC325A";
    private static final String COL_GREEN = "#27ae60";
    private static final String COL_RED_KPI = "#e74c3c";
    private static final String COL_OUTLOOK = "#0078d4";
    private static final String COL_ORANGE = "#f39c12";

    public void generate(ResourcePlanningService.ReportData data, ResourcePlanningService.CapacityAlerts alerts, String filename) {
        // Initialisation obligatoire pour le support UTF-8 (Consigne utilisateur)
        try { 
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8)); 
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8)); 
        } catch (Exception e) { e.printStackTrace(); }

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
        html.append(".kpi-card { flex: 1; background: #fff; border: 1px solid #e1e4e8; border-radius: 8px; padding: 15px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); text-align: center; border-top: 4px solid ").append(COL_BLUE).append("; display: flex; flex-direction: column; justify-content: center; }");
        html.append(".kpi-title { font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; font-weight: 700; min-height: 30px; display: flex; align-items: center; justify-content: center; }");
        html.append(".kpi-value { font-size: 26px; font-weight: 800; color: #2c3e50; margin-bottom: 5px; }");
        html.append(".trend-good { color: ").append(COL_GREEN).append("; background: rgba(39, 174, 96, 0.1); padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }");
        html.append(".trend-bad { color: ").append(COL_RED_KPI).append("; background: rgba(231, 76, 60, 0.1); padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }");
        html.append("h2 { font-size: 16px; color: ").append(COL_BLUE).append("; margin: 30px 0 15px 0; border-bottom: 2px solid #eee; padding-bottom: 8px; text-transform: uppercase; }");
        html.append("table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 20px; }");
        html.append("th { color: white; padding: 10px 5px; text-align: center; border: 1px solid rgba(255,255,255,0.2); font-weight: 700; }");
        html.append("td { padding: 8px 5px; text-align: center; border: 1px solid #ddd; color: #444; }");
        html.append(".sep-border { border-left: 2px solid #333 !important; }");
        html.append(".row-name { text-align: left; font-weight: 700; color: ").append(COL_BLUE).append("; width: 220px; padding-left: 15px; background: #fff !important; }");
        html.append(".total-row td { font-weight: 800; background-color: #f8f9fa; border-top: 2px solid #333; color: #2c3e50; }");
        html.append(".header-group { font-size: 13px; letter-spacing: 1px; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header-row'>");
        html.append("<h1>BT TEAM DASHBOARD FOR LOCAM</h1>");
        html.append("<button class='email-btn' onclick='copyReportForEmail()'>📧 Copier pour Outlook</button>");
        html.append("</div>");

        html.append("<div id='capture-area' style='background:white; padding:15px; border-radius:8px;'>");
        html.append("<div class='meta'>Généré le : ").append(dateGeneration).append("</div>");

        appendDashboard(html, data.dashboard, alerts);

        html.append("<h2>Number of tickets MEP1+MEP2 by theme</h2>");
        appendThemeTable(html, data.themeStats);

        html.append("<h2>Focus on BT Team Members</h2>");
        appendDetailTable(html, data.planning);

        html.append("</div>");
        appendCopyScript(html);
        html.append("</div></body></html>");

        try (FileWriter writer = new FileWriter(filename)) { writer.write(html.toString()); } 
        catch (IOException e) { e.printStackTrace(); }
    }

    private void appendDetailTable(StringBuilder html, PlanningData data) {
        Map<Integer, Double> totalTimeWeek = new LinkedHashMap<>();
        Map<Integer, Integer> totalAssignedWeek = new LinkedHashMap<>();
        for (Integer w : data.weeks) { totalTimeWeek.put(w, 0.0); totalAssignedWeek.put(w, 0); }

        for (ResourcePlanningService.UserStats u : data.userStats.values()) {
            for (Integer w : data.weeks) {
                totalTimeWeek.merge(w, u.timePerWeek.getOrDefault(w, 0.0), Double::sum);
                totalAssignedWeek.merge(w, u.assignedPerWeek.getOrDefault(w, 0), Integer::sum);
            }
        }

        html.append("<table><thead>");
        // Ligne 1 : Groupes
        html.append("<tr>");
        html.append("<th style='background:white; border:none;'></th>");
        html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border header-group' style='background:").append(COL_BLUE).append(";'>PAST ACTIVITY (DAYS)</th>");
        html.append("<th colspan='").append(data.nextWeeks.size()).append("' class='sep-border header-group' style='background:").append(COL_ORANGE).append(";'>UPCOMING ABSENCES</th>");
        html.append("<th colspan='").append(data.weeks.size()).append("' class='sep-border header-group' style='background:").append(COL_BLUE).append(";'>CURRENT STOCK (ASSIGNED)</th>");
        html.append("</tr>");

        // Ligne 2 : Semaines
        html.append("<tr>");
        html.append("<th style='background:white; border:none;'></th>");
        for (Integer w : data.weeks) html.append("<th style='background:").append(COL_BLUE).append("CC; border-left:1px solid #fff3;'>W").append(w).append("</th>");
        for (Integer w : data.nextWeeks) html.append("<th style='background:").append(COL_ORANGE).append("CC; border-left:1px solid #fff3;'>W").append(w).append("</th>");
        for (Integer w : data.weeks) html.append("<th style='background:").append(COL_BLUE).append("CC; border-left:1px solid #fff3;'>W").append(w).append("</th>");
        html.append("</tr></thead><tbody>");

        for (String login : ResourcePlanningService.TARGET_USERS.keySet()) {
            ResourcePlanningService.UserStats user = data.userStats.get(login);
            if (user == null) continue;

            html.append("<tr>");
            html.append("<td class='row-name'>").append(user.fullName).append("</td>");

            // 1. Activité Passée
            for (Integer w : data.weeks) {
                double val = user.timePerWeek.getOrDefault(w, 0.0);
                html.append("<td class='").append(w.equals(data.weeks.get(0)) ? "sep-border" : "").append("' style='background-color:").append(getGreenHeatmap(val)).append("; color:").append(val > 3.5 ? "#fff" : "#444").append(";'>");
                html.append(val > 0.05 ? String.format("%.1f", val) : "-").append("</td>");
            }

            // 2. Absences (Heatmap Orange/Vert)
            for (Integer w : data.nextWeeks) {
                int delta = data.getWeeklyDelta(login, w);
                html.append("<td class='").append(w.equals(data.nextWeeks.get(0)) ? "sep-border" : "").append("' style='background-color:").append(getOrangeHeatmap(delta)).append("; font-weight:700; color:").append(Math.abs(delta) > 3 ? "#fff" : "#444").append(";'>");
                html.append(delta == 0 ? "0" : (delta > 0 ? "+" + delta : delta)).append("</td>");
            }

            // 3. Stock
            for (Integer w : data.weeks) {
                int val = user.assignedPerWeek.getOrDefault(w, 0);
                html.append("<td class='").append(w.equals(data.weeks.get(0)) ? "sep-border" : "").append("' style='background-color:").append(getRedHeatmap(val)).append("; color:").append(val > 12 ? "#fff" : "#444").append(";'>");
                html.append(val > 0 ? val : "-").append("</td>");
            }
            html.append("</tr>");
        }

        // Pied de tableau : TOTAL
        html.append("<tr class='total-row'><td style='text-align:right; padding-right:15px;'>TOTAL TEAM</td>");
        for (Integer w : data.weeks) html.append("<td class='sep-border'>").append(String.format("%.1f", totalTimeWeek.get(w))).append("</td>");
        for (Integer w : data.nextWeeks) html.append("<td class='sep-border' style='background:#f1f1f1;'></td>");
        for (Integer w : data.weeks) html.append("<td class='sep-border'>").append(totalAssignedWeek.get(w)).append("</td>");
        html.append("</tr></tbody></table>");
    }

    private String getGreenHeatmap(double val) {
        if (val <= 0.05) return "#ffffff";
        double ratio = Math.min(val / 5.0, 1.0);
        return String.format("#%02x%02x%02x", (int) (255 - (255 - 39) * ratio), (int) (255 - (255 - 174) * ratio), (int) (255 - (255 - 96) * ratio));
    }

    private String getRedHeatmap(int val) {
        if (val == 0) return "#ffffff";
        double ratio = Math.min((double) val / 20.0, 1.0);
        return String.format("#%02x%02x%02x", (int) (255 - (255 - 231) * ratio), (int) (255 - (255 - 76) * ratio), (int) (255 - (255 - 60) * ratio));
    }

    private String getOrangeHeatmap(int delta) {
        if (delta == 0) return "#ffffff";
        if (delta > 0) return "#e8f5e9";
        double ratio = Math.min(Math.abs((double) delta) / 5.0, 1.0);
        return String.format("#ff%02x%02x", (int) (255 - 90 * ratio), (int) (255 - 180 * ratio));
    }

    private void appendDashboard(StringBuilder html, ResourcePlanningService.DashboardMetrics kpis, ResourcePlanningService.CapacityAlerts alerts) {
        html.append("<div class='dashboard'>");
        appendKpiCard(html, "New tickets assigned to Codix", kpis.stockWeb, false, false);
        appendKpiCard(html, "Answers sent to LOCAM", kpis.replies, true, false);
        appendKpiCard(html, "Tickets closed by LOCAM", kpis.closed, true, false);
        appendKpiCard(html, "Web tickets BTTEAM", kpis.stockGlobal, false, false);
        appendKpiCard(html, "% No answer > 7d assigned to BTTEAM", kpis.stalePercent, false, true);
        if (alerts != null && (!alerts.underCapacity.isEmpty() || !alerts.overCapacity.isEmpty())) appendSufferingKpiCard(html, alerts);
        html.append("</div>");
    }

    private void appendSufferingKpiCard(StringBuilder html, ResourcePlanningService.CapacityAlerts alerts) {
        html.append("<div class='kpi-card' style='border-top: 4px solid ").append(COL_RED_KPI).append("; text-align: left; min-width: 550px; flex: 2.5;'>");
        html.append("<div class='kpi-title' style='color:").append(COL_RED_KPI).append("; justify-content: flex-start;'>CAPACITY STATUS</div>");
        html.append("<div style='display: flex; gap: 15px;'>");
        html.append("<div style='flex: 1;'>");
        html.append("<div style='font-size: 10px; font-weight: 700; color: #c0392b; margin-bottom: 5px;'>⚠️ OVERLOAD</div>");
        for (SufferingTheme st : alerts.underCapacity) {
            html.append("<div style='background: #fff5f5; padding: 5px 8px; border-radius: 4px; border: 1px solid #ffccd5; margin-bottom: 4px; font-size: 11px;'>");
            html.append("<b>").append(st.getName()).append("</b>: <span style='color:#c0392b;'>+").append(String.format("%.1f", st.getExtraResources())).append(" res. needed</span>");
            html.append("</div>");
        }
        html.append("</div>");
        html.append("<div style='flex: 1; border-left: 1px solid #eee; padding-left: 15px;'>");
        html.append("<div style='font-size: 10px; font-weight: 700; color: ").append(COL_GREEN).append("; margin-bottom: 5px;'>✅ AVAILABLE</div>");
        for (SufferingTheme st : alerts.overCapacity) {
            html.append("<div style='background: #f0fff4; padding: 5px 8px; border-radius: 4px; border: 1px solid #c6f6d5; margin-bottom: 4px; font-size: 11px;'>");
            html.append("<b>").append(st.getName()).append("</b>: <span style='color:").append(COL_GREEN).append(";'>").append(String.format("%.1f", Math.abs(st.getExtraResources()))).append(" res. available</span>");
            html.append("</div>");
        }
        html.append("</div></div></div>");
    }

    private void appendKpiCard(StringBuilder html, String title, ResourcePlanningService.KpiMetric metric, boolean higherIsBetter, boolean isPercent) {
        html.append("<div class='kpi-card'>");
        html.append("<div class='kpi-title'>").append(title).append("</div>");
        String valStr = isPercent ? String.format("%.1f%%", metric.current) : String.format("%.0f", metric.current);
        html.append("<div class='kpi-value'>").append(valStr).append("</div>");
        double diff = metric.current - metric.previous;
        if (Math.abs(diff) > 0.1) {
            boolean isGood = higherIsBetter ? (diff > 0) : (diff < 0);
            String arrow = diff > 0 ? "▲" : "▼";
            String diffStr = isPercent ? String.format("%+.1f%%", diff) : String.format("%+.0f", diff);
            html.append("<div><span class='").append(isGood ? "trend-good" : "trend-bad").append("'>").append(arrow).append(" ").append(diffStr).append(" vs S-1</span></div>");
        }
        html.append("</div>");
    }

    private void appendThemeTable(StringBuilder html, Map<String, Map<String, Integer>> stats) {
        if (stats == null || stats.isEmpty()) return;
        html.append("<table><thead><tr><th style='background:").append(COL_BLUE).append(";'>Source</th>");
        for (String theme : stats.keySet()) html.append("<th style='background:").append(COL_BLUE).append(";'>").append(theme).append("</th>");
        html.append("<th style='background:").append(COL_BLUE).append(";'>TOTAL</th></tr></thead><tbody>");
        int gtLocam = 0, gtCodix = 0;
        html.append("<tr><td class='row-name'>LOCAM</td>");
        for (String t : stats.keySet()) { int v = stats.get(t).get("LOCAM"); gtLocam += v; html.append("<td>").append(v).append("</td>"); }
        html.append("<td style='font-weight:700; background:#f8f9fa;'>").append(gtLocam).append("</td></tr>");
        html.append("<tr><td class='row-name'>Codix</td>");
        for (String t : stats.keySet()) { int v = stats.get(t).get("Codix"); gtCodix += v; html.append("<td>").append(v).append("</td>"); }
        html.append("<td style='font-weight:700; background:#f8f9fa;'>").append(gtCodix).append("</td></tr>");
        html.append("</tbody></table>");
    }

    private void appendCopyScript(StringBuilder html) {
        html.append("<script>function copyReportForEmail() { const btn = document.querySelector('.email-btn'); html2canvas(document.getElementById('capture-area'), { scale: 2 }).then(canvas => { canvas.toBlob(blob => { const item = new ClipboardItem({ 'image/png': blob }); navigator.clipboard.write([item]).then(() => { btn.innerText = 'Copié !'; btn.style.backgroundColor = '#27ae60'; setTimeout(() => { btn.innerText = '📧 Copier pour Outlook'; btn.style.backgroundColor = '").append(COL_OUTLOOK).append("'; }, 2000); }); }); }); }</script>");
    }
}