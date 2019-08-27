# jdbcimage tool
Quickly exports/imports user schema's tables to/from a binary file using JDBC and Kryo. Supports Oracle, MSSQL, MySQL/MariaDB and PostgreSQL databases. 
Typically, a zip file is exported from one database to be imported to another database, possibly of a different type and vendor. 
The target database must already have schema (tables) created, the data are imported into an existing schema. 
The tool ignores missing tables and columns when importing the data.

## Quick Start 
1. Build the project using maven
   * mvn install
   * use the jdbcimage${version}.zip or jdbcimage${version}.zip
   * alternatively
      * chmod a+x jdbcimage
      * create a soft link to jdbcimage in a directory that is already in your PATH, such as:
        <code>ln -s "\`pwd\`/jdbcimage" "$HOME/.local/bin/jdbcimage"</code>
   * if you are using oracle database, copy its JDBC drivers to the lib directory 
2. Know JDBC connection settings to your database
   * *url* - JDBC connection URL 
   * *user* - database user 
   * *password* 
3. Export to a zip file
   * jdbcimage export -url=jdbc:mariadb://localhost:3306/qa -user=root -password=root mysql.zip
   * jdbcimage export -url=jdbc:postgresql://localhost:5432/inttests?currentSchema=qa -user=postgres -password=postres postgres.zip
   * jdbcimage export -url=jdbc:oracle:thin:@localhost:1521:XE -user=system -password=changeit oracle.zip
   * jdbcimage export -url=jdbc:sqlserver://localhost:1433;databaseName=XE -user=sa -password=changeit sqlserver.zip
   * jdbcimage export -url=jdbc:sap://localhost:30015?encrypt=true&validateCertificate=true&currentschema=ABC -user=SYSTEM -password=changeit hana.zip
4. Import from a zip file
   * BEWARE: !!!import deletes data from all tables contained in the imported zip file!!!
   * jdbcimage import -url=jdbc:mariadb://localhost:3306/qa -user=root -ignored_tables=schemaversion -password=root -ignored_tables=SCHEMAVERSION postgres.zip
   * jdbcimage import -url=jdbc:postgresql://localhost:5432/inttests?currentSchema=qa -user=postgres -password=postres -ignored_tables=schemaversion mysql.zip
   * jdbcimage -Xmx1024m import -url=jdbc:oracle:thin:@localhost:1521:XE -user=system -password=changeit -ignored_tables=SCHEMAVERSION mysql.zip
   * jdbcimage import -url=jdbc:sqlserver://localhost:1433;databaseName=XE -user=sa -password=changeit -ignored_tables=SCHEMAVERSION mysql.zip
5. Take a look at table data in a zip file
   * jdbcimage dump image.zip
      * prints out tables contained in the file, see next item
   * jdbcimage dump image.zip#passwd
      * prints out metadata and contents of _passwd_ table stored inside image.zip
   * jdbcimage dumpHeader image.zip#passwd
      * prints out columns, their types and stored row count of _passwd_ table stored inside image.zip
6. Perform adhoc queries, inserts or updates (more of them can be separated with lines containing just / )
   * jdbcimage exec -url="$DBURL" -user="$DBUSER" -password="$DBPASSWORD" -sql="select * from Users"
   * jdbcimage exec -url="$DBURL" -user="$DBUSER" -password="$DBPASSWORD" -sql="update Users set emailLocale='en' where emailLocale is null"
   * echo "select * from Users" | jdbcimage exec -url="$DBURL" -user="$DBUSER" -password="$DBPASSWORD"

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
memory is good enough. BLOB, CLOBs and lengthy columns still might require more heap memory depending on data
and JDBC driver in use, so you also might have to increase java heap memory and/or lower batch size used during 
data import, there are parameters in the scripts to do so.
1. The result files are binary encoded using Kryo and zipped to be small on file system.
1. The scripts accept several properties as arguments supplied as -property=value
   * -url=jdbc:mariadb://localhost:3306/qa - JDBC connection string 
   * -user=user 
   * -password=password 
   * -ignored_tables=a,b,c - used to ignore specific tables during import and export 
   * -tool_concurrency=7 - can be used to limit execution threads
   * -tool_builddir=/tmp/a - build directory used during import export to save/serve table files
   * -batch.size=100 - how many rows to wrap into a batch during table import

## Initializing the database after import
Once the data is imported, it might be necessary to execute additional SQL commands, this is realized using *-Dlisteners=* property/argument of the import tool.
  * -listeners=Dummy
     * only prints out what a listener reacts upon during import
     * more listeners can be specified as a comma separated list, such as  -listeners=Dummy,Dummy
  * -listeners=OracleRestartGlobalSequence -OracleRestartGlobalSequence.sequenceName=pricefxseq
     * this helps to restart database sequence that is used in Oracle to set up identity values in a *id* column in all tables
     * the sequence name is set using -OracleRestartGlobalSequence.sequenceName property
     * after all the data are imported, the sequence is dropped and created with the value that is one more than a max value of all imported id values, see [the code](src/main/java/pz/tool/jdbcimage/main/listener/OracleRestartGlobalSequenceListener.java) for more details.
  * more can be added using a custom implementation

## Missing pieces
* tests, review, better organization of shell scripts, error handling of invalid args
