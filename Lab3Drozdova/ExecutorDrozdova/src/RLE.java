public class RLE {
    static final String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    public byte[] RLECompression (byte[] array, int lengthSequence) {
        byte[] rle = new byte[array.length * 2];
        byte[] previous;
        byte[] current;
        int indexRLE = 0;
        previous = initArray(array, lengthSequence, 0);
        int count = 1;
        for(int i = lengthSequence; i <= array.length; i += lengthSequence) {
            int currentLength = remainingLengthCheck(array.length, i, lengthSequence);
            current = i < array.length ? initArray(array, currentLength, i) : new byte[currentLength];
            if (currentLength < lengthSequence && currentLength > 0) {
                rle[indexRLE++] = getControlByte(currentLength, 1);
                for (int j = 0; j < currentLength; j++) {
                    rle[indexRLE++] = (current[j]);
                }
            } else {
                if (!compare(current, previous) || i == array.length) {
                    rle[indexRLE++] = getControlByte(lengthSequence, count);
                    for (int j = 0; j < lengthSequence; j++) {
                        rle[indexRLE++] = (previous[j]);
                    }
                    count = 1;
                } else {
                    count++;
                }
                previous = current;
            }
        }
        rle = cropArray(rle, indexRLE);
        return rle;
    }

    public byte[] RLERecovery (byte[] rle) {
        byte[] originalSequence = new byte[rle.length * hexInInt(hex[hex.length - 1].charAt(0))];
        int index = 0;
        for(int i = 0; i < rle.length; ) {
            String controlByte = String.format("%02x", rle[i++]);
            int lengthSequence = hexInInt(controlByte.charAt(0));
            int count = hexInInt(controlByte.charAt(1));
            for(int j = 0; j < count; j++) {
                for(int k = 0; k < lengthSequence; k++) {
                    originalSequence[index++] = rle[i + k];
                }
            }
            i += lengthSequence;
        }
        originalSequence = cropArray(originalSequence, index);
        return originalSequence;
    }

    private byte[] cropArray(byte[] array, int size) {
        byte[] rleArr = new byte[size];
        System.arraycopy(array, 0, rleArr, 0, size);
        return rleArr;
    }

    private int remainingLengthCheck(int generalLength, int readLength, int length) {
        return Math.min(generalLength - readLength, length);
    }

    private byte hexInByte(String hexByte) {
        return (byte) ((Character.digit(hexByte.charAt(0), 16) << 4) + Character.digit(hexByte.charAt(1), 16));
    }

    private byte getControlByte (int length, int count) {
        String controlByteStr = String.valueOf(length) + getHexNumber(count);
        return hexInByte(controlByteStr);
    }

    private byte[] initArray(byte[] array, int length, int position) {
        byte[] newArray = new byte[length];
        int j = 0;
        for(int i = position ; i < position + length; i++) {
            newArray[j++] = array[i];
        }
        return newArray;
    }

    private String getHexNumber(int value) {
        return hex[value];
    }

    private int hexInInt (char value) {
        return arraySearch(hex, value);
    }

    private boolean compare(byte[] array1, byte[] array2) {
        if(array1.length != array2.length) {
            return false;
        }
        for(int i = 0; i < array1.length; i++) {
            if(array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    private int arraySearch (String[] arr, char value) {
        for(int i = 0; i < arr.length; i++) {
            if (RLE.hex[i].equals("" + value)) {
                return i;
            }
        }
        return -1;
    }
}
