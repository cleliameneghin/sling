/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.settings;

import java.net.URL;

/**
 * The <code>SlingSettingsService</code> provides basic Sling settings.
 *
 */
public interface SlingSettingsService {

    /**
     * The name of the framework property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, the
     * repository, etc., is located.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME_URL
     */
    String SLING_HOME = "sling.home";

    /**
     * The name of the framework property defining the Sling home directory as
     * an URL (value is "sling.home.url").
     * <p>
     * The value of this property is assigned the value of
     * <code>new File(${sling.home}).toURI().toString()</code> before
     * resolving the property variables.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME
     */
    String SLING_HOME_URL = "sling.home.url";

    /**
     * The identifier of the running Sling instance.
     */
    String getSlingId();

    /**
     * Returns the value of the {@link #SLING_HOME}
     * property.
     */
    String getSlingHomePath();

    /**
     * Returns the value of the {@link #SLING_HOME_URL}
     * property.
     */
    URL getSlingHome();
}
