package demo;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.match.annotation.Patterns;
import io.vavr.match.annotation.Unapply;

@Patterns
public interface UserPatterns {

    @Unapply
    static Tuple2<String, String> User(User user) {
        return Tuple.of(user.getName(), user.getId());
    }
}
