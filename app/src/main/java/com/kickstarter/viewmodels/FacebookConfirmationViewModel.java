package com.kickstarter.viewmodels;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.kickstarter.libs.ActivityViewModel;
import com.kickstarter.libs.CurrentConfigType;
import com.kickstarter.libs.CurrentUserType;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.rx.transformers.Transformers;
import com.kickstarter.libs.utils.I18nUtils;
import com.kickstarter.services.ApiClientType;
import com.kickstarter.services.apiresponses.AccessTokenEnvelope;
import com.kickstarter.services.apiresponses.ErrorEnvelope;
import com.kickstarter.ui.IntentKey;
import com.kickstarter.ui.activities.FacebookConfirmationActivity;
import com.kickstarter.viewmodels.errors.FacebookConfirmationViewModelErrors;
import com.kickstarter.viewmodels.inputs.FacebookConfirmationViewModelInputs;
import com.kickstarter.viewmodels.outputs.FacebookConfirmationViewModelOutputs;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class FacebookConfirmationViewModel extends ActivityViewModel<FacebookConfirmationActivity> implements
  FacebookConfirmationViewModelInputs, FacebookConfirmationViewModelOutputs, FacebookConfirmationViewModelErrors {
  private final ApiClientType client;
  private final CurrentUserType currentUser;
  private final CurrentConfigType currentConfig;

  // INPUTS
  private final PublishSubject<Void> createNewAccountClick = PublishSubject.create();
  public void createNewAccountClick() {
    createNewAccountClick.onNext(null);
  }
  private final PublishSubject<Boolean> sendNewslettersClick = PublishSubject.create();
  public void sendNewslettersClick(final boolean b) {
    sendNewslettersClick.onNext(b);
  }

  // OUTPUTS
  private final BehaviorSubject<String> prefillEmail = BehaviorSubject.create();
  public @NonNull Observable<String> prefillEmail() {
    return prefillEmail;
  }

  private final PublishSubject<Void> signupSuccess = PublishSubject.create();
  public @NonNull Observable<Void> signupSuccess() {
    return signupSuccess;
  }
  private final BehaviorSubject<Boolean> sendNewslettersIsChecked = BehaviorSubject.create();
  public @NonNull Observable<Boolean> sendNewslettersIsChecked() {
    return sendNewslettersIsChecked;
  }

  // ERRORS
  private final PublishSubject<ErrorEnvelope> signupError = PublishSubject.create();
  public Observable<String> signupError() {
    return signupError
      .takeUntil(signupSuccess)
      .map(ErrorEnvelope::errorMessage);
  }

  public final FacebookConfirmationViewModelInputs inputs = this;
  public final FacebookConfirmationViewModelOutputs outputs = this;
  public final FacebookConfirmationViewModelErrors errors = this;

  public FacebookConfirmationViewModel(final @NonNull Environment environment) {
    super(environment);

    this.client = environment.apiClient();
    this.currentConfig = environment.currentConfig();
    this.currentUser = environment.currentUser();

    final Observable<String> facebookAccessToken = intent()
      .map(i -> i.getStringExtra(IntentKey.FACEBOOK_TOKEN))
      .ofType(String.class);

    final Observable<Pair<String, Boolean>> tokenAndNewsletter = facebookAccessToken
      .compose(Transformers.combineLatestPair(sendNewslettersIsChecked));

    intent()
      .map(i -> i.getParcelableExtra(IntentKey.FACEBOOK_USER))
      .ofType(ErrorEnvelope.FacebookUser.class)
      .map(ErrorEnvelope.FacebookUser::email)
      .compose(bindToLifecycle())
      .subscribe(prefillEmail::onNext);

    tokenAndNewsletter
      .compose(Transformers.takeWhen(createNewAccountClick))
      .flatMap(tn -> createNewAccount(tn.first, tn.second))
      .compose(bindToLifecycle())
      .subscribe(this::registerWithFacebookSuccess);

    sendNewslettersClick
      .compose(bindToLifecycle())
      .subscribe(sendNewslettersIsChecked::onNext);
  }

  @Override
  protected void onCreate(final @NonNull Context context, final @Nullable Bundle savedInstanceState) {
    super.onCreate(context, savedInstanceState);

    currentConfig.observable()
      .take(1)
      .map(config -> I18nUtils.isCountryUS(config.countryCode()))
      .subscribe(sendNewslettersIsChecked::onNext);

    signupError
      .compose(bindToLifecycle())
      .subscribe(__ -> koala.trackRegisterError());

    sendNewslettersClick
      .compose(bindToLifecycle())
      .subscribe(koala::trackSignupNewsletterToggle);

    signupSuccess
      .compose(bindToLifecycle())
      .subscribe(__ -> {
        koala.trackLoginSuccess();
        koala.trackRegisterSuccess();
      });

    koala.trackFacebookConfirmation();
    koala.trackRegisterFormView();
  }

  public Observable<AccessTokenEnvelope> createNewAccount(final @NonNull String fbAccessToken, final boolean sendNewsletters) {
    return client.registerWithFacebook(fbAccessToken, sendNewsletters)
      .compose(Transformers.pipeApiErrorsTo(signupError))
      .compose(Transformers.neverError());
  }

  private void registerWithFacebookSuccess(final @NonNull AccessTokenEnvelope envelope) {
    currentUser.login(envelope.user(), envelope.accessToken());
    signupSuccess.onNext(null);
  }
}
