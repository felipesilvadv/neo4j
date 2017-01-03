/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.neo4j.kernel.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.neo4j.storageengine.api.lock.ResourceType;
import org.neo4j.time.SystemNanoClock;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Internal representation of the status of an executing query.
 *
 * This is used for inspecting the state of a query.
 *
 * @see ExecutingQuery#status
 */
abstract class ExecutingQueryStatus
{
    abstract long waitTimeNanos( SystemNanoClock clock );

    abstract Map<String,Object> toMap( SystemNanoClock clock );

    private static final Map<String,Object> RUNNING_STATE = unmodifiableMap( singletonMap( "state", "RUNNING" ) );
    static final ExecutingQueryStatus RUNNING = new ExecutingQueryStatus()
    {
        @Override
        long waitTimeNanos( SystemNanoClock clock )
        {
            return 0;
        }

        @Override
        Map<String,Object> toMap( SystemNanoClock clock )
        {
            return RUNNING_STATE;
        }
    };

    static class WaitingOnLock extends ExecutingQueryStatus
    {
        private final ResourceType resourceType;
        private final long[] resourceIds;
        private final long startTimeNanos;

        WaitingOnLock( ResourceType resourceType, long[] resourceIds, long startTimeNanos )
        {
            this.resourceType = resourceType;
            this.resourceIds = resourceIds;
            this.startTimeNanos = startTimeNanos;
        }

        @Override
        long waitTimeNanos( SystemNanoClock clock )
        {
            return clock.nanos() - startTimeNanos;
        }

        @Override
        Map<String,Object> toMap( SystemNanoClock clock )
        {
            Map<String,Object> map = new HashMap<>();
            map.put( "state", "WAITING" );
            map.put( "time", TimeUnit.NANOSECONDS.toMillis( waitTimeNanos( clock ) ) );
            map.put( "resourceType", resourceType.toString() );
            map.put( "resourceIds", resourceIds );
            return map;
        }
    }
}
