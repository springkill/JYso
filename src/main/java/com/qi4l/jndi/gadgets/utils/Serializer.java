package com.qi4l.jndi.gadgets.utils;

import com.caucho.hessian.io.*;
import com.qi4l.jndi.gadgets.utils.utf8OverlongEncoding.UTF8OverlongObjectOutputStream;
import com.thoughtworks.xstream.XStream;

import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import static com.qi4l.jndi.gadgets.Config.Config.*;

public class Serializer implements Callable<byte[]> {
    private final Object object;

    public Serializer(Object object) {
        this.object = object;
    }

    public static byte[] serialize(final Object obj) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        serialize(obj, out);
        return out.toByteArray();
    }

    public static byte[] serialize(final Object obj, final ByteArrayOutputStream out) throws IOException {
        final ObjectOutputStream objOut;
        objOut = new ObjectOutputStream(out);
        objOut.writeObject(obj);
        byte[] bytes = out.toByteArray();
        objOut.close();
        return bytes;
    }

    public static void qiserialize(Object obj, final OutputStream out) throws Exception {
        ObjectOutputStream    objOut  = null;
        AbstractHessianOutput AobjOut = null;
        ByteArrayOutputStream outB64  = new ByteArrayOutputStream();

        if (IS_UTF_Bypass) {
            if (BASE64) {
                objOut = new UTF8OverlongObjectOutputStream(outB64);
            } else {
                objOut = new UTF8OverlongObjectOutputStream(out);
            }
        } else if (IS_Hessian1) {
            if (BASE64) {
                AobjOut = new HessianOutput(outB64);
            } else {
                AobjOut = new HessianOutput(out);
            }
            NoWriteReplaceSerializerFactory sf = new NoWriteReplaceSerializerFactory();
            sf.setAllowNonSerializable(true);
            AobjOut.setSerializerFactory(sf);
        } else if (IS_Hessian2) {
            if (BASE64) {
                AobjOut = new Hessian2Output(outB64);
            } else {
                AobjOut = new Hessian2Output(out);
            }
            NoWriteReplaceSerializerFactory sf = new NoWriteReplaceSerializerFactory();
            sf.setAllowNonSerializable(true);
            AobjOut.setSerializerFactory(sf);
            AobjOut.writeObject(obj);
            AobjOut.close();
        } else if (IS_XSTREAM) {
            xStreamSerialize(obj);
        } else {
            if (BASE64) {
                objOut = new SuObjectOutputStream(outB64);
            } else {
                objOut = new SuObjectOutputStream(out);
            }
        }

        if (IS_Hessian1 || IS_Hessian2) {
            AobjOut.writeObject(obj);
        } else if (IS_XSTREAM){
            return;
        } else {
            objOut.writeObject(obj);
        }

        if (BASE64) {
            String encodedString = Base64.getEncoder().encodeToString(outB64.toByteArray());
            System.out.println(encodedString);
        }

    }

    public byte[] call() throws Exception {
        return serialize(object);
    }

    public static void xStreamSerialize(Object payload) {
        XStream xstream = new XStream();
        String xml = xstream.toXML(payload);
        System.out.println(xml);
    }

    public static class SuObjectOutputStream extends ObjectOutputStream {

        public SuObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            super.writeStreamHeader();
            try {
                // 写入
                for (int i = 0; i < DIRTY_LENGTH_IN_TC_RESET; i++) {
                    Reflections.getMethodAndInvoke(Reflections.getFieldValue(this, "bout"), "writeByte", new Class[]{int.class}, new Object[]{TC_RESET});
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class NoWriteReplaceSerializerFactory extends SerializerFactory {

        /**
         * {@inheritDoc}
         *
         * @see com.caucho.hessian.io.SerializerFactory#getObjectSerializer(java.lang.Class)
         */
        @Override
        public com.caucho.hessian.io.Serializer getObjectSerializer(Class<?> cl) throws HessianProtocolException {
            return super.getObjectSerializer(cl);
        }

        /**
         * {@inheritDoc}
         *
         * @see com.caucho.hessian.io.SerializerFactory#getSerializer(java.lang.Class)
         */
        @Override
        public com.caucho.hessian.io.Serializer getSerializer(Class cl) throws HessianProtocolException {
            com.caucho.hessian.io.Serializer serializer = super.getSerializer(cl);

            if (serializer instanceof WriteReplaceSerializer) {
                return UnsafeSerializer.create(cl);
            }
            return serializer;
        }
    }

}
