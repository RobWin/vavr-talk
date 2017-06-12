import io.vavr.Function2;
import io.vavr.control.Option;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

public class VavrExamples {

    @Test
    public void liftPartialFunction() {

        // tag::partialDivideFunction[]
        Function2<Integer, Integer, Integer> divide = (a, b) -> a / b;
        // end::partialDivideFunction[]

        // tag::liftedDivideFunction[]
        Function2<Integer, Integer, Option<Integer>> safeDivide = Function2.lift(divide);

        // = None
        Option<Integer> i1 = safeDivide.apply(1, 0); //<1>

        // = Some(2)
        Option<Integer> i2 = safeDivide.apply(4, 2); //<2>
        // end::liftedDivideFunction[]

        then(i1).isEqualTo(Option.none());
        then(i2).isEqualTo(Option.some(2));
    }
}
