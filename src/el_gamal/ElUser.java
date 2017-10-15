package el_gamal;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class ElUser {

    private BigInteger privateKey,
            publicKey;
    private ElSystem system;
    private Random random = ThreadLocalRandom.current();
    private final String baseFile;

    public ElUser(ElSystem system, String baseFileName) {
        this.baseFile = baseFileName;
        this.privateKey = new BigInteger(8, random);
        this.publicKey = system.getG()
                .modPow(privateKey, system.getP());
        this.system = system;
        System.out.println("G:" + system.getG() + " P:" + system.getP());
        System.out.println("pk:" + publicKey);
    }

    public void sendMessage(ElUser opponent) throws IOException {
        Path encrypt = Paths.get("El_gamal_encrypt");
        Files.deleteIfExists(encrypt);
        Files.createFile(encrypt);
        byte[] bytes = Files.readAllBytes(Paths.get(baseFile));
        BigIntegerPair re;
        System.out.println("opponent pk: " + opponent.getPublicKey());
        System.out.println("encode bytes: " + Arrays.toString(bytes));
//        Serializer serializer = new Serializer(encrypt);
        for (byte b : bytes) {
            re = encryptMessage(BigInteger.valueOf(b), opponent.getPublicKey());
//            serializer.serialize(re);
            Files.write(encrypt, re.toByteArray(), StandardOpenOption.APPEND);
        }
        opponent.receiveMessage(encrypt);
//        serializer.finishSerialisation();
    }

    public void receiveMessage(Path encodedMessage) throws IOException {
        byte[] bytes = Files.readAllBytes(encodedMessage);
        if (bytes.length % 4 != 0) {
            throw new ArrayIndexOutOfBoundsException("В файле нехватает байтов, чтобы произвести десериализацию");
        }
        Path decript = Paths.get("src/el_gamal/decripted_file");
        Files.deleteIfExists(decript);
        Files.createFile(decript);
/*
        Deserializer deserializer = new Deserializer(encodedMessage);
        ArrayList<BigIntegerPair> reList = deserializer.deserialize(bytes);

        for (BigIntegerPair re : reList) {
            Files.write(
                    decript,
                    decryptMessage(re).toByteArray(),
                    StandardOpenOption.APPEND
            );
        }
*/
        for (int i = 0; i + 4 != bytes.length; i += 4) {
            Files.write(
                    decript,
                    decryptMessage(new BigIntegerPair(Arrays.copyOfRange(bytes, i, i + 4))).toByteArray(),
                    StandardOpenOption.APPEND
            );
        }

    }

    /*public byte[] encryptMessage(byte msg, BigInteger userPK) {
        BigInteger k = new BigInteger(8, random);
        ArrayList<BigInteger> re = new ArrayList<BigInteger>() {{
            add(system.getG().modPow(k, system.getP()));
            add(
                    userPK.modPow(k, system.getP())
                            .multiply(BigInteger.valueOf(msg))
                            .mod(system.getP())
            );
        }};
        byte[] bytes = new byte[4], partOne = re.get(0).toByteArray(), partTwo = re.get(1).toByteArray();
        bytes[0] = partOne[0];
        bytes[0] = partOne[1];
        bytes[0] = partTwo[0];
        bytes[0] = partTwo[1];
        return bytes;
    }*/

    /**
     *
     * @param msg
     * @param userPK
     * @return <r, e>, where r - g^(k)%P and p with g ~ 16bit => r = 2byte, r + e = 4byte
     */
    public BigIntegerPair encryptMessage(BigInteger msg, BigInteger userPK) {
        BigInteger k = new BigInteger(16, random);
        return new BigIntegerPair(
                system.getG().modPow(k, system.getP()),
                    userPK.modPow(k, system.getP())
                            .multiply(msg)
                            .mod(system.getP())
        );

    }

    public BigInteger decryptMessage(BigIntegerPair re) {
        return re.second
                .multiply(
                        re.first
                                .modPow(
                                        system.getP()
                                                .subtract(ONE)
                                                .subtract(privateKey),
                                        system.getP()
                                )
                )
                .mod(system.getP());
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    private static class BigIntegerPair implements Serializable {
        private final BigInteger first;
        private final BigInteger second;

        public BigIntegerPair(BigInteger first, BigInteger second) {
            this.first = first;
            this.second = second;
        }

        byte[] toByteArray() throws IOException {
            System.out.println(Arrays.toString(first.toByteArray()) + " encoded " + Arrays.toString(second.toByteArray()));
            System.out.println(first + " encoded " + second);
            byte[] first = this.first.toByteArray();
            byte[] second = this.second.toByteArray();/*
            first = first.length != 2 ? new byte[] {first[0], 0} : first;
            second = second.length != 2 ? new byte[] {second[0], 0} : second;*/
            return concatenateArrays(first, second);
            /*char[] chars = new char[] { (char)this.first.intValue(), (char) this.second.intValue() };
            return new String(chars).getBytes("UTF-8");*/
        }

        public BigIntegerPair(byte[] bytes) {
            if (bytes.length < 4) {
                throw new ArrayIndexOutOfBoundsException("Передано недостаточное кол-во байтов");
            }
            byte[] first = new byte[] {bytes[0], bytes[1]};
            byte[] second = new byte[] {bytes[2], bytes[3]};
            this.first = new BigInteger(first);
            this.second = new BigInteger(second);
            System.out.println(Arrays.toString(first) + " decoded " + Arrays.toString(second));
        }

        private byte[] concatenateArrays(byte[] a, byte[] b) {
            int aLen = a.length;
            int bLen = b.length;
            byte[] c = new byte[aLen+bLen];
            System.arraycopy(a, 0, c, 0, aLen);
            System.arraycopy(b, 0, c, aLen, bLen);
            return c;
        }
    }
/*
    static class Deserializer {

        final ObjectInputStream inputStream;

        public Deserializer(Path path) throws IOException {
            inputStream = new ObjectInputStream(new FileInputStream(path.toFile()));
        }

        ArrayList<BigIntegerPair> deserialize(byte[] bytes) throws IOException {
            ArrayList<BigIntegerPair> reList = new ArrayList<>();
            try {
                BigIntegerPair re;
                for (re = (BigIntegerPair) inputStream.readObject();
                     re != null; reList.add(re), re = (BigIntegerPair) inputStream.readObject());
            } catch (ClassNotFoundException | EOFException e) {
                return reList;
            }
            return reList;
        }

        void finishDeserialisation() throws IOException {
            inputStream.close();
        }

    }

    static class Serializer {

        final ObjectOutputStream outputStream;

        public Serializer(Path encrypt) throws IOException {
            outputStream = new ObjectOutputStream(new FileOutputStream(encrypt.toFile()));
        }

        void serialize(BigIntegerPair bip) {
            try {
                outputStream.writeObject(bip);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void finishSerialisation() throws IOException {
            outputStream.close();
        }
    }*/
}
