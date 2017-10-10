/*
 * Copyright (c) 2017 Stamina.io developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.stamina.it;

import io.stamina.boot.helper.CommandLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.stamina.starter.it.OsgiHelper.lookupBundle;
import static io.stamina.starter.it.OsgiHelper.lookupService;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Integration tests for bundle <code>io.stamina.boot.helper</code>.
 *
 * @author Stamina.io developers
 */
@RunWith(PaxExam.class)
public class BootHelperTest {
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return CoreOptions.options(
                mavenBundle("io.stamina", "io.stamina.starter.it").versionAsInProject(),
                mavenBundle("io.stamina", "io.stamina.boot.helper").versionAsInProject().noStart(),
                mavenBundle("org.apache.felix", "org.apache.felix.log").versionAsInProject(),
                mavenBundle("org.everit.osgi", "org.everit.osgi.loglistener.slf4j").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),
                mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.region").versionAsInProject(),
                junitBundles()
        );
    }

    @Test
    public void testRegionDigraphWrite() throws BundleException {
        final Bundle regionBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        final File digraphFile = regionBundle.getDataFile("digraph");
        assertFalse(digraphFile.exists());
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();
        assertTrue(digraphFile.exists());
    }

    @Test
    public void testCommandLine() throws IOException, BundleException {
        final Bundle bootHelperBundle = lookupBundle(bundleContext, "io.stamina.boot.helper");
        final File cmdFile = bootHelperBundle.getDataFile("cmd.dat");
        try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(cmdFile))) {
            out.writeUTF("hello");
            out.writeInt(2);
            out.writeUTF("mr");
            out.writeUTF("bond");
        }
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();

        final CommandLine cmd = lookupService(bundleContext, CommandLine.class);
        assertEquals("hello", cmd.command());
        assertArrayEquals(new String[]{"mr", "bond"}, cmd.arguments());
    }

    @Test
    public void testNoCommandLine() throws BundleException {
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();
        assertNull(bundleContext.getServiceReference(CommandLine.class));
    }
}
