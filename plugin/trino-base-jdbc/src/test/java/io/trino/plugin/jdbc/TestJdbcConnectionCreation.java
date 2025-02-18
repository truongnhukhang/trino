/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.jdbc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.trino.plugin.jdbc.credential.CredentialProvider;
import io.trino.plugin.jdbc.credential.EmptyCredentialProvider;
import io.trino.plugin.jdbc.mapping.IdentifierMapping;
import io.trino.testing.QueryRunner;
import org.h2.Driver;
import org.intellij.lang.annotations.Language;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Properties;

import static io.trino.plugin.jdbc.H2QueryRunner.createH2QueryRunner;
import static io.trino.plugin.jdbc.TestingH2JdbcModule.createH2ConnectionUrl;
import static io.trino.tpch.TpchTable.NATION;
import static io.trino.tpch.TpchTable.REGION;
import static java.util.Objects.requireNonNull;

@Test(singleThreaded = true) // inherited from BaseJdbcConnectionCreationTest
public class TestJdbcConnectionCreation
        extends BaseJdbcConnectionCreationTest
{
    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        String connectionUrl = createH2ConnectionUrl();
        DriverConnectionFactory delegate = new DriverConnectionFactory(new Driver(), connectionUrl, new Properties(), new EmptyCredentialProvider());
        this.connectionFactory = new ConnectionCountingConnectionFactory(delegate);
        return createH2QueryRunner(ImmutableList.of(NATION, REGION), ImmutableMap.of("connection-url", connectionUrl), new TestingConnectionH2Module(connectionFactory));
    }

    @Test(dataProvider = "testCases")
    public void testJdbcConnectionCreations(@Language("SQL") String query, int expectedJdbcConnectionsCount, Optional<String> errorMessage)
    {
        assertJdbcConnections(query, expectedJdbcConnectionsCount, errorMessage);
    }

    @DataProvider
    public Object[][] testCases()
    {
        return new Object[][] {
                {"SELECT * FROM nation LIMIT 1", 3, Optional.empty()},
                {"SELECT * FROM nation ORDER BY nationkey LIMIT 1", 3, Optional.empty()},
                {"SELECT * FROM nation WHERE nationkey = 1", 3, Optional.empty()},
                {"SELECT avg(nationkey) FROM nation", 3, Optional.empty()},
                {"SELECT * FROM nation, region", 6, Optional.empty()},
                {"SELECT * FROM nation n, region r WHERE n.regionkey = r.regionkey", 6, Optional.empty()},
                {"SELECT * FROM nation JOIN region USING(regionkey)", 6, Optional.empty()},
                {"SELECT * FROM information_schema.schemata", 1, Optional.empty()},
                {"SELECT * FROM information_schema.tables", 1, Optional.empty()},
                {"SELECT * FROM information_schema.columns", 5, Optional.empty()},
                {"SELECT * FROM nation", 3, Optional.empty()},
                {"CREATE TABLE copy_of_nation AS SELECT * FROM nation", 13, Optional.empty()},
                {"INSERT INTO copy_of_nation SELECT * FROM nation", 14, Optional.empty()},
                {"DELETE FROM copy_of_nation WHERE nationkey = 3", 3, Optional.empty()},
                {"UPDATE copy_of_nation SET name = 'POLAND' WHERE nationkey = 1", 2, Optional.of("This connector does not support updates")},
                {"MERGE INTO copy_of_nation n USING region r ON r.regionkey= n.regionkey WHEN MATCHED THEN DELETE", 2, Optional.of("This connector does not support merges")},
                {"DROP TABLE copy_of_nation", 3, Optional.empty()},
                {"SHOW SCHEMAS", 1, Optional.empty()},
                {"SHOW TABLES", 2, Optional.empty()},
                {"SHOW STATS FOR nation", 2, Optional.empty()},
        };
    }

    private static class TestingConnectionH2Module
            implements Module
    {
        private final ConnectionCountingConnectionFactory connectionCountingConnectionFactory;

        TestingConnectionH2Module(ConnectionCountingConnectionFactory connectionCountingConnectionFactory)
        {
            this.connectionCountingConnectionFactory = requireNonNull(connectionCountingConnectionFactory, "connectionCountingConnectionFactory is null");
        }

        @Override
        public void configure(Binder binder) {}

        @Provides
        @Singleton
        @ForBaseJdbc
        public static JdbcClient provideJdbcClient(BaseJdbcConfig config, ConnectionFactory connectionFactory, IdentifierMapping identifierMapping)
        {
            return new TestingH2JdbcClient(config, connectionFactory, identifierMapping);
        }

        @Provides
        @Singleton
        @ForBaseJdbc
        public ConnectionFactory getConnectionFactory(BaseJdbcConfig config, CredentialProvider credentialProvider)
        {
            return connectionCountingConnectionFactory;
        }
    }
}
