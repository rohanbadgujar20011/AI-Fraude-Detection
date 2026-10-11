package com.frauddetection.util;

import java.util.HashMap;
import java.util.Map;

public class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final Map<String, double[]> CITY_COORDINATES = new HashMap<>();

    static {
        CITY_COORDINATES.put("IN-PUNE", new double[]{18.5204, 73.8567});
        CITY_COORDINATES.put("IN-MUMBAI", new double[]{19.0760, 72.8777});
        CITY_COORDINATES.put("IN-DELHI", new double[]{28.6139, 77.2090});
        CITY_COORDINATES.put("IN-BANGALORE", new double[]{12.9716, 77.5946});
        CITY_COORDINATES.put("US-NYC", new double[]{40.7128, -74.0060});
        CITY_COORDINATES.put("US-SFO", new double[]{37.7749, -122.4194});
        CITY_COORDINATES.put("UK-LON", new double[]{51.5074, -0.1278});
    }

    public static double[] parseCoordinates(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String clean = location.trim().toUpperCase();
        if (CITY_COORDINATES.containsKey(clean)) {
            return CITY_COORDINATES.get(clean);
        }

        if (location.contains(",")) {
            try {
                String[] parts = location.split(",");
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                return new double[]{lat, lon};
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static double calculateSpeedKmH(double distanceKm, long secondsElapsed) {
        if (secondsElapsed <= 0) {
            return distanceKm > 0 ? 999999.0 : 0.0;
        }
        double hours = secondsElapsed / 3600.0;
        return distanceKm / hours;
    }
}
