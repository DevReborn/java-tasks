# java-tasks
A task library to make Parallelisation easier in java

Java does not (in my opinion) have a easy-to-use, flexible way of doing parallelisation. So I made my own. Loosely based off of Promises in JS and the Task library in C#

## Create a Simple Task

You can create a task using the `Tasks.Create` function:

```java
import com.reborn.tasks.Tasks;

public class TaskRunner {
    public void runATask() {
        // Basic task
        ITask task = Tasks.create(o -> {
            System.out.print("This will run on a new thread!");
        });
        task.execute();
        
        // Task with return value
        IValueTask<String> valueTask = Tasks.create(o -> {
            return "This will be returned from the task!";
        });
        valueTask.execute();
    }
}
```

The `Tasks.Create` function  returns a task that can be executed at a later point in time using the `execute()` function.

The way the task is run is defined by the `ITaskExecutor` optional parameter passed into the `execute(ITaskExecutor executor)` function. By default, if you don't pass in an executor it will run the task on a single new thread (but a different thread than the calling thread), shared by all tasks. By passing in a different `ITaskExecutor` you can define different threading behaviour for that task.

Alternatively you can set the default executor by using creating a new `ITaskFactory` and using `Tasks.setTaskFactory(ITaskFactory factory)` to set it. (you can use the base `TaskFactory` class to make it easier.

### Chaining Task Callbacks

After you have created a task you can chain on callbacks for different responses to the task.

```java
IValueTask<Result> valueTask = Tasks.create(o -> {
        return doSomeStuff();
    })
    .onExecute(result -> {
        // runs on the calling thread after you call execute(), before the main task is kicked off
        // Is useful if you create a task to use at a later date and want to guarantee some 'start' state
    })
    .onSuccess(result -> {
        // Everything was great! handle the result :D
    })
    .onError(ex -> {
        // Something went wrong! handle the exception :(
    })
    .onUpdate(update -> {
        // called when the ITaskOperator.update(Object) function is called from the main task thread.
    })
    .onComplete((result, wasSuccess) -> {
        // Always called no matter, wasSuccess will tell you if the result is in fact the result of the task, 
        // or if it errored. In which case result would be null
    });
valueTask.execute();
```

As you can see the callback functions are easy to use and understand and can be chained multiple times so you pass around a task and manipulate it for different contexts.

*Note: the `ITaskExecutor.postback(Runnable)` is what defines how all the callbacks (apart from onExecute) are ran*


## Deferred Task

For cases where you need a Promise-style task like in Javascript, there is the `IDeferredValueTask<T>` and `IDeferredTask`:
```java
IDeferredValueTask<Result> deferredValueTask = Tasks.deferredValue(t -> {
    if(done) {
        t.setSucceeded("Done!");
    } else {
        t.setErrored(new Exception("Not done :("));
    }
});

// OR

IDeferredTask deferredTask = Tasks.deferred(t -> {
    if(done) {
        t.setSucceeded();
    } else {
        t.setErrored(new Exception("Not done :("));
    }
});
```

A deferred task extends `IValueTask` and `ITask` so once its created can be treated exactly the same as any other task. The only difference is that a deferred tasks completion is set by another function calling the `setSucceeded` or `setErrored` function. This is mostly useful is you are using other libaries that using threading and need to integrate them into your task:

```java
IDeferredValueTask<Result> deferredValueTask = Tasks.deferredValue(t -> {
    otherLibrary.callApi(apiResult -> {
        t.setSucceeded(apiResult);
    }, errorMessage -> {
        t.setErrored(new Exception(errorMessage));
    });
});
```

## Running and Canceling a task

#### Cancelations
A Tasks `execute()` function returns ICancelable which can be used to set the task as `TaskState.CANCELED`.
This can be queried inside the main task with the `ITaskOperator.isCanceled()` function on the `ITaskOperator` that is passed into the task lamdba where calling `Tasks.create(ThrowingFunction<ITaskOperator, T>)`

#### Tasks are a one time thing
Each task can only be run once, this may or may not be changed in the future, i'm not sure yet :)


## Android
see [Android Tasks](https://github.com/DevReborn/android-tasks) for how to use java-tasks with android.
