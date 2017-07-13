// Copyright 2015 Georg-August-Universität Göttingen, Germany
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package de.ugoe.cs.cpdp.eval;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import de.ugoe.cs.util.console.Console;

/**
 * <p>
 * Implements a storage of experiment results in a MySQL database.
 * </p>
 * 
 * @author Steffen Herbold
 */
public class MySQLResultStorage implements IResultStorage {

    /**
     * Name of the table where the results are stored.
     */
    final String resultsTableName;

    /**
     * Connection pool for the data base.
     */
    private MysqlDataSource connectionPool = null;

    /**
     * <p>
     * Creates a MySQLResultStorage with the default parameter file mysql.cred from the working
     * directory.
     * </p>
     * 
     * @see #MySQLResultStorage(String)
     */
    public MySQLResultStorage() {
        this("mysql.cred");
    }

    /**
     * <p>
     * Creates a new results storage. Tries to read a properties file located in the working
     * directory. If this file is not found or any parameter is not defined, the following default
     * values are used:
     * <ul>
     * <li>db.host = localhost</li>
     * <li>db.port = 3306</li>
     * <li>db.name = crosspare</li>
     * <li>db.user = crosspare</li>
     * <li>db.pass = crosspare</li>
     * <li>db.results.tablename = results</li>
     * <li>db.results.createtable = false</li>
     * </p>
     * 
     * @param parameterFile
     *            name of the parameter file
     */
    public MySQLResultStorage(String parameterFile) {
        Properties dbProperties = new Properties();
        try(FileInputStream is = new FileInputStream(parameterFile);) {
            dbProperties.load(is);
        }
        catch (IOException e) {
            Console.printerr("Could not load mysql.cred file: " + e.getMessage());
            Console.printerr("Must be a properties file located in working directory.");
            Console
                .traceln(Level.WARNING,
                         "Using default DB configuration since mysql.cred file could not be loaded");
        }
        String dbHost = dbProperties.getProperty("db.host", "localhost");
        String dbPort = dbProperties.getProperty("db.port", "3306");
        String dbName = dbProperties.getProperty("db.name", "crosspare");
        String dbUser = dbProperties.getProperty("db.user", "crosspare");
        String dbPass = dbProperties.getProperty("db.pass", "crosspare");
        this.resultsTableName = dbProperties.getProperty("db.results.tablename", "results");
        boolean createTableIfNotExists =
            Boolean.parseBoolean(dbProperties.getProperty("db.results.createtable", "false"));
        connectToDB(dbHost, dbPort, dbName, dbUser, dbPass);

        // create the results table if required
        if (!doesResultsTableExist() && createTableIfNotExists) {
            createResultsTable();
        }
    }

    /**
     * <p>
     * Sets up the database connection
     * </p>
     *
     * @param dbHost
     *            host of the database
     * @param dbPort
     *            port of the database
     * @param dbName
     *            name of the database
     * @param dbUser
     *            user of the database
     * @param dbPass
     *            password of the user
     */
    private void connectToDB(String dbHost,
                             String dbPort,
                             String dbName,
                             String dbUser,
                             String dbPass)
    {
        this.connectionPool = new MysqlDataSource();
        this.connectionPool.setUser(dbUser);
        this.connectionPool.setPassword(dbPass);
        this.connectionPool.setUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ugoe.cs.cpdp.eval.IResultStorage#addResult(de.ugoe.cs.cpdp.eval.ExperimentResult)
     */
    @Override
    public void addResult(ExperimentResult result) {
        StringBuilder preparedSql = new StringBuilder();
        preparedSql.append("INSERT INTO " + this.resultsTableName + " (");
        preparedSql.append("`configurationName`,");
        preparedSql.append("`productName`,");
        preparedSql.append("`classifier`,");
        preparedSql.append("`testsize`,");
        preparedSql.append("`trainsize`,");
        preparedSql.append("`error`,");
        preparedSql.append("`recall`,");
        preparedSql.append("`precision`,");
        preparedSql.append("`fscore`,");
        preparedSql.append("`gscore`,");
        preparedSql.append("`mcc`,");
        preparedSql.append("`balance`,");
        preparedSql.append("`auc`,");
        preparedSql.append("`aucec`,");
        preparedSql.append("`nofb20`,");
        preparedSql.append("`relb20`,");
        preparedSql.append("`nofi80`,");
        preparedSql.append("`reli80`,");
        preparedSql.append("`rele80`,");
        preparedSql.append("`necm15`,");
        preparedSql.append("`necm20`,");
        preparedSql.append("`necm25`,");
        preparedSql.append("`tpr`,");
        preparedSql.append("`tnr`,");
        preparedSql.append("`fpr`,");
        preparedSql.append("`fnr`,");
        preparedSql.append("`tp`,");
        preparedSql.append("`fn`,");
        preparedSql.append("`tn`,");
        preparedSql.append("`fp`) VALUES ");
        preparedSql.append("(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

        try(PreparedStatement stmt = this.connectionPool.getConnection().prepareStatement(preparedSql.toString());) {
            stmt.setString(1, result.getConfigurationName());
            stmt.setString(2, result.getProductName());
            stmt.setString(3, result.getClassifier());
            stmt.setInt(4, result.getSizeTestData());
            stmt.setInt(5, result.getSizeTrainingData());
            stmt.setDouble(6, result.getError());
            stmt.setDouble(7, result.getRecall());
            stmt.setDouble(8, result.getPrecision());
            stmt.setDouble(9, result.getFscore());
            stmt.setDouble(10, result.getGscore());
            stmt.setDouble(11, result.getMcc());
            stmt.setDouble(12, result.getAuc());
            stmt.setDouble(13, result.getBalance());
            stmt.setDouble(14, result.getAucec());
            stmt.setDouble(15, result.getNofb20());
            stmt.setDouble(16, result.getRelb20());
            stmt.setDouble(17, result.getNofi80());
            stmt.setDouble(18, result.getReli80());
            stmt.setDouble(19, result.getRele80());
            stmt.setDouble(20, result.getNecm15());
            stmt.setDouble(21, result.getNecm20());
            stmt.setDouble(22, result.getNecm25());
            stmt.setDouble(23, result.getTpr());
            stmt.setDouble(24, result.getTnr());
            stmt.setDouble(25, result.getFpr());
            stmt.setDouble(26, result.getFnr());
            stmt.setDouble(27, result.getTp());
            stmt.setDouble(28, result.getFn());
            stmt.setDouble(29, result.getTn());
            stmt.setDouble(30, result.getFp());

            int qryResult = stmt.executeUpdate();
            if (qryResult < 1) {
                Console.printerr("Insert failed.");
            }
        }
        catch (SQLException e) {
            Console.printerr("Problem with MySQL connection: ");
            Console.printerr("SQLException: " + e.getMessage());
            Console.printerr("SQLState: " + e.getSQLState());
            Console.printerr("VendorError: " + e.getErrorCode());
            return;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.ugoe.cs.cpdp.eval.IResultStorage#containsResult(java.lang.String, java.lang.String)
     */
    @Override
    public int containsResult(String experimentName, String productName, String classifierName) {
        String preparedSql = "SELECT COUNT(*) as cnt FROM " + this.resultsTableName +
            " WHERE configurationName=? AND productName=? AND classifier=?";
        try(PreparedStatement stmt = this.connectionPool.getConnection().prepareStatement(preparedSql);) {
            stmt.setString(1, experimentName);
            stmt.setString(2, productName);
            stmt.setString(3, classifierName);
            try(ResultSet results = stmt.executeQuery();) {
                results.next();
                return results.getInt("cnt");
            }
        }
        catch (SQLException e) {
            Console.printerr("Problem with MySQL connection: \n");
            Console.printerr("SQLException: " + e.getMessage() + "\n");
            Console.printerr("SQLState: " + e.getSQLState() + "\n");
            Console.printerr("VendorError: " + e.getErrorCode() + "\n");
            return 0;
        }
    }

    /**
     * <p>
     * Checks if the results table exists.
     * </p>
     *
     * @return true if exists, false otherwise
     */
    public boolean doesResultsTableExist() {
        boolean exists = false;
        try {
            DatabaseMetaData meta = this.connectionPool.getConnection().getMetaData();
            try(ResultSet res = meta.getTables(null, null, this.resultsTableName, null);) {
                exists = res.next();
            }
        }
        catch (SQLException e) {
            Console.printerr("Problem with MySQL connection: \n");
            Console.printerr("SQLException: " + e.getMessage() + "\n");
            Console.printerr("SQLState: " + e.getSQLState() + "\n");
            Console.printerr("VendorError: " + e.getErrorCode() + "\n");
        }
        return exists;
    }

    /**
     * <p>
     * Tries to create the results table in the DB.
     * </p>
     */
    public void createResultsTable() {
        String sql = "CREATE TABLE `" + this.resultsTableName + "` (" +
            "`idresults` int(11) NOT NULL AUTO_INCREMENT," +
            "`configurationName` varchar(45) NOT NULL," + "`productName` varchar(45) NOT NULL," +
            "`classifier` varchar(45) NOT NULL," + "`testsize` int(11) DEFAULT NULL," +
            "`trainsize` int(11) DEFAULT NULL," + "`error` double DEFAULT NULL," +
            "`recall` double DEFAULT NULL," + "`precision` double DEFAULT NULL," +
            "`fscore` double DEFAULT NULL," + "`gscore` double DEFAULT NULL," +
            "`mcc` double DEFAULT NULL," + "`auc` double DEFAULT NULL," +
            "`balance` double DEFAULT NULL," + 
            "`aucec` double DEFAULT NULL," + "`nofb20` double DEFAULT NULL," +
            "`relb20` double DEFAULT NULL," + "`nofi80` double DEFAULT NULL," +  
            "`reli80` double DEFAULT NULL," + "`rele80` double DEFAULT NULL," +  
            "`necm15` double DEFAULT NULL," + "`necm20` double DEFAULT NULL," +  
            "`necm25` double DEFAULT NULL," + "`tpr` double DEFAULT NULL," +
            "`tnr` double DEFAULT NULL," + "`fpr` double DEFAULT NULL," +
            "`fnr` double DEFAULT NULL," + "`tp` double DEFAULT NULL," +
            "`fn` double DEFAULT NULL," + "`tn` double DEFAULT NULL," +
            "`fp` double DEFAULT NULL," + "PRIMARY KEY (`idresults`)" +
            ") ENGINE=InnoDB AUTO_INCREMENT=77777 DEFAULT CHARSET=utf8;";
        try(Statement stmt = this.connectionPool.getConnection().createStatement();) { 
            stmt.execute(sql);
            Console.traceln(Level.FINE, "Created new table " + this.resultsTableName);
        }
        catch (SQLException e) {
            Console.printerr("Problem with MySQL connection: \n");
            Console.printerr("SQLException: " + e.getMessage() + "\n");
            Console.printerr("SQLState: " + e.getSQLState() + "\n");
            Console.printerr("VendorError: " + e.getErrorCode() + "\n");
        }
    }

    @Override
    public int containsHeterogeneousResult(String experimentName,
                                           String productName,
                                           String classifierName,
                                           String trainProductName)
    {
        // TODO dummy implementation
        return 0;
    }

}