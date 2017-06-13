import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import org.javamoney.moneta.Money;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.money.MonetaryAmount;
import javax.ws.rs.WebApplicationException;

import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.metrics.CircuitBreakerMetrics;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.API;
import io.vavr.Function0;
import io.vavr.Function1;
import io.vavr.Function2;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.anyOf;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Predicates.is;
import static io.vavr.Predicates.isIn;
import static io.vavr.Predicates.noneOf;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.BDDAssertions.then;

public class VavrExamples {

    private Logger LOG = LoggerFactory.getLogger(VavrExamples.class);

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

    @Test
    public void higherOrderFunction() {
        Function1<Function1<Integer, Integer>, Function1<Integer, Integer>> multiplyByTwo =
                (Function1<Integer, Integer> function) -> (Integer y) ->  function.apply(y) * 2;

        Function1<Integer, Integer> plusOneAndMultiplyTwo = multiplyByTwo.apply(a -> a + 1);
        then(plusOneAndMultiplyTwo.apply(2)).isEqualTo(6);

    }

    @Test
    public void composition() {
        Function1<Integer, Integer> plusOne = a -> a + 1;
        Function1<Integer, Integer> multiplyByTwo = a -> a * 2;
        Function1<Integer, Integer> plusOneAndMultiplyBy2 = plusOne.andThen(multiplyByTwo);
        then(plusOneAndMultiplyBy2.apply(2)).isEqualTo(6);
    }

    @Test
    public void filter() {
        List<String> fruits = List.of("orange", "apple", "lemon", "orange");
        java.util.List<String> oranges = filterOranges(fruits).toJavaList();
        then(oranges).hasSize(2);
    }

    @Test
    public void map() {
        Map<String, MonetaryAmount> prices = HashMap.of(
                "orange", Money.of(2, "EUR"),
                "apple", Money.of(1.50, "EUR"),
                "lemon", Money.of(1, "EUR"));
        Option<MonetaryAmount> peachPrice = prices.get("peach");

        Map<String, MonetaryAmount> doubledPrices = prices.mapValues(price -> price.multiply(2));

        MonetaryAmount price = List.of("orange", "orange", "apple")
                .map(doubledPrices)
                .reduce(MonetaryAmount::add);
        then(price).isEqualTo(Money.of(11, "EUR"));
    }

    @Test
    public void filterJdk() {
        java.util.List<String> fruits = Arrays.asList("orange", "apple", "lemon");
        List<String> oranges = filterOranges(List.ofAll(fruits));
    }

    public List<String> filterOranges(List<String> fruits){
        return fruits.map(String::toUpperCase)
            .filter(fruit -> fruit.equals("ORANGE"));
    }

    public Option<String> findOrange(List<String> fruits){
        return fruits.find(fruit -> fruit.equals("ORAGNE"));
    }




    public void  bla(){
        Function2<Integer, Integer, Integer> sum = (a, b) -> a + b;
        // Parameter a wird mit 2 festgeschrieben
        Function1<Integer, Integer> add2 = sum.curried().apply(2);

        then(add2.apply(4)).isEqualTo(6);


        Function0<Double> hashCache =
                Function0.of(Math::random).memoized();

        double randomValue1 = hashCache.apply();
        double randomValue2 = hashCache.apply();

        then(randomValue1).isEqualTo(randomValue2);


        Function2<Integer, Integer, Integer> divide = (a, b) -> a / b;

        Function2<Integer, Integer, Option<Integer>> safeDivide = Function2.lift(divide);

        // = None
        Option<Integer> i1 = safeDivide.apply(1, 0);

        // = Some(2)
        Option<Integer> i2 = safeDivide.apply(4, 2);


        // = (Java, 8)
        Tuple2<String, Integer> java8 = Tuple.of("Java", 8);
        // = (Vavr, 1)
        Tuple2<String, Integer> vavr1 = java8.map(
                s -> s.substring(2) + "vr",
                i -> i / 8);
        // = "Vavr"
        String vavr = vavr1._1;
        // = 1
        int one = vavr1._2;
    }

    int sum(int first, int second) {
        if (first < 0 || second < 0) {
            throw new IllegalArgumentException("Only positive integers are allowed");
        }
        return first + second;
    }


    // Service Interface mit Checked exception
    public interface HelloWorldService {
        String sayHelloWorld(String name) throws BusinessException;
        //Try<String> sayHelloWorld(String name);
    }

    public void tryExample(){
        HelloWorldService helloWorldService = null;

        String result = Try.of(() -> helloWorldService.sayHelloWorld("Robert"))
                .map(value -> value + " und allen Anwesenden")
                .recover(BusinessException.class, throwable -> "Ich muss weg!")
                .onFailure(throwable -> LOG.warn("Handled exception", throwable))
                .getOrElse("Ich muss auch weg");
    }

    public void eitherExample(){
        Either<Exception, String> either = Either.right("Hello world");

        // = "HELLO WORLD"
        String result = either
                .map(String::toUpperCase)
                .get();

        then(result).isEqualTo("HELLO WORLD");

        Try<String> toTry = either.toTry();
        Option<String> toOption = either.toOption();

        String helloWorld = Option.of("Hello")
                .map(value -> value + " world")
                .peek(value -> LOG.debug("Value: {}", value))
                .getOrElse(() -> "Ich muss weg!");
    }

    public void lazyExample(){
        Lazy<Double> lazy = Lazy.of(Math::random);
        lazy.isEvaluated(); // = false
        lazy.get();         // = 0.123 (random generated)
        lazy.isEvaluated(); // = true
        lazy.get();         // = 0.123 (cached)
    }

    public String patternMatching(Integer i){
        return Match(i).of(
                Case($(1), () -> "1"),
                Case($(isIn(2, 3)), () ->  "2 or 3"),
                Case($(anyOf(is(4), noneOf(is(5), is(6)))), () ->  "4 or not (5 or 6)"),
                Case($(), () -> "?")
                );
    }

    public void circuitBreaker() throws Exception {

        HelloWorldService service = null;

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .recordFailure(throwable -> Match(throwable).of(
                        Case($(instanceOf(IOException.class)), true),
                        Case($(instanceOf(WebApplicationException.class)),
                                ex -> ex.getResponse().getStatus() == INTERNAL_SERVER_ERROR.getStatusCode()),
                        Case($(), false)))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);

        Callable<String> decorateCallable = CircuitBreaker
                .decorateCallable(circuitBreaker, () -> service.sayHelloWorld("Robert"));
        String result = Try.ofCallable(decorateCallable)
            .getOrElse("Ich muss weg!");

        result = circuitBreaker.executeCallable(() -> service.sayHelloWorld("Robert")) ;

    }

    public void metrics() throws Exception {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = registry.circuitBreaker("backendName");

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        float failureRate = metrics.getFailureRate();
        int bufferedCalls = metrics.getNumberOfBufferedCalls();
        int failedCalls = metrics.getNumberOfFailedCalls();
        int successfulCalls = metrics.getNumberOfSuccessfulCalls();
        long notPermittedCalls = metrics.getNumberOfNotPermittedCalls();

        MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("myRegistry");
        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(registry));
    }

    public void events() throws Exception {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("name1");
        circuitBreaker.getEventPublisher()
            .onSuccess(event -> LOG.debug(event.toString()))
            .onError(event -> LOG.warn(event.toString()))
            .onStateTransition(event -> LOG.info(event.toString()));

        RxJava2Adapter.toFlowable(circuitBreaker.getEventPublisher())
                .filter(event -> event.getEventType() == Type.ERROR)
                .cast(CircuitBreakerOnErrorEvent.class)
                .subscribe(event -> LOG.warn(event.toString()));




        CircularEventConsumer<CircuitBreakerEvent> ringBuffer = new CircularEventConsumer<>(10);
        circuitBreaker.getEventPublisher().onEvent(ringBuffer);
        List<CircuitBreakerEvent> bufferedEvents = ringBuffer.getBufferedEvents();
    }


    public void rateLimiter() throws Exception {
        HelloWorldService service = null;

        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .build();
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);

        Callable<String> decorateCallable = RateLimiter
                .decorateCallable(rateLimiter, () -> service.sayHelloWorld("Robert"));

        Try<String> firstTry = Try.ofCallable(decorateCallable);
        then(firstTry.isSuccess()).isTrue();
        Try<String> secondTry = Try.ofCallable(decorateCallable);
        then(secondTry.isFailure()).isTrue();
        then(secondTry.getCause()).isInstanceOf(RequestNotPermitted.class);

    }

    @Test
    public void bulkhead() throws Exception {
        HelloWorldService service = null;

        BulkheadConfig config = BulkheadConfig.custom()
                .maxWaitTime(1000)
                .maxConcurrentCalls(10)
                .build();

        Bulkhead bulkhead = Bulkhead.of("bar", config);

        Callable<String> decorateCallable = Bulkhead
                .decorateCallable(bulkhead, () -> service.sayHelloWorld("Robert"));

        Try<String> firstTry = Try.ofCallable(decorateCallable);
        then(firstTry.isSuccess()).isTrue();
        Try<String> secondTry = Try.ofCallable(decorateCallable);
        then(secondTry.isFailure()).isTrue();
        then(secondTry.getCause()).isInstanceOf(BulkheadFullException.class);

    }


    public void retry(){
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");

        HelloWorldService service = null;

        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(100))
            .intervalFunction(IntervalFunction.ofExponentialBackoff())
            .retryOnException(throwable -> API.Match(throwable).of(
                Case($(instanceOf(IOException.class)), true),
                Case($(), false)))
            .build();
        Retry retry = Retry.of("backendName", config);

        Callable<String> decorateCallable = CircuitBreaker
                .decorateCallable(circuitBreaker, () -> service.sayHelloWorld("Robert"));
        decorateCallable = Retry.decorateCallable(retry, decorateCallable);
        String result = Try.ofCallable(decorateCallable)
                .getOrElse("Ich muss weg!");

    }
}
