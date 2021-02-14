import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;

public class Writer implements IWriter {

    private FileOutputStream fileOutput;
    private int buffetSize;
    private final Logger LOG;
    private final WriterGrammar grammar = new WriterGrammar(WriterSyntax.writerToken);
    private final TYPE[] inputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private IMediator producerMediator;
    private TYPE producerMediatorType;

    public Writer(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setOutputStream(FileOutputStream fileOutputStream) {
        if(fileOutputStream == null) {
            LOG.warning("Invalid output stream");
            return RC.CODE_INVALID_INPUT_STREAM;
        } else {
            fileOutput = fileOutputStream;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty writer's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            ConfigParserWriter configParserWriter = new ConfigParserWriter(grammar, LOG);
            RC error;
            String[] config = configParserWriter.parser(s);
            error = configParserWriter.getError();
            if (error == RC.CODE_SUCCESS) {
                buffetSize = parseInt(config[Arrays.asList(WriterSyntax.writerToken).indexOf(WriterSyntax.bufferSize)]);
                if (buffetSize <= 0) {
                    LOG.warning("Invalid argument in writer's config: BUFFER_SIZE less than zero");
                    return RC.CODE_CONFIG_SEMANTIC_ERROR;
                }
            }
            return error;
        }
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer iProducer) {
        if(iProducer == null) {
            LOG.warning("Writer's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            TYPE[] producerTypes = iProducer.getOutputTypes();
            for (TYPE inputType : inputTypes) {
                for (TYPE producerType : producerTypes) {
                    if (producerType == inputType) {
                        producerMediator = iProducer.getMediator(producerType);
                        producerMediatorType = producerType;
                        return RC.CODE_SUCCESS;
                    }
                }
            }
            LOG.warning("There is no match between the types of the employee and the types of its producer");
        }
        return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }

    @Override
    public RC execute() {
        Object data = producerMediator.getData();
        if (data != null) {
            byte[] bufferByte = processData(data);
            if (bufferByte == null) {
                return RC.CODE_INVALID_ARGUMENT;
            }
            byte[] buffer;
            for (int i = 0; i < bufferByte.length; i = i + buffetSize) {
                int currentBufferSize = min(buffetSize, bufferByte.length - i);
                buffer = Arrays.copyOfRange(bufferByte, i, i + currentBufferSize);
                try {
                    fileOutput.write(buffer);
                } catch (IOException exception) {
                    LOG.warning("Error writing output file");
                    return RC.CODE_FAILED_TO_WRITE;
                }
            }
        }
        return RC.CODE_SUCCESS;
    }

    private byte[] processData(Object data) {
        switch (producerMediatorType) {
            case BYTE:
                return (byte[]) data;
            case SHORT:
                return shortInByte((short[]) data);
            case CHAR:
                return charInByte((char[]) data);
            default:
                LOG.warning("Unknown mediator type");
                return null;
        }

    }

    private byte[] shortInByte(short[] data) {
        byte[] newData = new byte[data.length * 2];
        for (int i = 0, j = 0; i < newData.length; i+=2, j++) {
            newData[i] = (byte)(data[j] & 0xff);
            newData[i + 1] = (byte)((data[i] >> 8) & 0xff);
        }
        return newData;
    }

    private byte[] charInByte(char[] data) {
        CharBuffer charBuffer = CharBuffer.wrap(data);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] newData = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte)0);
        return newData;
    }
}