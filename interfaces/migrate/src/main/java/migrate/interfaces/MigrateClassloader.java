package migrate.interfaces;

public class MigrateClassloader extends ClassLoader {
    private final ClassLoader parent;

    public MigrateClassloader(ClassLoader parent) {
        super(null);
        this.parent = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("migrate.interfaces")) {
            return parent.loadClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }
}
