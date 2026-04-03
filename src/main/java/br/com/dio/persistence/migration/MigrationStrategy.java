package br.com.dio.persistence.migration;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.AllArgsConstructor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

@AllArgsConstructor
public class MigrationStrategy {

    private final Connection connection;

    public void executeMigration() throws SQLException {
        var originalOut = System.out;
        var originalErr = System.err;
        try (var fos = new FileOutputStream("liquibase.log")) {
            System.setOut(new PrintStream(fos));
            System.setErr(new PrintStream(fos));
            try (var jdbcConnection = new JdbcConnection(connection)) {
                var liquibase = new Liquibase(
                        "/db/changelog/db.changelog-master.yml",
                        new ClassLoaderResourceAccessor(),
                        jdbcConnection);
                liquibase.update();
            } catch (LiquibaseException e) {
                System.setErr(originalErr);
                throw new SQLException("Erro ao executar migrations do Liquibase", e);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Não foi possível abrir o arquivo de log do Liquibase", ex);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

}
