package com.reborn.tasks;

import com.reborn.tasks.common.ThrowingConsumer;
import com.reborn.tasks.exceptions.CompoundTaskException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Tasks {
    private static ITaskFactory _factory;
    private static Supplier<ITaskFactory> _factorySupplier;

    public static void setTaskFactory(final Supplier<ITaskFactory> factorySupplier) {
        _factorySupplier = factorySupplier;
        _factory = null;
    }

    public static ITaskFactory getTaskFactory() {
        if(_factory != null)
            return _factory;

        if(_factorySupplier != null)
            return _factory = _factorySupplier.get();

        try {
            final Class<?> looperClass = Class.forName("android.os.Looper");
            final Object instance = Class.forName("com.reborn.common.android.threading.AndroidTaskFactory").newInstance();
            return _factory = (ITaskFactory) instance;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            return _factory = new TaskFactory();
        }
    }

    public static ITask create(final ThrowingConsumer<ITaskOperator> consumer) {
        return getTaskFactory().create(consumer);
    }
    public static <T> IValueTask<T> fromResult(final T result) {
        return getTaskFactory().fromResult(result);
    }

    public static <T> IDeferredValueTask<T> createDeferred(final Consumer<IDeferredValueTask<T>> consumer) {
        return getTaskFactory().createDeferred(consumer);
    }

    public static IValueTask<ITask[]> executeAll(final ITask... tasks) {
        return getTaskFactory().createDeferred(def -> {
//            final CompositeCancelable composite = Cancel.composite();

            for (final ITask task : tasks) {
                task.onComplete(() -> {
                    if(Arrays.stream(tasks).allMatch(x -> x.getState() == TaskState.SUCCEEDED)) {
                        def.setSucceeded(tasks);
                    } else if(Arrays.stream(tasks).allMatch(x ->
                            x.getState() == TaskState.SUCCEEDED || x.getState() == TaskState.ERRORED)) {

                        final List<Throwable> throwables = Arrays.stream(tasks)
                                .filter(x -> x.getState() == TaskState.ERRORED)
                                .map(ITask::getException)
                                .collect(Collectors.toList());
                        def.setErrored(new CompoundTaskException(throwables));
                    }
                }).onError(x -> true).execute();
//                composite.assign(cancelable);
            }
        });
    }

    public static <TIn, TOut> IValueTask<TOut> then(final IValueTask<TIn> runningTask,
                                                    final Function<TIn, TOut> converter) {
        return getTaskFactory().createDeferred(task -> {
            runningTask
                    .onSuccess(valueIn -> task.setSucceeded(converter.apply(valueIn)))
                    .onError(e -> {
                        task.setErrored(e);
                        return true;
                    })
                    .execute();
        });
    }
}
