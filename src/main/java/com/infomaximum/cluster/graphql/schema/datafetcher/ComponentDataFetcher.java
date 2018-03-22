package com.infomaximum.cluster.graphql.schema.datafetcher;

import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomFieldUtils;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.datafetcher.utils.ExtResult;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.struct.GRequest;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.VariableReference;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by kris on 11.01.17.
 */
public class ComponentDataFetcher implements DataFetcher {

    protected final static Logger log = LoggerFactory.getLogger(ComponentDataFetcher.class);

    protected final Remotes remotes;
    protected final GraphQLComponentExecutor sdkGraphQLItemExecutor;

    protected final String graphQLTypeName;
    protected final RGraphQLObjectTypeField rTypeGraphQLField;

    public ComponentDataFetcher(Remotes remotes, GraphQLComponentExecutor sdkGraphQLItemExecutor, String graphQLTypeName, RGraphQLObjectTypeField rTypeGraphQLField) {
        this.remotes = remotes;
        this.sdkGraphQLItemExecutor = sdkGraphQLItemExecutor;

        this.graphQLTypeName = graphQLTypeName;
        this.rTypeGraphQLField = rTypeGraphQLField;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        GRequest gRequest = environment.getContext();

        try {
            if (rTypeGraphQLField.componentUuid==null) {
                //У этого объекта нет родительской подсистемы - вызываем прямо тут
                Object result = sdkGraphQLItemExecutor.execute(
                        gRequest,
                        PrepareCustomFieldUtils.uniqueFieldKey(gRequest, environment),
                        environment.getSource(), graphQLTypeName, rTypeGraphQLField.name,
                        getArguments(rTypeGraphQLField, environment, gRequest.getExternalVariables()),
                        false
                );
                return ExtResult.get(result);
            } else {
                //Этот объект принадлежит определенной подсистеме - необходимо вызывать метод удаленно именно не родительской подсистеме
                RControllerGraphQL rControllerGraphQL = remotes.getFromSSUuid(rTypeGraphQLField.componentUuid, RControllerGraphQL.class);
                Object result = rControllerGraphQL.execute(
                        gRequest,
                        PrepareCustomFieldUtils.uniqueFieldKey(gRequest, environment),
                        environment.getSource(), graphQLTypeName, rTypeGraphQLField.name,
                        getArguments(rTypeGraphQLField, environment, gRequest.getExternalVariables())
                );
                return ExtResult.get(result);
            }
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
    protected static HashMap<String, Serializable> getArguments(RGraphQLObjectTypeField rTypeGraphQLField, DataFetchingEnvironment environment, HashMap<String, Serializable> externalVariables) {
        Field field=null;
        for (Field iField: environment.getFields()) {
            if (iField.getName().equals(rTypeGraphQLField.externalName)) {
                field=iField;
                break;
            }
        }
        if (field==null || field.getArguments().isEmpty()) return new HashMap<>();

        return filterArguments(field, environment.getArguments(), externalVariables.keySet());
    }

    /** Фильтруем из запроса пришедшие аргументы */
    public static HashMap<String, Serializable> filterArguments(Field field, Map<String, Object> arguments, Set<String> externalNameVariables) {
        HashMap<String, Serializable> result = new HashMap<String, Serializable>();
        for (Map.Entry<String, Object> entry: arguments.entrySet()) {
            Argument argument = getArgument(field, entry.getKey());
            if (argument.getValue() instanceof VariableReference) {
                //Проверим хитрую ситуацию, если аргумент в методе был зарезервирован под переменную из variables
                //но этот variable не был передан, то считаем, что этот агрумент и не собирались отправлять
                VariableReference variableReference = (VariableReference) argument.getValue();
                if (!externalNameVariables.contains(variableReference.getName())) continue;
            }


            result.put(entry.getKey(), (Serializable) entry.getValue());
        }

        //TODO Необходима предварительная конвертация скаляров!!!
//        fieldArgumentConverter

        return result;
    }

    private static Argument getArgument(Field field, String name) {
        for (Argument argument: field.getArguments()) {
            if (argument.getName().equals(name)) return argument;
        }
        throw new RuntimeException();//Такого быть не должно в принципе
    }

}
