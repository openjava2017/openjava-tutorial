package org.openjava.mongo.core;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.*;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.openjava.mongo.pojo.ClassRoom;
import org.openjava.mongo.pojo.Feature;
import org.openjava.mongo.pojo.Student;
import org.openjava.mongo.pojo.StudentGroup;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 测试数据结构
 * class(_id: "6313417f0e35b420c7fa7f78", "name: "温江光华实验小学", address: "成都市温江区", created_time: "2022-09-09 15:13")
 * student(_id:"6313417f0e35b420c7fa7f78", name:"黄妙灵", age: 12, gender: false, role:["admin", "leader"],
 *         class_id:"6313417f0e35b420c7fa7f78", score: 90, feature:{height: 162, weight: 70}, created_time: "2022-09-09 15:13")
 */
public class TestMongo {
    public static void main(String[] args) {
        String uri = "mongodb://127.0.0.1:27017/?maxPoolSize=20&w=majority&retryWrites=false";
        // 重要：_id字段默认为ObjectId类型，也可以使用Long类型
        try (MongoClient client = MongoClients.create(uri)) {
//            testInsertTransaction(client);
//            testInsertPOJO(client);
//            testQuery(client);
//            testUpdate(client);
            testDelete(client);
            // https://blog.csdn.net/weixin_42668211/article/details/114954146
        }
    }

    private static void testInsertTransaction(MongoClient client) {
        MongoDatabase database = client.getDatabase("test");
        // 单实例/单节点不支持事务(只在副本集和分片群集模式下支持事务)，因此需要将单节点配置成副本集(副本集只配置一个节点)的方式来支持事务
        // 参见 https://www.jianshu.com/p/5a03b956ce1c
        // 可以通过startSession(ClientSessionOptions options)设置session级别的read/write concern
        ClientSession session = client.startSession();
        // 使用事务时只能设置事务级的read/write concern，不能单独为某个写操作设置read/write concern，否则将抛出异常
        // 如果未设置事务级的read/write concern，将使用session级别的read/write concern;如果都没设置，将使用默认设置
        // read/write concern参数设置参见https://www.mongodb.com/docs/manual/reference/read-concern/
        // https://www.mongodb.com/docs/manual/reference/write-concern/
        TransactionOptions txnOptions = TransactionOptions.builder().readPreference(ReadPreference.secondaryPreferred())
            .readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.MAJORITY).build();
        session.startTransaction(txnOptions);

        MongoCollection<Document> classCollection = database.getCollection("class_room", Document.class);
        Document classDocument = new Document().append("name", "温江光华实验小学").append("address", "成都市温江区")
            // LocalDateTimeCodec将本地日期时间按照UTC+0时区转化成距离1970-01-01T00：00：00Z的毫秒数
            // LocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            .append("created_time", LocalDateTime.now());
        InsertOneResult result = classCollection.insertOne(session, classDocument);
        BsonObjectId classObjectId = (BsonObjectId) result.getInsertedId();

        MongoCollection<BsonDocument> studentCollection = database.getCollection("student", BsonDocument.class);
        BsonArray roles = new BsonArray(Arrays.asList(new BsonString("admin"), new BsonString("leader")));
        var students = new ArrayList<BsonDocument>();
        for (int i = 0; i < new Random().nextInt(8); i++) {
            int randomInt = new Random().nextInt(15);
            BsonDocument feature = new BsonDocument().append("height", new BsonInt32(160 + randomInt))
                .append("weight", new BsonInt32(70 + randomInt));
            BsonDocument bsonDocument = new BsonDocument().append("name", new BsonString("黄妙灵"))
                .append("age", new BsonInt32(12 + randomInt)).append("gender", new BsonBoolean(new Random().nextBoolean()))
                .append("roles", roles).append("class_id", classObjectId).append("score", new BsonInt32(80 + randomInt))
                // MongoDB默认使用的时区是UTC+0, 且MongoDB时间只有BsonType.DATE_TIME和TIMESTAMP两种，并未提供年月日这类日期类型
                .append("feature", feature).append("created_time", new BsonDateTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()));
            students.add(bsonDocument);
        }
        InsertManyResult results = studentCollection.insertMany(session, students);
        var ids = results.getInsertedIds();
        System.out.println(ids);

        // _id字段默认为ObjectId类型，也可以使用Long等任意类型
        BsonDocument bsonDocument = new BsonDocument().append("_id", new BsonInt64(new Random().nextLong()))
            .append("name", new BsonString("winton"));
        MongoCollection<BsonDocument> bsonCollection = database.getCollection("test_collection", BsonDocument.class);
        result = bsonCollection.insertOne(session, bsonDocument);
        var id = result.getInsertedId();
        System.out.println(id);

        session.commitTransaction();
//        session.abortTransaction();
    }

    private static void testInsertPOJO(MongoClient client) {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(pojoCodecProvider));
        MongoDatabase database = client.getDatabase("test").withCodecRegistry(pojoCodecRegistry);
        ClientSession session = client.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder().readPreference(ReadPreference.secondaryPreferred())
            .readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.MAJORITY).build();
        session.startTransaction(txnOptions);

        ClassRoom room = ClassRoom.of("温江光华实验小学", "成都市温江区", LocalDateTime.now());
        MongoCollection<ClassRoom> classCollection = database.getCollection("class_room", ClassRoom.class);
        InsertOneResult result = classCollection.insertOne(session, room);
        BsonObjectId classObjectId = (BsonObjectId) result.getInsertedId();

        List<String> roles = Arrays.asList("admin", "leader");
        Student student = Student.of("黄妙灵", 12, false, roles, classObjectId.getValue().toString(),
            90, Feature.of(162, 68), LocalDateTime.now());
        MongoCollection<Student> studentCollection = database.getCollection("student", Student.class);
        result = studentCollection.insertOne(student);
        System.out.println(result);

        session.commitTransaction();
    }

    private static void testQuery(MongoClient client) {
        // 重要提示：当查询使用事务时，Read preference必须设置成primary
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(pojoCodecProvider));
        MongoDatabase database = client.getDatabase("test").withCodecRegistry(pojoCodecRegistry);
        MongoCollection<Student> studentCollection = database.getCollection("student", Student.class);
        Bson fields = Projections.fields(Projections.include("name", "class_id", "roles", "created_time"));
        Bson filter = Filters.and(Filters.gt("age", 12), Filters.gte("score", 90),
            Filters.gte("feature.height", 170), Filters.all("roles", "leader", "admin"));
//        Filters.exists("roles", true); Filters.type("name", BsonType.NULL);
//        Filters.type("roles", BsonType.ARRAY);
        List<Student> students = studentCollection.find(filter).projection(fields)
            .sort(Sorts.ascending("name")).skip(0).limit(2).into(new ArrayList<>());
        students.forEach(s -> System.out.println(String.format("{%s, %s, %s, %s}", s.getName(), s.getClassId(),
            s.getRoles(), s.getCreatedTime())));
        long total = studentCollection.countDocuments(filter);
        System.out.println("total num: " + total);

        // 可以根据MongoDB查询script的要素反推API写法，比如：... $group: {_id: {class_id: "$class_id", name: "$name"}}...
        Document groupFields = new Document().append("class_id", "$class_id").append("name", "$name");
//        Document groupFields = Document.parse("{\"class_id\": \"$class_id\", \"name\": \"$name\"}");
        List<Bson> aggregates1 = Arrays.asList(
            Aggregates.match(Filters.and(Filters.gte("score", 90), Filters.eq("name", "黄妙灵"))),
            Aggregates.group(groupFields, Accumulators.sum("count", 1), Accumulators.sum("score", "$score"))
//            Aggregates.group("$class_id", Accumulators.sum("count", 1), Accumulators.sum("score", "$score"))
        );
        MongoCollection<Document> studentCollection1 = database.getCollection("student");
        AggregateIterable<Document> result1 = studentCollection1.aggregate(aggregates1);
        result1.forEach(doc -> System.out.println(doc.toJson()));

        List<Bson> aggregates2 = Arrays.asList(
            Aggregates.match(Filters.and(Filters.gte("score", 90), Filters.eq("name", "黄妙灵"))),
            Aggregates.group("$class_id", Accumulators.sum("count", 1), Accumulators.sum("score", "$score"))
        );
        MongoCollection<StudentGroup> studentCollection2 = database.getCollection("student", StudentGroup.class);
        List<StudentGroup> result2 = studentCollection2.aggregate(aggregates2).into(new ArrayList<>());
        result2.forEach(g -> System.out.println(String.format("%s, %s, %s", g.getClassId(), g.getCount(), g.getScore())));
    }

    private static void testUpdate(MongoClient client) {
        MongoDatabase database = client.getDatabase("test");
        ClientSession session = client.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder().readPreference(ReadPreference.secondaryPreferred())
            .readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.MAJORITY).build();
        session.startTransaction(txnOptions);

        MongoCollection<Document> studentCollection = database.getCollection("student");
        Bson filters = Filters.and(Filters.exists("level", false), Filters.gte("score", 90));
        Bson updates = Updates.combine(Updates.set("level", "A"), Updates.set("created_time", LocalDateTime.now()));
        // upsert=true时，如果不存在满足条件的文档将新增
        UpdateOptions options = new UpdateOptions().upsert(false);
        UpdateResult result = studentCollection.updateOne(filters, updates, options);
        // upsert=true且新增成功时返回result.getUpsertedId()
        System.out.println(String.format("updateOne matched: %s, modified: %s, insertId: %s", result.getMatchedCount(),
            result.getModifiedCount(), result.getUpsertedId()));

        filters = Filters.and(Filters.exists("level", false), Filters.lt("score", 90));
        updates = Updates.combine(Updates.set("level", "B"), Updates.set("created_time", LocalDateTime.now()));
        result = studentCollection.updateMany(filters, updates, options);
        System.out.println(String.format("updateMany matched: %s, modified: %s, insertId: %s", result.getMatchedCount(),
            result.getModifiedCount(), result.getUpsertedId()));

        filters = Filters.exists("level", true);
        Bson fields = Projections.fields(Projections.exclude("level"));
        // 当查询使用事务时，Read preference必须设置成primary
//        Document doc = studentCollection.find(session, filters).projection(fields).first();
        Document doc = studentCollection.find(filters).projection(fields).first();
        if (doc != null) {
            // 将指定_id的文档，替换成无level字段的文档(移除level)
            doc = studentCollection.findOneAndReplace(session, Filters.eq(doc.getObjectId("_id")), doc);
            System.out.println("replace doc: " + doc.toJson());
        }

        // findOneAndUpdate findOneAndReplace findOneAndDelete
        session.commitTransaction();
    }

    private static void testDelete(MongoClient client) {
        MongoDatabase database = client.getDatabase("test");
        ClientSession session = client.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder().readPreference(ReadPreference.secondaryPreferred())
            .readConcern(ReadConcern.LOCAL).writeConcern(WriteConcern.MAJORITY).build();
        session.startTransaction(txnOptions);
        MongoCollection<Document> classCollection = database.getCollection("class_room");
        FindIterable<Document> rooms = classCollection.find(Filters.empty());
        MongoCollection<Document> studentCollection = database.getCollection("student");
        rooms.forEach(room -> {
            DeleteResult result = studentCollection.deleteMany(session, Filters.eq("class_id", room.getObjectId("_id")));
            System.out.println(String.format("delete %s students for class %s", result.getDeletedCount(), room.getObjectId("_id")));
        });

        DeleteResult result = classCollection.deleteMany(session, Filters.empty());
        System.out.println(String.format("delete %s class", result.getDeletedCount()));

        MongoCollection<Document> testCollection = database.getCollection("test_collection");
        testCollection.deleteMany(session, Filters.empty());
        session.commitTransaction();
    }
}
