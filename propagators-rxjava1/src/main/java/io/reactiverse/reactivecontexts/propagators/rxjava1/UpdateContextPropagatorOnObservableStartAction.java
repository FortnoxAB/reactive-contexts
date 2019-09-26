package io.reactiverse.reactivecontexts.propagators.rxjava1;

import io.reactiverse.reactivecontexts.propagators.rxjava1.ContextPropagatorOnObservableCreateAction.ContextCapturerObservable;
import rx.Observable;
import rx.functions.Func2;

public class UpdateContextPropagatorOnObservableStartAction implements Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe> {
    @Override
    public Observable.OnSubscribe call(Observable observable, Observable.OnSubscribe onSubscribe) {
        return new ContextCapturerObservable(onSubscribe);
    }
}
