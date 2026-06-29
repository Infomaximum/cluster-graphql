package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.struct.subscribe.SubscribeKey;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что движок подписок удерживает последнее значение и доставляет его
 * подписчику, подключившемуся после {@code pushEvent} (вариант B, IS-1025).
 */
class GraphQLSubscribeEngineImplTest {

    private static final UUID NODE = new UUID(1L, 2L);
    private static final int COMPONENT_ID = 7;
    private static final byte[] KEY_BYTES = {1, 2, 3};

    /**
     * Событие, отправленное до подписки, доставляется подписчику при регистрации
     * (главный сценарий гонки: до фикса событие терялось).
     */
    @Test
    void deliversEventPushedBeforeSubscribe() {
        GraphQLSubscribeEngineImpl engine = new GraphQLSubscribeEngineImpl();
        Optional<String> value = Optional.of("COMPLETED");

        engine.pushEvent(key(), value);                 // слушателя ещё нет

        RecordingEmitter emitter = new RecordingEmitter();
        engine.subscribe(NODE, COMPONENT_ID, KEY_BYTES, emitter);

        assertThat(emitter.values).containsExactly(value);
    }

    /**
     * Событие, отправленное после подписки, доставляется зарегистрированному
     * слушателю как прежде (поведение не изменилось).
     */
    @Test
    void deliversEventPushedAfterSubscribe() {
        GraphQLSubscribeEngineImpl engine = new GraphQLSubscribeEngineImpl();
        Optional<String> value = Optional.of("COMPLETED");

        RecordingEmitter emitter = new RecordingEmitter();
        engine.subscribe(NODE, COMPONENT_ID, KEY_BYTES, emitter);

        engine.pushEvent(key(), value);

        assertThat(emitter.values).containsExactly(value);
    }

    private static SubscribeKey key() {
        return new SubscribeKey(NODE, COMPONENT_ID, KEY_BYTES);
    }

    /** Эмиттер, фиксирующий полученные через {@code onNext} значения. */
    private static final class RecordingEmitter implements ObservableEmitter<Object> {

        private final List<Object> values = new ArrayList<>();

        @Override
        public void onNext(Object value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable error) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void setDisposable(Disposable d) {
        }

        @Override
        public void setCancellable(Cancellable c) {
        }

        @Override
        public boolean isDisposed() {
            return false;
        }

        @Override
        public ObservableEmitter<Object> serialize() {
            return this;
        }

        @Override
        public boolean tryOnError(Throwable t) {
            return false;
        }
    }
}
