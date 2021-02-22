/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
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
package io.helidon.dbclient.blocking;

import java.util.Optional;

import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbStatement;
import io.helidon.dbclient.DbStatementGet;


/**
 * Database statement that queries the database and returns a single row if present, or an empty optional.
 * In case the statement returns more than one rows, the future returned by {@link #execute()} will end in
 * {@link RuntimeException}.
 */
public interface BlockingDbStatementGet extends DbStatement<BlockingDbStatementGet, Optional<DbRow>> {

    /**
     * Create Blocking DbStatementGet wrapper.
     *
     * @param dbStatementGet input DbStatementDml
     * @return Blocking DbStatementGet
     */
    static BlockingDbStatementGet create(DbStatementGet dbStatementGet){
        return new BlockingDbStatementGetImpl(dbStatementGet);
    }
}
