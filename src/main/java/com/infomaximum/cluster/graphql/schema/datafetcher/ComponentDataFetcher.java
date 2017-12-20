package com.infomaximum.cluster.graphql.schema.datafetcher;

import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.datafetcher.utils.ExtResult;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GRequestItem;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.VariableReference;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by kris on 11.01.17.
 */
public class ComponentDataFetcher implements DataFetcher {

    protected final static Logger log = LoggerFactory.getLogger(ComponentDataFetcher.class);

    protected final Remotes remotes;

    protected final String graphQLTypeName;
    protected final RGraphQLObjectTypeField rTypeGraphQLField;

    public ComponentDataFetcher(Remotes remotes, String graphQLTypeName, RGraphQLObjectTypeField rTypeGraphQLField) {
        this.remotes = remotes;

        this.graphQLTypeName = graphQLTypeName;
        this.rTypeGraphQLField = rTypeGraphQLField;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        GRequest gRequest = environment.getContext();

        try {
            GRequestItem gRequestItem = new GRequestItem(
                    environment.getSource(),
                    getReceivedArguments(rTypeGraphQLField, environment, gRequest.getExternalNameVariables())
            );

            RControllerGraphQL rControllerGraphQL = remotes.getFromSSUuid(rTypeGraphQLField.subsystem, RControllerGraphQL.class);
            Object result = rControllerGraphQL.execute(gRequest, gRequestItem, graphQLTypeName, rTypeGraphQLField.name, environment.getArguments());
            return ExtResult.get(result);
        } catch (Throwable t) {
            Throwable e;
            if (t instanceof InvocationTargetException) {
                e = ((InvocationTargetException) t).getTargetException();
            } else if (t instanceof ExecutionException) {
                e = t.getCause();
            } else {
                e = t;
            }
            throw new RuntimeException(e);
        }
    }

    /** Вытаскиваем из запроса пришедшие аргументы */
    protected static Set<String> getReceivedArguments(RGraphQLObjectTypeField rTypeGraphQLField, DataFetchingEnvironment environment, Set<String> variables) {
        Field field=null;
        for (Field iField: environment.getFields()) {
            if (iField.getName().equals(rTypeGraphQLField.externalName)) {
                field=iField;
                break;
            }
        }
        if (field==null || field.getArguments().isEmpty()) return Collections.emptySet();

        Set<String> receivedArguments = new HashSet<String>();
        for (Argument argument: field.getArguments()) {
            if (argument.getValue() instanceof VariableReference) {
                //Проверим хитрую ситуацию, если аргумент в методе был зарезервирован под переменную из variables
                //но этот variable не был передан, то считаем, что этот агрумент и не собирались отправлять
                VariableReference variableReference = (VariableReference) argument.getValue();
                if (!variables.contains(variableReference.getName())) continue;
            }

            receivedArguments.add(argument.getName());
        }

        return receivedArguments;
    }


}
