/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.security;

import org.mockito.Mockito;

import java.time.Clock;
import java.util.function.Supplier;

import org.neo4j.configuration.Config;
import org.neo4j.cypher.internal.security.SecureHasher;
import org.neo4j.dbms.database.DefaultSystemGraphInitializer;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.server.security.auth.AuthenticationStrategy;
import org.neo4j.server.security.auth.CommunitySecurityModule;
import org.neo4j.server.security.auth.RateLimitedAuthenticationStrategy;
import org.neo4j.server.security.auth.UserRepository;
import org.neo4j.server.security.systemgraph.BasicSystemGraphOperations;
import org.neo4j.server.security.systemgraph.BasicSystemGraphRealm;
import org.neo4j.server.security.systemgraph.UserSecurityGraphInitializer;
import org.neo4j.test.rule.TestDirectory;

import static org.mockito.Mockito.mock;
import static org.neo4j.cypher.security.BasicSystemGraphRealmTestHelper.TestDatabaseManager;
import static org.neo4j.dbms.DatabaseManagementSystemSettings.auth_store_directory;

public class TestBasicSystemGraphRealm
{
    protected static final SecureHasher secureHasher = new SecureHasher();

    static BasicSystemGraphRealm testRealm( BasicImportOptionsBuilder importOptions, TestDatabaseManager dbManager, Config config ) throws Throwable
    {
        return testRealm( importOptions.migrationSupplier(), importOptions.initialUserSupplier(), newRateLimitedAuthStrategy(), dbManager, config );
    }

    static BasicSystemGraphRealm testRealm( TestDatabaseManager dbManager, TestDirectory testDirectory, Config cfg ) throws Throwable
    {
        Config config = Config.newBuilder()
                .fromConfig( cfg )
                .set( auth_store_directory, testDirectory.directory( "data/dbms" ).toPath() )
                .build();
        LogProvider logProvider = mock(LogProvider.class);
        FileSystemAbstraction fileSystem = testDirectory.getFileSystem();

        Supplier<UserRepository> migrationUserRepositorySupplier = () -> CommunitySecurityModule.getUserRepository( config, logProvider, fileSystem );
        Supplier<UserRepository> initialUserRepositorySupplier = () -> CommunitySecurityModule.getInitialUserRepository( config, logProvider, fileSystem );
        return testRealm( migrationUserRepositorySupplier, initialUserRepositorySupplier, newRateLimitedAuthStrategy(), dbManager, config );
    }

    private static BasicSystemGraphRealm testRealm(
            Supplier<UserRepository> migrationSupplier,
            Supplier<UserRepository> initialUserSupplier,
            AuthenticationStrategy authStrategy,
            TestDatabaseManager manager,
            Config config ) throws Throwable
    {

        BasicSystemGraphOperations systemGraphOperations = new BasicSystemGraphOperations( manager, secureHasher );
        UserSecurityGraphInitializer securityGraphInitializer =
                new UserSecurityGraphInitializer(
                        manager,
                        new DefaultSystemGraphInitializer( manager, config ),
                        Mockito.mock(Log.class),
                        migrationSupplier,
                        initialUserSupplier,
                        secureHasher
                );

        BasicSystemGraphRealm realm = new BasicSystemGraphRealm(
                systemGraphOperations,
                securityGraphInitializer,
                authStrategy,
                true
        );
        realm.start();

        return realm;
    }

    protected static AuthenticationStrategy newRateLimitedAuthStrategy()
    {
        return new RateLimitedAuthenticationStrategy( Clock.systemUTC(), Config.defaults() );
    }
}
