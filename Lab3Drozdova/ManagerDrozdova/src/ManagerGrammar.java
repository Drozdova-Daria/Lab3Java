import ru.spbstu.pipeline.BaseGrammar;

public class ManagerGrammar extends BaseGrammar{
    private static final String space = " ";

    ManagerGrammar(String[] tokens) {
        super(tokens);
    }

    String space() {
        return space;
    }
}
