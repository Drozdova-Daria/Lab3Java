import ru.spbstu.pipeline.*;
import ru.spbstu.pipeline.IExecutor;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class Executor implements IExecutor {

    private final Logger LOG;
    private final RLE rle = new RLE();
    private boolean compression;
    private int lengthSequence;
    private final ExecutorGrammar grammar = new ExecutorGrammar(ExecutorSyntax.executorToken);
    private final TYPE[] inputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final TYPE[] outputTypes = {TYPE.BYTE, TYPE.SHORT, TYPE.CHAR};
    private final ExecutorMediator mediator = new ExecutorMediator();
    private IConsumer consumer;
    private IMediator producerMediator;
    private TYPE producerMediatorType;

    public Executor(Logger LOG) {
        this.LOG = LOG;
    }

    @Override
    public RC setConfig(String s) {
        if (s == null) {
            LOG.warning("Empty executor's config name string");
            return RC.CODE_INVALID_ARGUMENT;
        } else {
            RC error;
            ConfigParserExecutor configParserExecutor = new ConfigParserExecutor(grammar, LOG);
            String[] config = configParserExecutor.parser(s);
            error = configParserExecutor.getError();
            if(error != RC.CODE_SUCCESS) {
                return error;
            } else {
                lengthSequence = Integer.parseInt(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.lengthSequence)]);
                compression = Boolean.parseBoolean(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.compression)]);
                boolean recovery = Boolean.parseBoolean(config[Arrays.asList(ExecutorSyntax.executorToken).indexOf(ExecutorSyntax.recovery)]);
                if (compression && recovery) {
                    LOG.warning("Invalid argument in executor's config: COMPRESSION and RECOVERY match");
                    return RC.CODE_INVALID_ARGUMENT;
                }

            }
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        if (iConsumer == null) {
            LOG.warning("Executor's consumer is null: is impossible to build pipeline");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        } else {
            consumer = iConsumer;
            return RC.CODE_SUCCESS;
        }
    }

    @Override
    public RC setProducer(IProducer iProducer) {
        if (iProducer == null) {
            LOG.warning("Executor's producer is null: is impossible to build pipeline");
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
        if (data == null) {
            mediator.data = null;
            return consumer.execute();
        } else {
            RC error = processData(data);
            if (error != RC.CODE_SUCCESS) {
                return error;
            }
            error = consumer.execute();
            if (error != RC.CODE_SUCCESS) {
                return error;
            }
            return error;
        }
    }

    private RC processData(Object data) {
        if (data != null) {
            switch (producerMediatorType) {
                case BYTE:
                    mediator.data = arrayWork((byte[]) data);
                    break;
                case SHORT:
                    mediator.data = arrayWork(shortInByte((short[]) data));
                    break;
                case CHAR:
                    mediator.data = arrayWork(charInByte((char[]) data));
                    break;
                default:
                    LOG.warning("Unknown mediator type");
                    return RC.CODE_INVALID_ARGUMENT;
            }
        }
        return RC.CODE_SUCCESS;
    }

    private byte[] arrayWork(byte[] bytes) {
        if (compression) {
            return rle.RLECompression(bytes, lengthSequence);
        } else {
            return rle.RLERecovery(bytes);
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

    @Override
    public TYPE[] getOutputTypes() {
        return outputTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        mediator.setType(type);
        return mediator;
    }

    private static class ExecutorMediator implements IMediator {
        private TYPE type;
        private byte[] data;

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
