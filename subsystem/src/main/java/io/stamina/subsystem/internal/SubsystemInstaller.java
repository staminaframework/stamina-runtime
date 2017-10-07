/*
 * Copyright (c) 2017 Stamina developers.
 * All rights reserved.
 */

package io.stamina.subsystem.internal;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * OSGi Subsystem installer.
 *
 * @author Stamina developers
 */
@Component(service = ArtifactInstaller.class, immediate = true)
public class SubsystemInstaller implements ArtifactInstaller {
    @Reference
    private LogService logService;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() throws IOException {
        bundleContext = null;
    }

    @Override
    public void install(File artifact) throws Exception {
        final Manifest man = getManifest(artifact);
        final String sid = getSubsystemId(man);

        final String spath = artifact.getCanonicalFile().toURI().toURL().toExternalForm();
        final boolean alreadyInstalled = bundleContext.getBundle(spath) != null;
        if (alreadyInstalled) {
            logService.log(LogService.LOG_DEBUG, "Subsystem " + sid + " is already installed");
            return;
        }

        logService.log(LogService.LOG_INFO, "Installing subsystem: " + sid);
        final Subsystem sub = root.install(spath);
        logService.log(LogService.LOG_INFO, "Starting subsystem: " + sid);
        sub.start();
    }

    @Override
    public void update(File artifact) throws Exception {
        final Manifest man = getManifest(artifact);
        final String sid = getSubsystemId(man);
        logService.log(LogService.LOG_INFO, "Updating subsystem: " + sid);
        uninstall(artifact);
        install(artifact);
        logService.log(LogService.LOG_INFO, "Subsystem updated: " + sid);
    }

    @Override
    public void uninstall(File artifact) throws Exception {
        final String spath = artifact.getCanonicalFile().toURI().toURL().toExternalForm();
        // Subsystem is already installed: we need to uninstall it first.
        for (final Subsystem sub : root.getChildren()) {
            if (spath.equals(sub.getLocation())) {
                final String sid = getSubsystemId(sub);
                logService.log(LogService.LOG_INFO, "Uninstalling subsystem: " + sid);
                sub.uninstall();
                logService.log(LogService.LOG_INFO, "Subsystem uninstalled: " + sid);
            }
        }
    }

    @Override
    public boolean canHandle(File artifact) {
        if (!artifact.getName().toLowerCase().endsWith(".esa")) {
            return false;
        }
        try {
            final Manifest man = getManifest(artifact);
            final String ssn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
            if (ssn == null) {
                throw new IOException("Missing symbolic name in subsystem manifest");
            }
        } catch (IOException e) {
            logService.log(LogService.LOG_WARNING, "Failed to open file as a subsystem: " + artifact, e);
            return false;
        }
        return true;
    }

    private Manifest getManifest(File artifact) throws IOException {
        final Manifest man = new Manifest();
        try (final ZipFile zip = new ZipFile(artifact)) {
            final ZipEntry manEntry = zip.getEntry("OSGI-INF/SUBSYSTEM.MF");
            if (manEntry == null) {
                throw new IOException("Missing subsystem manifest");
            }
            try (final InputStream manIn = zip.getInputStream(manEntry)) {
                man.read(manIn);
            }
            final Attributes atts = man.getMainAttributes();
            final String ssn = atts.getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
            if (ssn == null) {
                throw new IOException("Missing symbolic name in subsystem manifest");
            }
        }
        return man;
    }

    private String getSubsystemId(Manifest man) {
        final String ssn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
        String svn = man.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
        if (svn == null) {
            svn = "0.0.0";
        }
        return ssn + "/" + svn;
    }

    private String getSubsystemId(Subsystem sub) {
        final String ssn = sub.getSymbolicName();
        final String svn = sub.getVersion().toString();
        return ssn + "/" + svn;
    }
}
