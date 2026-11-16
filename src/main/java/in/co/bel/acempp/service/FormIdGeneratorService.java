package in.co.bel.acempp.service;

import com.mongodb.client.AggregateIterable;
import in.co.bel.acempp.entity.FormIdCounter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FormIdGeneratorService {

    private final MongoTemplate mongoTemplate;

    public String generate(
            String collectionName,
            String idField,
            String prefix,
            int sequenceWidth) {

        int year = Year.now().getValue();
        long highestExistingSequence =
                findHighestExistingSequence(
                        collectionName,
                        idField,
                        prefix,
                        year
                );

        String counterId = String.join(
                ":",
                collectionName,
                idField,
                prefix,
                String.valueOf(year)
        );

        Query counterQuery = Query.query(
                Criteria.where("_id").is(counterId)
        );

        /*
         * Keep an existing counter ahead of manually inserted/imported forms,
         * while bootstrapping safely from existing collections on deployment.
         */
        try {
            mongoTemplate.upsert(
                    counterQuery,
                    new Update().max(
                            "sequence",
                            highestExistingSequence
                    ),
                    FormIdCounter.class
            );
        } catch (DuplicateKeyException ignored) {
            /*
             * Another request initialized this same counter concurrently.
             * The atomic increment below will still allocate a unique value.
             */
        }

        FormIdCounter counter = mongoTemplate.findAndModify(
                counterQuery,
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options()
                        .returnNew(true)
                        .upsert(true),
                FormIdCounter.class
        );

        if (counter == null) {
            throw new IllegalStateException(
                    "Unable to generate form ID for " + prefix
            );
        }

        return String.format(
                "%s-%d-%0" + sequenceWidth + "d",
                prefix,
                year,
                counter.getSequence()
        );
    }

    private long findHighestExistingSequence(
            String collectionName,
            String idField,
            String prefix,
            int year) {

        String idPrefix = prefix + "-" + year + "-";
        String regex = "^"
                + Pattern.quote(idPrefix)
                + "\\d+$";

        Document splitId = new Document(
                "$split",
                List.of("$" + idField, "-")
        );

        Document suffix = new Document(
                "$arrayElemAt",
                List.of(splitId, -1)
        );

        List<Document> pipeline = List.of(
                new Document(
                        "$match",
                        new Document(
                                idField,
                                new Document("$regex", regex)
                        )
                ),
                new Document(
                        "$project",
                        new Document(
                                "sequence",
                                new Document("$toLong", suffix)
                        )
                ),
                new Document(
                        "$sort",
                        new Document("sequence", -1)
                ),
                new Document("$limit", 1)
        );

        AggregateIterable<Document> results =
                mongoTemplate
                        .getCollection(collectionName)
                        .aggregate(pipeline);

        Document highest = results.first();

        if (highest == null) {
            return 0;
        }

        Number sequence = highest.get("sequence", Number.class);
        return sequence != null ? sequence.longValue() : 0;
    }
}
