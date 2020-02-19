package net.sauray.domain;

import akka.persistence.query.Offset;
import akka.persistence.query.Sequence;
import net.sauray.infrastructure.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class OffsetStore {

    private final Connection conn;
    private AtomicReference<Long> currentOffset;

    private OffsetStore(Connection conn) {
        this.conn = conn;
        this.currentOffset = new AtomicReference<>(0L);
    }

    /*
    public CompletableFuture<Void> letMeKnow(UUID event) {
        return CompletableFuture.runAsync(() -> {
            while(currentOffset.get() < offset) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("READ SIDE IS NOT UP TO DATE, currently at " + currentOffset.get().toString() + ", waiting for " + offset);
                }
            }
            System.out.println("READ SIDE IS UP TO DATE, expected " + offset + ", currently at " + currentOffset.get().toString());
        });
    }
    */
    public CompletionStage<Long> updateOffset(String readSideId, Offset offset) {
        return CompletableFuture.supplyAsync(() -> {
            if (offset instanceof Sequence) {
                final var seqOffset = (Sequence)offset;
                final var longOffset = seqOffset.value();
                PreparedStatement pstmt = null;
                try {
                    pstmt = conn.prepareStatement("UPDATE offsets SET offset=? WHERE read_side_id=?");
                    pstmt.setLong(1, longOffset);
                    pstmt.setString(2, readSideId);
                    pstmt.executeUpdate();

                    currentOffset.set(longOffset);

                    return longOffset;
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                System.out.println("UNKNOWN OFFSET: " + offset.toString());
            }
            return 0L;
        });
    }

    public CompletionStage<Long> latestOffset(String readSideId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement pstmt = conn.prepareStatement("SELECT offset FROM offsets WHERE read_side_id=?");
                pstmt.setString(1, readSideId);
                ResultSet result = pstmt.executeQuery();
                result.next();
                final long offset = result.getLong(1);
                this.currentOffset.set(offset);
                return offset;
            } catch (SQLException e) {
                PreparedStatement pstmt2 = null;
                try {
                    pstmt2 = conn.prepareStatement("INSERT INTO offsets(offset,read_side_id) VALUES(?,?)");
                    pstmt2.setLong(1, 0L);
                    pstmt2.setString(2, readSideId);
                    pstmt2.executeUpdate();
                    final long offset = 0L;
                    this.currentOffset.set(offset);
                    return offset;
                } catch (SQLException e2) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        });
    }

    public static OffsetStore init(String user, String password, String database, String host, int port) throws SQLException {

        final var conn = SQL.newConnection(user, password, database, host, port);
        conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS offsets ("
                + "read_side_id VARCHAR(36) PRIMARY KEY,"
                + "offset INTEGER DEFAULT 0"
                + ");"
        );
        return new OffsetStore(conn);
    }
}
