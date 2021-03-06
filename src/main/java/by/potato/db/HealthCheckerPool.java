package by.potato.db;

import oracle.ucp.jdbc.JDBCConnectionPoolStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Health checker for database connection pool
 * <p>
 * Version: 1.0
 * <p>
 * Copyright: OOO Aplana
 *
 * @author Maksim Stepanov
 */
class HealthCheckerPool implements Runnable {

    private static final Logger logger = LogManager
            .getFormatterLogger(HealthCheckerPool.class.getSimpleName());
    private final String poolName;
    private JDBCConnectionPoolStatistics stat;

    public HealthCheckerPool(JDBCConnectionPoolStatistics stat, String poolName) {
        logger.info("Start pool statistics");
        this.stat = stat;
        this.poolName = poolName;
    }

    @Override
    public void run() {

        logger.info("===== Pool statistics =====");
        logger.info("[%s] Total connection count: %d", this.poolName,
                this.stat.getTotalConnectionsCount());
        logger.info("[%s] Borrowed connection count: %d", this.poolName,
                this.stat.getBorrowedConnectionsCount());
        logger.info("[%s] Available connection count: %d", this.poolName,
                this.stat.getAvailableConnectionsCount());
        logger.info("[%s] Peak connection count: %d", this.poolName,
                this.stat.getPeakConnectionsCount());
        logger.info("[%s] Average connection wait time: %d", this.poolName,
                this.stat.getAverageConnectionWaitTime());

    }

}