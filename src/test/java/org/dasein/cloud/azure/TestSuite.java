/**
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
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
 * ====================================================================
 */

package org.dasein.cloud.azure;

import org.apache.log4j.Logger;
import org.dasein.cloud.azure.compute.image.AzureImageTest;
import org.dasein.cloud.azure.compute.image.AzureLogger;
import org.dasein.cloud.test.GlobalTestSuite;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import mockit.Mock;
import mockit.MockUp;

@RunWith(Suite.class)
@Suite.SuiteClasses({AzureImageTest.class})
public final class TestSuite extends GlobalTestSuite{
	
	@BeforeClass
	public static void setupMocks() {
		new MockUp<Logger> () {
			@Mock
			public Logger getLogger(String name) {
				System.out.println("enter mock getLogger");
			    return Logger.getLogger(AzureLogger.class);
			}
				
			@Mock
			public void debug(Object msg) {
				System.out.println(msg);
			}
			
			@Mock
			public void info(Object msg) {
				System.out.println(msg);
			}

			@Mock
			public void warn(Object msg) {
				System.out.println(msg);
			}
			
			@Mock
			public void error(Object msg) {
				System.out.println(msg);
			}
			
			@Mock
			public void trace(Object msg) {
				System.out.println(msg);
			}
			
			@Mock
			public boolean isTraceEnabled() {
				return false;
			}
		};
	}
	
}
