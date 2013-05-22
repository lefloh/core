/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.manager;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.Addon;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * FIXME This test needs to be refactored to be a bit less brittle. It breaks when addon POMs change.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Ignore
@RunWith(Arquillian.class)
public class AddonManagerVersionRangeTest
{
   @Deployment
   @Dependencies({
            @Addon(name = "org.jboss.forge.addon:addon-manager", version = "2.0.0-SNAPSHOT"),
            @Addon(name = "org.jboss.forge.addon:maven", version = "2.0.0-SNAPSHOT")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge.addon:addon-manager", "2.0.0-SNAPSHOT"))
               );

      return archive;
   }

   @Inject
   private AddonRegistry registry;

   @Inject
   private AddonManager addonManager;

   @Inject
   private AddonRepository repository;

   @Test
   public void testInstallingAddonWithSingleOptionalAddonDependency() throws InterruptedException
   {
      int addonCount = registry.getAddons().size();
      AddonId example = AddonId.fromCoordinates("org.jboss.forge.addon:example3,2.0.0-SNAPSHOT");
      InstallRequest request = addonManager.install(example);

      Assert.assertEquals(0, request.getRequiredAddons().size());
      Assert.assertEquals(1, request.getOptionalAddons().size());

      request.perform();

      Assert.assertTrue(repository.isEnabled(example));
      Assert.assertEquals(2, repository.getAddonResources(example).size());
      Assert.assertTrue(repository.getAddonResources(example).contains(
               new File(repository.getAddonBaseDir(example), "commons-lang-2.6.jar")));
      Assert.assertTrue(repository.getAddonResources(example).contains(
               new File(repository.getAddonBaseDir(example), "example-2.0.0-SNAPSHOT-forge-addon.jar")));

      Set<AddonDependencyEntry> dependencies = repository.getAddonDependencies(example);
      Assert.assertEquals(1, dependencies.size());
      AddonDependencyEntry dependency = dependencies.toArray(new AddonDependencyEntry[dependencies.size()])[0];
      Assert.assertEquals("org.jboss.forge.addon:example2", dependency
               .getId().getName());
      Assert.assertEquals(new SingleVersion("2.0.0-SNAPSHOT"), dependency
               .getId().getVersion());
      Assert.assertTrue(dependency.isOptional());
      Assert.assertFalse(dependency.isExported());

      Assert.assertTrue(registry.getAddon(AddonId.from("org.jboss.forge.addon:example2", "2.0.0-SNAPSHOT"))
               .getStatus().isMissing());

      Addons.waitUntilStarted(registry.getAddon(example), 10, TimeUnit.SECONDS);
      Assert.assertEquals(addonCount + 2, registry.getAddons().size());
   }

}