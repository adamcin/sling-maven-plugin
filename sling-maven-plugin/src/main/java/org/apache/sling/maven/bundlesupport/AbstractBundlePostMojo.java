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
package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractBundlePostMojo extends AbstractMojo {

    /**
     * The URL of the running Sling instance.
     *
     * <p>The default is only useful for <strong>WebConsole</strong> deployment.</p>
     *
     * <p>For <strong>WebDAV</strong> deployment it is recommended to include the Sling Simple WebDAV servlet root, for instance <a href="http://localhost:8080/dav/default/libs/sling/install">http://localhost:8080/dav/default/libs/sling/install</a>. Omitting the <tt>dav/default</tt> segment can lead to conflicts with other servlets.</p>
     */
    @Parameter(property="sling.url", defaultValue="http://localhost:8080/system/console", required = true)
    protected String slingUrl;

    /**
     * The WebConsole URL of the running Sling instance. This is required for file system provider operations.
     * If not configured the value of slingUrl is used.
     */
    @Parameter(property="sling.console.url")
    protected String slingConsoleUrl;
        
    /**
     * An optional url suffix which will be appended to the <code>sling.url</code>
     * for use as the real target url. This allows to configure different target URLs
     * in each POM, while using the same common <code>sling.url</code> in a parent
     * POM (eg. <code>sling.url=http://localhost:8080</code> and
     * <code>sling.urlSuffix=/project/specific/path</code>). This is typically used
     * in conjunction with WebDAV or SlingPostServlet deployment methods.
     */
    @Parameter(property="sling.urlSuffix")
    protected String slingUrlSuffix;
    
    /**
     * The user name to authenticate at the running Sling instance.
     */
    @Parameter(property="sling.user", defaultValue = "admin", required = true)
    private String user;

    /**
     * The password to authenticate at the running Sling instance.
     */
    @Parameter(property="sling.password", defaultValue = "admin", required = true)
    private String password;
    
    /**
     * Determines whether or not to fail the build if
     * the HTTP POST or PUT returns an non-OK response code.
     */
    @Parameter(property = "sling.failOnError", defaultValue = "true", required = true)
    protected boolean failOnError;

    /**
     * Returns the symbolic name of the given bundle. If the
     * <code>jarFile</code> does not contain a manifest with a
     * <code>Bundle-SymbolicName</code> header <code>null</code> is
     * returned. Otherwise the value of the <code>Bundle-SymbolicName</code>
     * header is returned.
     * <p>
     * This method may also be used to check whether the file is a bundle at all
     * as it is assumed, that only if the file contains an OSGi bundle will the
     * <code>Bundle-SymbolicName</code> manifest header be set.
     *
     * @param jarFile The file providing the bundle whose symbolic name is
     *            requested.
     * @return The bundle's symbolic name from the
     *         <code>Bundle-SymbolicName</code> manifest header or
     *         <code>null</code> if no manifest exists in the file or the
     *         header is not contained in the manifest. However, if
     *         <code>null</code> is returned, the file may be assumed to not
     *         contain an OSGi bundle.
     */
    protected String getBundleSymbolicName(File jarFile) {

        if (!jarFile.exists()) {
            return null;
        }

        JarFile jaf = null;
        try {
            jaf = new JarFile(jarFile);
            Manifest manif = jaf.getManifest();
            if (manif == null) {
                getLog().debug(
                    "getBundleSymbolicName: Missing manifest in " + jarFile);
                return null;
            }

            String symbName = manif.getMainAttributes().getValue(
                "Bundle-SymbolicName");
            if (symbName == null) {
                getLog().debug(
                    "getBundleSymbolicName: No Bundle-SymbolicName in "
                        + jarFile);
                return null;
            }

            return symbName;
        } catch (IOException ioe) {
            getLog().warn("getBundleSymbolicName: Problem checking " + jarFile,
                ioe);
        } finally {
            if (jaf != null) {
                try {
                    jaf.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
        }

        // fall back to not being a bundle
        return null;
    }

    /**
     * @return Returns the combination of <code>sling.url</code> and <code>sling.urlSuffix</code>.
     */
    protected String getTargetURL() {
        String targetURL = slingUrl;
        if (slingUrlSuffix != null) {
            targetURL += slingUrlSuffix;
        }
        return targetURL;
    }

    /**
     * @return Returns the combination of <code>sling.console.url</code> and <code>sling.urlSuffix</code>.
     */
    protected String getConsoleTargetURL() {
        String targetURL = StringUtils.defaultString(slingConsoleUrl, slingUrl);
        if (slingUrlSuffix != null) {
            targetURL += slingUrlSuffix;
        }
        return targetURL;
    }

    /**
     * @return Get the http client
     */
    protected HttpClient getHttpClient() {
        final HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(
            5000);

        // authentication stuff
        client.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials(user,
            password);
        client.getState().setCredentials(AuthScope.ANY, defaultcreds);

        return client;
    }

}
