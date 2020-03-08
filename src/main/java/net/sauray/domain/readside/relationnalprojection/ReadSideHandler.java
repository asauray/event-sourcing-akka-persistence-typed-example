package net.sauray.domain.readside.relationnalprojection;

import net.sauray.domain.events.BankEvent;
import net.sauray.domain.events.MoneyAddedToAccount;
import net.sauray.domain.events.MoneyRemovedFromAccount;
import net.sauray.infrastructure.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ReadSideHandler {

    private Connection conn;
    private static final Logger logger = Logger.getAnonymousLogger();

    private ReadSideHandler(Connection conn) {
        this.conn = conn;
    }

    private void handleEvent(String entityId, MoneyAddedToAccount event) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO test_projection(account_id, value) VALUES(?, ?) ON DUPLICATE KEY UPDATE value=?;");
        pstmt.setString(1, entityId);
        pstmt.setLong(2, event.getAmountCents());
        pstmt.setLong(3, event.getAmountCents());
        pstmt.executeUpdate();
    }

    private void handleEvent(String entityId, MoneyRemovedFromAccount event) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO test_projection(account_id, value) VALUES(?, ?) ON DUPLICATE KEY UPDATE value=?");
        pstmt.setString(1, entityId);
        pstmt.setLong(2, event.getAmountCents());
        pstmt.setLong(3, event.getAmountCents());
        pstmt.executeUpdate();
    }

    public void handleEvent(String entityId, BankEvent event) throws SQLException {

        if(event instanceof MoneyAddedToAccount) {
            handleEvent(entityId, (MoneyAddedToAccount) event);
        } else if(event instanceof MoneyRemovedFromAccount) {
            handleEvent(entityId, (MoneyRemovedFromAccount) event);
        } else {
            logger.warning("Unhandled event in readside");
        }

    }

    public static ReadSideHandler init(String user, String password, String database, String host, int port) throws SQLException {

        final var conn = SQL.newConnection(user, password, database, host, port);
        conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS test_projection ("
                + "account_id VARCHAR(36) PRIMARY KEY,"
                + "value INTEGER DEFAULT 0"
                + ");"
        );
        return new ReadSideHandler(conn);
    }
}
