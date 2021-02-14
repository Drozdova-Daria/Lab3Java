import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger("Lab2 Logger");
    static RC CheckArgs(String[] args) {
        if (args.length != 1) {
            LOG.warning("Invalid number of program argument");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            return RC.CODE_SUCCESS;
        }
    }
    public static void main(String[] args) {
        RC error = CheckArgs(args);
        if (error == RC.CODE_SUCCESS) {
            Manager manager = new Manager(LOG, args[0]);
            error = manager.getError();
            if (error != RC.CODE_SUCCESS) {
                LOG.info("The program failed with an error");
                return;
            }
            error = manager.setConfig(args[0]);
            if (error != RC.CODE_SUCCESS) {
                LOG.info("The program failed with an error");
                return;
            }
            error = manager.Work();
            if (error != RC.CODE_SUCCESS) {
                LOG.info("The program failed with an error");
                return;
            }
            LOG.info("The program ended successfully");
        } else {
            LOG.info("The program failed with an error");
        }
    }
}
