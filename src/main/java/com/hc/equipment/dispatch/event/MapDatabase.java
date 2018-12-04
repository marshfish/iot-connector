package com.hc.equipment.dispatch.event;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class MapDatabase {
    @Resource
    private Gson gson = new Gson();
    private volatile DB db;
    private static final String FILE_PATH = System.getProperty("user.dir") +
            File.separator + "db" + File.separator + "msgDB";

    /**
     * 持久化的是上传的消息，消息重发并不频繁，因此默认不连接数据库文件，读写时连接
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private HTreeMap<String, String> connect(String selectDB) {
        //并发创建同一DB可能导致创建DB对象后先创建的对象未被使用，且未调用close方法而被GC，导致锁mapDB的数据库文件
        if (db == null) {
            synchronized (this) {
                if (db == null) {
                    db = DBMaker.fileDB(new File(FILE_PATH)).closeOnJvmShutdown().make();
                }
            }
        }
        return db.hashMap(selectDB).
                    keySerializer(Serializer.STRING).
                    valueSerializer(Serializer.JAVA).
                    //持久化的消息最多保存48小时，48小时后无论发送成功或失败都会彻底删除
                            expireAfterCreate(24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS).
                            createOrOpen();

    }

    /**
     * 支持并发写
     */
    @SneakyThrows
    public MapDatabase write(String key, Object value, String selectDB) {
        Optional.of(connect(selectDB)).ifPresent(map -> map.put(key, gson.toJson(value)));
        return this;
    }

    public void remove(String key, String selectDB) {
        Optional.of(connect(selectDB)).ifPresent(map -> map.remove(key));
    }

    /**
     * 注意DB一旦关闭，map也随之关闭，FileDB里的HTreeMap是文件内容的映射，并不会一直存在
     * 小心db过大吃满数据上传线程
     * @param type 反序列化类型
     * @param selectDB DB名
     * @param consumer 操作
     * @param <T> void
     */
    public synchronized <T> void read(Class<T> type, String selectDB, Consumer<T> consumer) {
        Optional.of(connect(selectDB)).
                ifPresent(map -> map.
                        forEach((key, value) -> {
                            consumer.accept(gson.fromJson(value, type));
                        }));
    }

    /**
     * 注意写操作完成一定要closeDB，否则会导致脏数据
     */
    public void close() {
        if (db != null) {
            db.close();
        }
    }
}
