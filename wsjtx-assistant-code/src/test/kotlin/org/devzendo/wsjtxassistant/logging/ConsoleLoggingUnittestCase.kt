package org.devzendo.wsjtxassistant.logging

import org.apache.log4j.BasicConfigurator
import org.junit.After
import org.junit.Before

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
abstract class ConsoleLoggingUnittestCase {

    // Intended to be callable from @BeforeClass when logging is needed
    fun setupLoggingStatically() {
        BasicConfigurator.resetConfiguration()
        BasicConfigurator.configure()
    }

    @Before
    fun setupLogging() {
        setupLoggingStatically()
    }

    @After
    fun teardownLogging() {
        BasicConfigurator.resetConfiguration()
    }
}