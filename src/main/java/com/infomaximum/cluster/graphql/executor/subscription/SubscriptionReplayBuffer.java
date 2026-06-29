package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.struct.subscribe.SubscribeKey;
import io.reactivex.ObservableEmitter;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Буфер последнего значения по ключу подписки.
 *
 * <p>Удерживает значение, отправленное через {@code pushEvent}, и проигрывает его
 * подписчику, который подключился позже. Это закрывает гонку «событие отправлено
 * раньше, чем подписчик успел зарегистрироваться». Удержания вытесняются по TTL
 * (ленивая очистка раз в N сохранений), поэтому буфер не растёт неограниченно.
 */
class SubscriptionReplayBuffer {

    /** Сколько времени удерживается последнее значение по ключу. */
    private static final long RETENTION_TTL_MILLIS = Duration.ofMinutes(1).toMillis();

    /** Периодичность ленивой очистки протухших удержаний — раз в N сохранений. */
    private static final int SWEEP_EVERY_N_PUTS = 100;

    private final ConcurrentMap<SubscribeKey, Retained> lastValue = new ConcurrentHashMap<>();

    private final AtomicInteger putCounter = new AtomicInteger();

    /**
     * Сохраняет значение по ключу для последующего реплея позднему подписчику и
     * периодически вытесняет протухшие удержания.
     *
     * @param subscribeKey ключ подписки
     * @param value сохраняемое значение
     */
    void retain(SubscribeKey subscribeKey, Optional<? extends Serializable> value) {
        lastValue.put(subscribeKey, new Retained(value.orElse(null), System.currentTimeMillis()));
        if (putCounter.incrementAndGet() % SWEEP_EVERY_N_PUTS == 0) {
            purgeExpired();
        }
    }

    /**
     * Проигрывает удержанное по ключу значение подписчику, если оно есть и не протухло.
     *
     * @param subscribeKey ключ подписки
     * @param observable подписчик, которому проигрывается значение
     */
    void replayTo(SubscribeKey subscribeKey, ObservableEmitter observable) {
        Retained retained = lastValue.get(subscribeKey);
        if (retained != null && !isExpired(retained.timestampMillis(), System.currentTimeMillis())) {
            observable.onNext(Optional.ofNullable(retained.value()));
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        lastValue.values().removeIf(retained -> isExpired(retained.timestampMillis(), now));
    }

    /** Истёк ли срок удержания значения, сохранённого в {@code timestampMillis}, к моменту {@code nowMillis}. */
    static boolean isExpired(long timestampMillis, long nowMillis) {
        return nowMillis - timestampMillis >= RETENTION_TTL_MILLIS;
    }

    /** Удержанное значение (может быть {@code null} — пустой результат) с моментом сохранения. */
    private record Retained(Serializable value, long timestampMillis) {
    }
}
