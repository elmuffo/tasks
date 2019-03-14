package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class ActionViewModel extends ViewModel {
  private final MutableLiveData<Boolean> completed = new MutableLiveData<>();
  private final MutableLiveData<Throwable> error = new MutableLiveData<>();
  private final CompositeDisposable disposables = new CompositeDisposable();
  private boolean inProgress;

  public LiveData<Boolean> getData() {
    return completed;
  }

  public LiveData<Throwable> getError() {
    return error;
  }

  public boolean inProgress() {
    return inProgress;
  }

  protected void run(Action action) {
    assertMainThread();

    if (!inProgress) {
      inProgress = true;
      disposables.add(
          Completable.fromAction(action)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .doFinally(
                  () -> {
                    assertMainThread();
                    inProgress = false;
                  })
              .subscribe(() -> completed.setValue(true), error::setValue));
    }
  }

  @Override
  protected void onCleared() {
    disposables.clear();
  }
}
