import dev.mccue.log.alpha.LoggerFactory;
import dev.mccue.log.alpha.publisher.GlobalFanOutLogger;
import dev.mccue.log.alpha.publisher.Publisher;

module dev.mccue.log.alpha.publisher {
    requires io.vavr;
    requires dev.mccue.async;
    requires transitive dev.mccue.log.alpha;

    uses Publisher;

    exports dev.mccue.log.alpha.publisher;

    provides LoggerFactory with GlobalFanOutLogger;

}