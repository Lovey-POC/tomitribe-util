/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tomitribe.util.dir;

import org.junit.Test;
import org.tomitribe.util.Files;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReturnFilteredStreamTest {
    @Test
    public void testArray() throws Exception {
        final File dir = Files.tmpdir();

        final List<String> strings = Arrays.asList("red", "green", "blue");
        for (final String submodule : strings) {
            Files.mkdirs(dir, submodule, "src", "main", "java");
            Files.mkdirs(dir, submodule, "src", "main", "resources");
            Files.mkdirs(dir, submodule, "src", "test", "java");
            Files.mkdirs(dir, submodule, "src", "test", "resources");
            Files.mkdirs(dir, submodule, "target");
            Files.file(dir, submodule, "pom.xml").createNewFile();
        }

        Files.file(dir, "pom.xml").createNewFile();

        final Module parent = Dir.of(Module.class, dir);

        final Stream<Module> modules = parent.modules();

        final AtomicInteger count = new AtomicInteger(0);
        modules.forEach(module -> {
            final File moduleDir = module.dir();
            System.out.println("FILE " + moduleDir.getAbsolutePath());
            assertFiles(dir, moduleDir.getName(), moduleDir);

            assertFiles(moduleDir, "src/main/java", module.src().main().java());
            assertFiles(moduleDir, "src/main/resources", module.src().main().resources());
            assertFiles(moduleDir, "src/test/java", module.src().test().java());
            assertFiles(moduleDir, "src/test/resources", module.src().test().resources());
            assertFiles(moduleDir, "pom.xml", module.pomXml());
            assertTrue(module.pomXml().exists());

            count.incrementAndGet();
        });

        // The pom.xml is NOT listed
        assertEquals(strings.size(), count.get());
    }

    private void assertFiles(final File dir, final String expectedPath, final File actual) {
        final File expected = new File(dir, expectedPath);
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    public interface Module extends Dir {

        @Name("pom.xml")
        File pomXml();

        @Filter(HasPomXml.class)
        Stream<Module> modules();

        Src src();
    }

    public interface Src {
        Section main();

        Section test();
    }

    public interface Section {
        File java();

        File resources();
    }

    public static class HasPomXml implements FileFilter {
        @Override
        public boolean accept(final File pathname) {
            final File pom = new File(pathname, "pom.xml");
            return pom.exists();
        }
    }

}