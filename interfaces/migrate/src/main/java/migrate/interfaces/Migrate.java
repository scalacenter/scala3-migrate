package migrate.interfaces;

import coursierapi.Dependency;
import coursierapi.Fetch;
import coursierapi.ResolutionParams;
import coursierapi.ScalaVersion;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;

public interface Migrate {

    void migrate(List<Path> unmanagedSources, 
                 List<Path> managedSources,
                 Path targetRoot,
                 List<Path> scala2Classpath,
                 List<String> scala2CompilerOptions,
                 List<Path> scala3Classpath,
                 List<String> scala3CompilerOptions,
                 Path scala3ClassDirectory);
    
    MigratedScalacOptions migrateScalacOption(List<String> scala3CompilerOptions);
    MigratedLibs migrateLibs(List<Lib> libs);

    void migrateSyntax(List<Path> unmanagedSources,
                       Path targetRoot,
                       List<Path> scala2Classpath,
                       List<String> scala2CompilerOptions);


    // Todo: Maybe using ServiceLoader could simplify this code a bit:
    // https://www.baeldung.com/java-spi
    static Migrate fetchAndClassloadInstance(String migrateVersion, String scalaVersion, Logger logger) throws Exception {
        List<URL> jars = getJars(migrateVersion, scalaVersion);

        ClassLoader parent = new MigrateClassloader(Migrate.class.getClassLoader());
        URLClassLoader classLoader = new URLClassLoader(jars.stream().toArray(URL[]::new), parent);

        return classloadInstance(classLoader, logger);
    }

    static Migrate classloadInstance(URLClassLoader classLoader, Logger logger) throws Exception {
        Class<?> loggerCls = classLoader.loadClass("migrate.interfaces.Logger");
        Class<?> cls = classLoader.loadClass("migrate.interfaces.MigrateImpl");
        Constructor<?> ctor = cls.getDeclaredConstructor(loggerCls);
        ctor.setAccessible(true);
        return (Migrate) ctor.newInstance(logger);
    }

    // put all needed dependecies here.
    static List<URL> getJars(String migrateVersion, String scalaVersion) throws Exception {
        ScalaVersion scalaV = ScalaVersion.of(scalaVersion);
        Dependency migrate = Dependency.parse("ch.epfl.scala:::migrate-core:" + migrateVersion, scalaV);
        return fetch(Collections.singletonList(migrate), ResolutionParams.create());
    }

    static List<URL> fetch(List<Dependency> dependencies, ResolutionParams resolutionParams) throws Exception {
        List<URL> jars = new ArrayList<>();
        List<File> files = Fetch.create().withDependencies(dependencies.stream().toArray(Dependency[]::new))
                .withResolutionParams(resolutionParams).fetch();
        for (File file : files) {
            URL url = file.toURI().toURL();
            jars.add(url);
        }
        return jars;
    }
}
