package edu.asu.smartcity.server.dao;

import edu.asu.smartcity.server.db.DatabaseConnection;
import edu.asu.smartcity.server.dto.TransitRouteDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** SQL for the read-only transit_routes table (seeded by db/seed.sql). */
public class TransitDAO {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final DatabaseConnection database;

    public TransitDAO(DatabaseConnection database) {
        this.database = database;
    }

    /** Every scheduled trip, or only one route's trips when {@code route} is not null; sorted by route, then time. */
    public List<TransitRouteDto> findSchedule(String route) throws SQLException {
        String sql = "SELECT id, route, stop, departure_time, arrival_time FROM transit_routes"
                + (route == null ? "" : " WHERE route = ?")
                + " ORDER BY route, departure_time, stop";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (route != null) {
                stmt.setString(1, route);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<TransitRouteDto> trips = new ArrayList<>();
                while (rs.next()) {
                    trips.add(new TransitRouteDto(rs.getInt("id"), rs.getString("route"), rs.getString("stop"),
                            rs.getTime("departure_time").toLocalTime().format(TIME),
                            rs.getTime("arrival_time").toLocalTime().format(TIME)));
                }
                return trips;
            }
        }
    }
}
