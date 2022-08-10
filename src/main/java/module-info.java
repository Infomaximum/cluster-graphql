module com.infomaximum.cluster.graphql {

    requires com.infomaximum.cluster;
    requires java.validation;
    requires org.slf4j;
    requires io.reactivex.rxjava2;
    requires org.reactivestreams;
    requires com.graphqljava.graphqljava;
    requires com.google.guava.guava;
    requires org.reflections.reflections;

    exports com.infomaximum.cluster.graphql.remote.graphql.executor;
    exports com.infomaximum.cluster.graphql.struct;
    exports com.infomaximum.cluster.graphql.executor.subscription;
    exports com.infomaximum.cluster.graphql.subscription;
    exports com.infomaximum.cluster.graphql;
    exports com.infomaximum.cluster.graphql.executor;
    exports com.infomaximum.cluster.graphql.schema;
    exports com.infomaximum.cluster.graphql.schema.scalartype;
    exports com.infomaximum.cluster.graphql.exception;
    exports com.infomaximum.cluster.graphql.schema.struct.out;
    exports com.infomaximum.cluster.graphql.schema.build.graphqltype;
    exports com.infomaximum.cluster.graphql.executor.component;
    exports com.infomaximum.cluster.graphql.schema.datafetcher;
    exports com.infomaximum.cluster.graphql.preparecustomfield;
    exports com.infomaximum.cluster.graphql.anotation;
    exports com.infomaximum.cluster.graphql.fieldargument.custom;

}