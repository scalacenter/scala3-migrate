package migrate.interfaces;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import coursierapi.*;

public interface Migrate {
    String migrateVersion = "0.1.0-SNAPSHOT";
    ScalaVersion scalaVersion = ScalaVersion.of("2.13");

    MigrateService getService();

    static Migrate fetchAndClassloadInstance() throws Exception {
        List<URL> jars = getJars();
        
        ClassLoader parent = new MigrateClassloader(Migrate.class.getClassLoader());
        URLClassLoader classLoader = new URLClassLoader(jars.stream().toArray(URL[]::new), parent);
        
        return classloadInstance(classLoader);
    }
    
    static Migrate classloadInstance(URLClassLoader classLoader) throws Exception {
        Class<?> cls = classLoader.loadClass("migrate.interfaces.MigrateImpl");
        Constructor<?> ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return (Migrate) ctor.newInstance();
    }
    
    // put all needed dependecies here.
    static List<URL> getJars() throws Exception {
        Dependency migrate = Dependency.parse(
                "ch.epfl.scala:::migrate:" + migrateVersion,
                scalaVersion
        );
        return fetch(Collections.singletonList(migrate), ResolutionParams.create());
    }
    

    static List<URL> fetch(List<Dependency> dependencies, ResolutionParams resolutionParams) throws Exception {
        List<URL> jars = new ArrayList<>();
        List<File> files = Fetch.create()
                    .withDependencies(dependencies.stream().toArray(Dependency[]::new))
                    .withResolutionParams(resolutionParams)
                    .fetch();
        for (File file : files) {
            URL url = file.toURI().toURL();
            jars.add(url);
        }
        return jars;
    }
}
