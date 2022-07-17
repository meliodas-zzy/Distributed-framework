package zzy.distributed.serialize.serializer.impl;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import zzy.distributed.serialize.serializer.MySerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class HessianSerializer implements MySerializer {

    @Override
    public <T> byte[] serialize(T obj) {
        if (Objects.isNull(obj)) {
           throw new NullPointerException();
        }

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HessianOutput ho = new HessianOutput(os);
            ho.writeObject(obj);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if(Objects.isNull(data)) throw new NullPointerException();

        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            HessianInput hi = new HessianInput(is);
            return(T) hi.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
