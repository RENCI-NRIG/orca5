package orca.util.persistence;

import java.util.Set;

import orca.util.OrcaException;
import orca.util.ReflectionUtils;

import org.reflections.Reflections;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * A simple persistence validation utility. It supports discovering <code>Persistable</code> classes and ensuring that
 * the classes are properly annotated. The intent is to run this tool as part of the build process to catch persistence
 * problems during build instead of at runtime. Note: a successful execution of the tool does not guarantee that the
 * code is free of persistence-related bugs. This tool should be considered as a "meets-min" validator.
 * 
 * @author aydan
 */
public class Validator {
    private static class ValidatorOptions {
        @Parameter(names = "--help", help = true, description = "displays this help message")
        private boolean help = false;

        @Parameter(names = "--list", description = "lists all orca Persistable classes")
        private boolean list = false;

        @Parameter(names = "--validate", description = "validates all Persistable classes")
        private boolean validate = false;
    }

    private ValidatorOptions options;

    public Validator(ValidatorOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null");
        }
        this.options = options;
    }

    private void list() {
        Reflections reflections = new Reflections("orca.");
        Set<Class<? extends Persistable>> subTypes = reflections.getSubTypesOf(Persistable.class);
        for (Class<? extends Persistable> c : subTypes) {
            System.err.println(c.getName() + " is Persistable");
        }
    }

    private void validate(Class<? extends Persistable> c) throws PersistenceException {
        System.out.println("Validating class: " + c.getName());

        // Attempt to resolve the class. This will ensure
        // that all fields are properly annotated and supported.
        PersistenceUtils.resolve(c);

        // Classes must also provide an accessible default constructor
        if (!c.isInterface()) {
            if (!ReflectionUtils.hasDefaultConstructor(c)) {
                throw new PersistenceException(
                        "Class " + c.getName() + " does not provide a default (parameter-less) constructor");
            }
        }
    }

    private void validate() throws PersistenceException {
        // get all classes/interfaces that derive from Persistable and validate them
        Reflections reflections = new Reflections("orca.");
        Set<Class<? extends Persistable>> subTypes = reflections.getSubTypesOf(Persistable.class);
        for (Class<? extends Persistable> c : subTypes) {
            validate(c);
        }
        System.out.println("Successfully validated " + subTypes.size() + " persistable classes");
    }

    public boolean run() throws OrcaException {
        if (options.list) {
            list();
            return true;
        }

        if (options.validate) {
            validate();
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws OrcaException {
        ValidatorOptions opts = new ValidatorOptions();
        JCommander jc = new JCommander(opts, args);
        if (opts.help) {
            jc.usage();
            System.exit(1);
        }

        Validator v = new Validator(opts);
        if (!v.run()) {
            jc.usage();
            System.exit(1);
        }
    }
}