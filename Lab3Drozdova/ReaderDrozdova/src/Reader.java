import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.logging.Logger;
import static java.lang.Integer.parseInt;

public class Reader implements IReader {

    private FileInputStream fileInput;
    private int bufferSize;
    private final Logger LOG;
    private final ReaderGrammar grammar = new ReaderGrammar(ReaderSyntax.readerToken);
    private IConsumer consumer;
    private final TYPE[] outputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final ReaderMediator mediator = new ReaderMediator();

    public Reader(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setInputStream(FileInputStream fileInputStream) {
        if(fileInputStream == null) {
            LOG.warning("Invalid input stream");
            return RC.CODE_INVALID_INPUT_STREAM;
        } else {
            fileInput = fileInputStream;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty reader's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            ConfigParserReader configParserReader = new ConfigParserReader(grammar, LOG);
            RC error;
            String[] config = configParserReader.parser(s);
            error = configParserReader.getError();
            if (error == RC.CODE_SUCCESS) {
                bufferSize = parseInt(config[Arrays.asList(ReaderSyntax.readerToken).indexOf(ReaderSyntax.bufferSize)]);
                if (bufferSize <= 0) {
                    LOG.warning("Invalid argument in reader's config: BUFFER_SIZE less than zero");
                    return RC.CODE_INVALID_ARGUMENT;
                }
            }
            return error;
        }
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        if(iConsumer == null) {
            LOG.warning("Reader's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            consumer = iConsumer;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setProducer(IProducer iProducer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {
        try {
            while (fileInput.available() > 0) {
                int currentBufferSize = Math.min(bufferSize, fileInput.available());
                byte[] bytes = new byte[currentBufferSize];
                if (fileInput.read(bytes) == -1) {
                    LOG.warning("No data in input file");
                    return RC.CODE_FAILED_TO_READ;
                }
                mediator.data = new byte[currentBufferSize];
                System.arraycopy(bytes, 0, mediator.data, 0, currentBufferSize);
                RC error = consumer.execute();
                if (error != RC.CODE_SUCCESS) {
                    return error;
                }
            }
            mediator.data = null;
            return consumer.execute();
        } catch (IOException exception) {
            LOG.warning("Error reading the input file");
            return RC.CODE_FAILED_TO_READ;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        mediator.setType(type);
        return mediator;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return outputTypes;
    }

    private class ReaderMediator implements IMediator {
        private TYPE type;
        byte[] data = new byte[bufferSize];

        @Override
        public Object getData() {
            if (data == null || type == null) {
                return null;
            } else {
                switch (type) {
                    case BYTE:
                        return data;
                    case SHORT:
                        return shortData(data);
                    case CHAR:
                        return charData(data);
                    default:
                        return null;
                }
            }

        }

        public void setType(TYPE type) {
            this.type = type;
        }

        private short[] shortData(byte[] data) {
            if (data.length % 2 != 0) {
                return null;
            } else {
                short[] newData = new short[data.length / 2];
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                for (int i = 0; i < data.length / 2; i++) {
                    newData[i] = byteBuffer.getShort(2 * i);
                }
                return newData;
            }
        }

        private char[] charData(byte[] data) {
           CharBuffer cBuffer = ByteBuffer.wrap(data).asCharBuffer();
           return cBuffer.toString().toCharArray();
        }
    }
}