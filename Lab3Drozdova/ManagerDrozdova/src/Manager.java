import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Logger;


public class Manager {
    private final Logger LOG;
    private static final String[] managerSyntax = ManagerSyntax.managerTokens;
    private RC error;
    private final String[] configs;
    private String[] workersClass;
    private String[] workersConfigs;

    public Manager(Logger LOG, String config) {
        this.LOG = LOG;
        ConfigParserManager cfg = new ConfigParserManager(new ManagerGrammar(managerSyntax), LOG);
        configs = cfg.parser(config);
        error = cfg.getError();
    }

    RC setConfig(String config) {
        int countWorkers = Integer.parseInt(configs[Arrays.asList(managerSyntax).indexOf(ManagerSyntax.countWorker)]);
        String[] workers = new String[countWorkers];
        String[] configs = new String[countWorkers];
        for(int i = 0; i < countWorkers; i++) {
            workers[i] = ManagerSyntax.worker + (i + 1);
            configs[i] = ManagerSyntax.config + (i + 1);
        }
        ConfigParserManager configWorker = new ConfigParserManager(new ManagerGrammar(workers), LOG);
        workersClass = configWorker.parser(config);
        if (configWorker.getError() != RC.CODE_SUCCESS) {
            return configWorker.getError();
        }
        ConfigParserManager configConfigs = new ConfigParserManager(new ManagerGrammar(configs), LOG);
        workersConfigs = configConfigs.parser(config);
        if (configConfigs.getError() != RC.CODE_SUCCESS) {
            return configConfigs.getError();
        }
        return error;
    }

    private RC pipelineBuild(String[] workersClass, String[] workersConfigs) {
        int countWorkers = workersClass.length;
        IReader reader;
        IExecutor[] executors = new IExecutor[countWorkers - 2];
        IWriter writer;

        Object w1 = workerCreate(workersClass[0], workersConfigs[0], LOG);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        if (w1 instanceof IReader) {
            reader = (IReader)w1;
        } else {
            LOG.warning("Unable to build pipeline");
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            return error;
        }
        for(int i = 1; i < countWorkers - 1; i++) {
            Object w = workerCreate(workersClass[i], workersConfigs[i], LOG);
            if (error != RC.CODE_SUCCESS) {
                return error;
            }
            if (w instanceof IExecutor) {
                executors[i-1] = (IExecutor)w;
            } else {
                LOG.warning("Unable to build pipeline");
                error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
                return error;
            }
        }
        Object w2 = workerCreate(workersClass[countWorkers - 1], workersConfigs[countWorkers - 1], LOG);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        if (w2 instanceof IWriter) {
            writer = (IWriter)w2;
        } else {
            LOG.warning("Unable to build pipeline");
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            return error;
        }
        try {
            reader.setInputStream(new FileInputStream(configs[Arrays.asList(ManagerSyntax.managerTokens).indexOf(ManagerSyntax.input)]));
        } catch (FileNotFoundException exception) {
            LOG.warning("Unable to open the input stream");
            error = RC.CODE_INVALID_INPUT_STREAM;
            return error;
        }
        try {
            writer.setOutputStream(new FileOutputStream(configs[Arrays.asList(ManagerSyntax.managerTokens).indexOf(ManagerSyntax.output)]));
        } catch (FileNotFoundException exception) {
            LOG.warning("Unable to open the output stream");
            error = RC.CODE_INVALID_OUTPUT_STREAM;
            return error;
        }
        error = CreateDependencies(reader, executors, writer);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        error = reader.execute();
        return error;
    }

    private RC CreateDependencies (IReader reader, IExecutor[] executors, IWriter writer) {
        RC error;
        error = reader.setConsumer(executors[0]);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        error = writer.setProducer(executors[executors.length - 1]);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        error = executors[0].setProducer(reader);
        if (error != RC.CODE_SUCCESS) {
            return error;
        }
        error = executors[executors.length - 1].setConsumer(writer);
        for (int i = 0, j = 1; i < executors.length - 1 && j < executors.length; i++, j++) {
            error = executors[i].setConsumer(executors[i + 1]);
            if (error != RC.CODE_SUCCESS) {
                return error;
            }
            error = executors[j].setProducer(executors[j - 1]);
            if (error != RC.CODE_SUCCESS) {
                return error;
            }
        }
        return error;
    }

    public RC getError() {
        return error;
    }

    public RC Work(){
        error = pipelineBuild(workersClass, workersConfigs);
        return error;
    }

    private Object workerCreate(String className, String configName, Logger LOG) {
        Object worker = createClass(className, configName, LOG);
        if (worker == null) {
            LOG.warning("Unable to build pipeline");
            error = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            return null;
        } else {
            return worker;
        }
    }
    private Object createClass (String className, String configName, Logger LOG) {
        Object worker;
        try {
            Class<?> _class = Class.forName(className);
            Constructor<?> constructor = _class.getConstructor(Logger.class);
            try {
                worker = constructor.newInstance(LOG);
                error = ((IConfigurable)worker).setConfig(configName);
                if (error != RC.CODE_SUCCESS) {
                    return null;
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                return null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
        return worker;
    }
}
