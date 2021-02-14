import ru.spbstu.pipeline.RC;

import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Scanner;
import java.util.logging.Logger;

public class ConfigParserManager {
    private final ManagerGrammar grammar;
    private static RC error;
    private final Logger LOG;

    public ConfigParserManager(ManagerGrammar grammar, Logger LOG) {
        this.grammar = grammar;
        this.LOG = LOG;
    }

    public String[] parser(String filename) {
        try {
            Scanner scanner = new Scanner(new FileReader(filename));
            String[] readied = new String[grammar.numberTokens()];
            while (scanner.hasNextLine()) {
                String[] parameter = scanner.nextLine().split(grammar.delimiter());
                error = ReadParameter(parameter, readied);
                if (error != RC.CODE_SUCCESS) {
                    break;
                }
            }
            scanner.close();
            return readied;
        } catch (IOException ex) {
            LOG.warning("Unable to open the manager's config's input stream");
            error = RC.CODE_INVALID_INPUT_STREAM;
        }
        return new String[]{""};
    }

    private RC ReadParameter (String[] parameter, String[] readied) {
        if(parameter.length != 2) {
            LOG.warning("Invalid argument in manager's config");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            for (int i = 0; i < grammar.numberTokens(); i++) {
                skipSpaces(parameter);
                if (parameter[0].equals(grammar.token(i))) {
                    if (parameter[0].equals(ManagerSyntax.countWorker)) {
                        if (!isNumber(parameter[1])) {
                            LOG.warning("Invalid argument in manager's config: COUNT_WORKERS isn't a number");
                            return RC.CODE_INVALID_ARGUMENT;
                        }
                    }
                    readied[i] = parameter[1];
                    break;
                }
            }
            return RC.CODE_SUCCESS;
        }
    }

    private static void skipSpaces (String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i].replaceAll(" ", "");
        }
    }

    private static boolean isNumber(String sting) {
        NumberFormat format = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        format.parse(sting, pos);
        return sting.length() == pos.getIndex();
    }

    public RC getError() {
        return error;
    }
}
