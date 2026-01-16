package com.codix.tools.management;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class JiraHtmlReport {

    public static void generate(List<JiraDownloader.TicketData> dataList, String filename) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        html.append("<title>Rapport Jira</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background-color: #f4f4f9; }");
        html.append("h1 { color: #333; }");
        html.append("table { border-collapse: collapse; width: 100%; box-shadow: 0 1px 3px rgba(0,0,0,0.2); background-color: #fff; }");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; vertical-align: top; text-align: left; font-size: 14px; }");
        html.append("th { background-color: #4a90e2; color: white; position: sticky; top: 0; cursor: pointer; user-select: none; }");
        html.append("th:hover { background-color: #357abd; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append("tr:hover { background-color: #f1f1f1; }");

        // --- CSS DES LIENS ---
        html.append("a { color: #4a90e2; text-decoration: none; font-weight: bold; }");
        html.append("a:visited { color: #4a90e2; }"); // Force le bleu par défaut
        html.append("a:hover { text-decoration: underline; }");
        html.append(".clicked-link { color: #8e44ad !important; }"); // Violet au clic (via JS)
        
        html.append(".badge-oui { color: #28a745; font-weight: bold; }");
        html.append(".badge-non { color: #ccc; }");
        html.append(".analysis { white-space: pre-wrap; font-family: Consolas, 'Courier New', monospace; font-size: 13px; color: #444; background: #fafafa; padding: 8px; border-radius: 4px; border: 1px solid #eee;}");
        html.append("</style>");
        html.append("</head><body>");

        html.append("<h1>Suivi Pilotage - ").append(LocalDate.now()).append("</h1>");
        html.append("<p><i>Cliquez sur les titres de colonnes pour trier.</i></p>");
        html.append("<table id='myTable'>");
        html.append("<thead><tr>");
        html.append("<th onclick='sortTable(0)'>Réf</th>");
        html.append("<th onclick='sortTable(1)'>Titre</th>");
        html.append("<th onclick='sortTable(2)'>Prio</th>");
        html.append("<th onclick='sortTable(3)'>Thème</th>");
        html.append("<th onclick='sortTable(4)'>DDCA</th>");
        html.append("<th onclick='sortTable(5)'>Jours Client</th>");
        html.append("<th onclick='sortTable(6)'>MAJ Auj.</th>");
        html.append("<th onclick='sortTable(7)'>IA</th>");
        html.append("<th onclick='sortTable(8)' style='width: 40%'>Analyse IA</th>");
        html.append("</tr></thead><tbody>");

        for (JiraDownloader.TicketData d : dataList) {
            html.append("<tr>");
            // Lien avec tracking JS
            html.append("<td><a href='https://tts.codix.eu/jira/browse/").append(d.key).append("' target='_blank' class='track-click'>").append(d.key).append("</a></td>");
            html.append("<td>").append(escapeXML(d.summary)).append("</td>");
            html.append("<td>").append(escapeXML(d.priority)).append("</td>");
            html.append("<td>").append(escapeXML(d.theme)).append("</td>");

            // --- LOGIQUE DDCA ---
            String ddcaHtml = "<span style='color:gray'>-</span>";
            long ddcaSortValue = 999999;

            if (d.ddca != null && !d.ddca.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(d.ddca, DateTimeFormatter.ISO_LOCAL_DATE);
                    LocalDate today = LocalDate.now();
                    DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                    ddcaSortValue = ChronoUnit.DAYS.between(today, date);

                    if (date.isBefore(today)) {
                        long businesDaysDelay = calculateBusinessDays(d.ddca);
                        ddcaHtml = "<span style='color:red; font-weight:bold'>OUTDATED " + businesDaysDelay + "</span>";
                    } else if (date.equals(today)) {
                        ddcaHtml = "<span style='color:orange; font-weight:bold'>" + date.format(outFmt) + "</span>";
                    } else if (date.equals(today.plusDays(1))) {
                        ddcaHtml = "<span style='color:orange'>" + date.format(outFmt) + "</span>";
                    } else {
                        ddcaHtml = "<span style='color:gray'>" + date.format(outFmt) + "</span>";
                    }
                } catch (Exception e) {
                    ddcaHtml = d.ddca;
                }
            }
            html.append("<td style='white-space:nowrap' data-sort='").append(ddcaSortValue).append("'>").append(ddcaHtml).append("</td>");

            long daysClient = calculateBusinessDays(d.clientAssignationDate);
            html.append("<td data-sort='").append(daysClient).append("'>").append(daysClient).append("</td>");

            html.append("<td class='").append(d.updatedToday ? "badge-oui" : "badge-non").append("'>").append(d.updatedToday ? "OUI" : "NON").append("</td>");
            html.append("<td>").append(d.aiCalled ? "Oui" : "Cache").append("</td>");
            html.append("<td class='analysis'>").append(escapeXML(d.aiAnalysis)).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        // --- SCRIPTS JS ---
        html.append("<script>");
        // Gestion du clic (changement de couleur immédiat)
        html.append("document.addEventListener('DOMContentLoaded', function() {");
        html.append("  var links = document.querySelectorAll('a.track-click');");
        html.append("  links.forEach(function(link) {");
        html.append("    link.addEventListener('click', function() {");
        html.append("      this.classList.add('clicked-link');");
        html.append("    });");
        html.append("  });");
        html.append("});");

        // Fonction de tri
        html.append("function sortTable(n) {");
        html.append("  var table, rows, switching, i, x, y, shouldSwitch, dir, switchcount = 0;");
        html.append("  table = document.getElementById('myTable');");
        html.append("  switching = true;");
        html.append("  dir = 'asc';");
        html.append("  while (switching) {");
        html.append("    switching = false;");
        html.append("    rows = table.rows;");
        html.append("    for (i = 1; i < (rows.length - 1); i++) {");
        html.append("      shouldSwitch = false;");
        html.append("      x = rows[i].getElementsByTagName('TD')[n];");
        html.append("      y = rows[i + 1].getElementsByTagName('TD')[n];");
        html.append("      var xVal = x.getAttribute('data-sort') || x.innerText.toLowerCase();");
        html.append("      var yVal = y.getAttribute('data-sort') || y.innerText.toLowerCase();");
        html.append("      var xNum = parseFloat(xVal);");
        html.append("      var yNum = parseFloat(yVal);");
        html.append("      var isNumeric = !isNaN(xNum) && !isNaN(yNum);");
        html.append("      if (dir == 'asc') {");
        html.append("        if (isNumeric ? (xNum > yNum) : (xVal > yVal)) { shouldSwitch = true; break; }");
        html.append("      } else if (dir == 'desc') {");
        html.append("        if (isNumeric ? (xNum < yNum) : (xVal < yVal)) { shouldSwitch = true; break; }");
        html.append("      }");
        html.append("    }");
        html.append("    if (shouldSwitch) {");
        html.append("      rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);");
        html.append("      switching = true;");
        html.append("      switchcount ++;");
        html.append("    } else {");
        html.append("      if (switchcount == 0 && dir == 'asc') { dir = 'desc'; switching = true; }");
        html.append("    }");
        html.append("  }");
        html.append("}");
        html.append("</script>");

        html.append("</body></html>");

        try {
            // Remplacement de Files.writeString par Files.write
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), html.toString().getBytes("UTF-8"));
            System.out.println("Fichier HTML généré : " + filename);
        } catch (Exception e) {
            System.err.println("Erreur génération HTML: " + e.getMessage());
        }
    }

    private static long calculateBusinessDays(String startDateStr) {
        if (startDateStr == null) return 0;
        try {
            LocalDate s = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate e = LocalDate.now();
            long d = 0;
            LocalDate current = s.plusDays(1); // On ne compte pas le jour même
            while (!current.isAfter(e)) {
                if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    d++;
                }
                current = current.plusDays(1);
            }
            return d;
        } catch (Exception e) { return 0; }
    }

    private static String escapeXML(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}