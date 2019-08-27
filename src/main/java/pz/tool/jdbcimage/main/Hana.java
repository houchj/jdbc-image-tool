package pz.tool.jdbcimage.main;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * DB facade for SAP HANA.
 */
public class Hana extends DBFacade {
    @Override
    public void setupDataSource(BasicDataSource bds) {
        // bds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        // bds.setConnectionInitSqls(Collections.singletonList("SET FOREIGN_KEY_CHECKS =
        // 0"));
    }

    @Override
    public List<String> getDbUserTables(Connection con) throws SQLException {
        List<String> retVal = new ArrayList<>();
        try (ResultSet tables = con.getMetaData().getTables(con.getCatalog(), con.getSchema(), "%",
                new String[] { "TABLE" })) {
            while (tables.next()) {
                String tableName = tables.getString(3);
                retVal.add(tableName);
            }
        }
        System.out.println("total table count: " + retVal.size());
        return retVal;
    }

    @Override
    public String escapeColumnName(String s) {
        return s;
    }

    @Override
    public String escapeTableName(String s) {
        return s;
    }

    /**
     * TODO: complete these 2 methods if the HANA DB uses constraints and indexes. Now it's only for DBs that don't use constraints and index.
     */
    @Override
    public void modifyConstraints(boolean enable) throws SQLException {
    	mainToolBase.out.println("This is only suitable for HANA DB that doesn't use constraints.");
    }

    @Override
    public void modifyIndexes(boolean enable) throws SQLException {
        mainToolBase.out.println("This is only suitable for HANA DB that doesn't use indexes.");
    }

    /**
     * TODO: this also only supports HANA DB that doesn't uses blob 
     */
    @Override
    public boolean canCreateBlobs() {
        return false;
    }

    @Override
    public void importStarted() {
        super.importStarted();
    }
}
