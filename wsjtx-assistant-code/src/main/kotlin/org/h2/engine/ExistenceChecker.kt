package org.h2.engine

import org.slf4j.LoggerFactory

/**
 * Copyright (C) 2008-2017 Matt Gumbley, DevZendo.org http://devzendo.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A brutal hack - Database.exists is not public any more, but package-private.
 */
object ExistenceChecker {
    private val LOGGER = LoggerFactory.getLogger(ExistenceChecker::class.java)

    fun exists(path: String): Boolean {
        LOGGER.debug("Checking for existence of " + path)
        return Database.exists(path)
    }
}
