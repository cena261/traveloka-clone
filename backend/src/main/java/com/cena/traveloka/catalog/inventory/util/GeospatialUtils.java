package com.cena.traveloka.catalog.inventory.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class GeospatialUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Creates a PostGIS Point geometry from latitude and longitude
     */
    public static Point createPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Calculates the distance between two points using the Haversine formula
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Validates if coordinates are within valid ranges
     */
    public static boolean isValidCoordinates(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0 &&
               longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Converts kilometers to meters for PostGIS distance queries
     */
    public static double kmToMeters(double kilometers) {
        return kilometers * 1000.0;
    }

    /**
     * Converts meters to kilometers
     */
    public static double metersToKm(double meters) {
        return meters / 1000.0;
    }

    /**
     * Creates a bounding box around a point with given radius in kilometers
     */
    public static BoundingBox createBoundingBox(double latitude, double longitude, double radiusKm) {
        // Approximate degrees per kilometer
        double degPerKmLat = 1.0 / 111.0;
        double degPerKmLon = 1.0 / (111.0 * Math.cos(Math.toRadians(latitude)));

        double latDelta = radiusKm * degPerKmLat;
        double lonDelta = radiusKm * degPerKmLon;

        return new BoundingBox(
            latitude - latDelta,   // minLat
            latitude + latDelta,   // maxLat
            longitude - lonDelta,  // minLon
            longitude + lonDelta   // maxLon
        );
    }

    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {}
}