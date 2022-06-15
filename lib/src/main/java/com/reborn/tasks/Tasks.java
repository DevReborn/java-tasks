package com.reborn.tasks;

import com.reborn.tasks.common.Cancel;
import com.reborn.tasks.common.CompoundCancelable;
import com.reborn.tasks.common.ICancelable;
import com.reborn.tasks.common.ThrowingConsumer;
import com.reborn.tasks.common.ThrowingFunction;
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
    public static void setTaskFactory(final ITaskFactory factory) {
        _factorySupplier = null;
        _factory = factory;
    }

    public static ITaskFactory getTaskFactory() {
        if(_factory != null)
            return _factory;

        if(_factorySupplier != null)
            return _factory = _factorySupplier.get();

        try {
            final Class<?> looperClass = Class.forName("android.os.Looper");
        } catch (ClassNotFoundException e) {
            return _factory = new TaskFactory(TaskExecutor.getSingleton());
        }

        try {
            final Object instance = Class.forName("com.reborn.tasks.android.AndroidTaskFactory").newInstance();
            return _factory = (ITaskFactory) instance;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("android-tasks is needed to run tasks on the android platform.\r\n" +
                    "Visit https://github.com/DevReborn/android-tasks for more information", e);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Something went wrong when trying to create the TaskFactory", e);
        }
    }

    public static ITask create(final ThrowingConsumer<ITaskOperator> consumer) {
        return getTaskFactory().create(consumer);
    }
    public static <T> IValueTask<T> create(final ThrowingFunction<ITaskOperator, T> consumer) {
        return getTaskFactory().create(consumer);
    }
    public static <T> IValueTask<T> fromResult(final T result) {
        return getTaskFactory().fromResult(result);
    }

    public static <T> IDeferredValueTask<T> deferredValue(final Function<IDeferredValueTask<T>, ICancelable> function) {
        return getTaskFactory().createDeferred(function);
    }
    public static <T> IDeferredValueTask<T> deferredValue(final Consumer<IDeferredValueTask<T>> function) {
        return deferredValue(t -> {
            function.accept(t);
            return Cancel.empty;
        });
    }
    public static IDeferredTask deferred(final Function<IDeferredTask, ICancelable> function) {
        return getTaskFactory().createDeferred(function::apply);
    }
    public static IDeferredTask deferred(final Consumer<IDeferredTask> function) {
        return deferred(t -> {
            function.accept(t);
            return Cancel.empty;
        });
    }

    public static IValueTask<ITask[]> executeAll(final ITask... tasks) {
        return deferredValue(def -> {
            final CompoundCancelable cancelable = new CompoundCancelable();
            for (final ITask task : tasks) {
                final ICancelable taskCancelable = task.onComplete(() -> {
                    if (Arrays.stream(tasks).allMatch(x -> x.getState() == TaskState.SUCCEEDED)) {
                        def.setSucceeded(tasks);
                    } else if (Arrays.stream(tasks).allMatch(x ->
                            x.getState() == TaskState.SUCCEEDED || x.getState() == TaskState.ERRORED)) {

                        final List<Throwable> throwables = Arrays.stream(tasks)
                                .filter(x -> x.getState() == TaskState.ERRORED)
                                .map(ITask::getException)
                                .collect(Collectors.toList());
                        def.setErrored(new CompoundTaskException(throwables));
                    }
                }).onError(x -> true).execute();
                cancelable.add(taskCancelable);
            }
            return cancelable;
        });
    }

    public static <TIn, TOut> IValueTask<TOut> then(final IValueTask<TIn> runningTask,
                                                    final Function<TIn, TOut> converter) {
        return deferredValue(task -> {
            runningTask.onSuccess(valueIn -> task.setSucceeded(converter.apply(valueIn)))
                    .onError(e -> {
                        task.setErrored(e);
                        return true;
                    })
                    .execute();
        });
    }
}
