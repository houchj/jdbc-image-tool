# jdbc-image-tool
Quickly exports/imports user schema's tables to/from a binary file using JDBC and Kryo. Supports Oracle, MSSQL, MySQL/MariaDB and PostgreSQL databases. Typically, a zip file is exported from one database to be imported to another database, possibly  of a different type and vendor. The target database must have the tables defined, so that the data can be imported. The tool ignores missing tables and columns.

## Quick Start 
1. Build the project using maven
   * mvn install
   * chmod a+x bin/jdbcimage.sh
2. Know JDBC connection settings to your database
   * *url* - such as _jdbc:mariadb://localhost:3306/qa_, _jdbc:postgresql://localhost:5432/inttests?currentSchema=qa_, _jdbc:oracle:thin:@localhost:1521:XE_ 
   * *user* - root, postgress, system  
   * *password* 
3. Run export or import a zip file as the only argument
   * bin/jdbcimage.sh export -url=jdbc:mariadb://localhost:3306/qa -user=root -password=root image.zip
      * See more examples in [exportMariadb.sh](exportMariadb.sh), [exportPostgres.sh](exportPostgres.sh), [exportMssql.sh](exportMssql.sh) and [exportOracle.sh](exportOracle.sh)
   * bin/jdbcimage.sh import -url=jdbc:postgresql://localhost:5432/qa -user=me -password=pwd image.zip
      * BEWARE: !!!import deletes data from existing tables!!!
      * See more examples in [importMariadb.sh](importMariadb.sh), [importPostgres.sh](importPostgres.sh), [importMssql.sh](importMssql.sh) and [importOracle.sh](importOracle.sh)
   * bin/jdbcimage.sh dump image.zip
      * lists the tables contained in the file, see next item
   * bin/jdbcimage.sh dump image.zip#passwd
      * dumps metadata and contents of _passwd_ table stored inside image.zip
   * bin/jdbcimage.sh dumpHeader image.zip#passwd
      * dumps columns, their types and stored row count of _passwd_ table stored inside image.zip

## How it works
The key principles are:
1. More threads are used speed up data export/import, by default set to the number of (client) 
machine processors; see tool_concurrency parameter in the scripts.
1. The lowest transaction isolation level is used to make database export/import faster. 
1. Database metadata are used to export/import all user+schema tables to/from a zip file with entries 
per exported/imported table.
1. Table and column names are always case-insensitive internally, error is reported when there are more tables 
with the same case-insensitive name.
1. Concurrent execution requires an extra setup/teardown instructions during data import. 
These vary between database types, but they always include disabling/enabling foreign 
key constraints, see database classes defined in the [main package](src/main/java/pz/tool/jdbcimage/jdbcimage/main/) for more details.
   * All triggers are disabled on Oracle before data import and then enabled after data import.
   * Oracle sequences, when used, are out of scope and usually have to be reset manually after data import.
   * All foreign key constraints are dropped by the tool on Postgress before importing the data, but a table jdbcimage_create_constraints is created with rows that are used to recreate them after data import.  
   * Identity column sequences are reset to the lowest value after data import on Postgres.
1. Streams of data are used for both export and import to have the lowest memory footprint, typically 256M of heap 
memory is good enough. BLOB, CLOBs and lengthty columns still might require more heap memory depending on data 
and JDBC driver in use, so you also might have to increase java heap memory and/or lower batch size used during 
data import, there are parameters in the scripts to do so.
1. The result files and binary encoded using Kryo qnd zipped to be small. The files can be the easily dumped with information about table columns and their types.
1. The scripts accept several properties as arguments supplied as -property=value
   * -url=jdbc:mariadb://localhost:3306/qa - JDBC connection string 
   * -user=user 
   * -password=password 
   * -ignored_tables=a,b,c - used to ignore specific tables during import and export 
   * -tool_concurrency=7 - can be used to limit execution threads
   * -tool_builddir=/tmp/a - build directory used during import export to save/serve table files
   * -batch.size=100 - how many rows to wrap into a batch during table import

## Initializing the database after import
Once the data are imported, it might be necessary to execute additional SQL commands, this is realized using *-Dlisteners=* property/argument of the import tool.
  * -listeners=Dummy
     * only prints out what a listener reacts upon during import
     * more listeners can be specifed as a comma separated list, such as  -listeners=Dummy,Dummy
  * -listeners=OracleRestartGlobalSequence -OracleRestartGlobalSequence.sequenceName=pricefxseq
     * this helps to restart database sequence that is used in Oracle to set up identity values in a *id* column in all tables
     * the sequence name is set using -OracleRestartGlobalSequence.sequenceName property
     * after all the data are imported, the sequence is dropped and created with the value that is one more than a max value of all imported id values, see [the code](src/main/java/pz/tool/jdbcimage/main/listener/OracleRestartGlobalSequenceListener.java) for more details.
  * more can be added using a custom implementation

## Missing pieces
* tests, review, better organization of shell scripts, error handling of invalid args
* docker support to have a simple packacking of a reusable tool that can be operated with simple examples
* code refactoring to make the it easier to read