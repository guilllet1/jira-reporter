package com.codix.tools;

import java.util.*;

public class PlanningData {
    public List<Integer> weeks = new ArrayList<>();
    public List<Integer> nextWeeks = new ArrayList<>();
    public Map<Integer, ResourcePlanningService.WeekRange> weekDates = new HashMap<>();
    public Map<String, ResourcePlanningService.UserStats> userStats = new LinkedHashMap<>();
    
    // Stockage du delta hebdomadaire : Login -> (Semaine -> Delta)
    public Map<String, Map<Integer, Integer>> userWeeklyDeltas = new HashMap<>();

    public void setWeeklyDelta(String login, int week, int delta) {
        userWeeklyDeltas.computeIfAbsent(login, k -> new HashMap<>()).put(week, delta);
    }

    public int getWeeklyDelta(String login, int week) {
        return userWeeklyDeltas.getOrDefault(login, Collections.emptyMap()).getOrDefault(week, 0);
    }

    // Gardé pour compatibilité si nécessaire
    public void addTime(String login, int week, double days) {
        if (userStats.containsKey(login)) userStats.get(login).timePerWeek.merge(week, days, Double::sum);
    }

    public void setAssigned(String login, int week, int count) {
        if (userStats.containsKey(login)) userStats.get(login).assignedPerWeek.put(week, count);
    }
}