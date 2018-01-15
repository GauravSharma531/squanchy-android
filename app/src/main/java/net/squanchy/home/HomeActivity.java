package net.squanchy.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.transition.Fade;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.squanchy.R;
import net.squanchy.analytics.Analytics;
import net.squanchy.analytics.ContentType;
import net.squanchy.home.deeplink.HomeActivityDeepLinkCreator;
import net.squanchy.home.deeplink.HomeActivityIntentParser;
import net.squanchy.navigation.Navigator;
import net.squanchy.support.lang.Optional;
import net.squanchy.support.widget.InterceptingBottomNavigationView;

import io.reactivex.disposables.CompositeDisposable;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_SIGN_IN_MAY_GOD_HAVE_MERCY_OF_OUR_SOULS = 666;

    private final Map<BottomNavigationSection, View> pageViews = new HashMap<>(4);
    private final List<Loadable> loadables = new ArrayList<>(4);

    private int pageFadeDurationMillis;

    private BottomNavigationSection currentSection;
    private InterceptingBottomNavigationView bottomNavigationView;
    private ViewGroup pageContainer;
    private Analytics analytics;
    private Navigator navigator;

    private CompositeDisposable subscriptions;

    public static Intent createScheduleIntent(Context context, Optional<String> dayId, Optional<String> eventId) {
        return new HomeActivityDeepLinkCreator(context)
                .deepLinkTo(BottomNavigationSection.SCHEDULE)
                .withDayId(dayId)
                .withEventId(eventId)
                .build();
    }

    public static Intent createFavoritesIntent(Context context) {
        return new HomeActivityDeepLinkCreator(context)
                .deepLinkTo(BottomNavigationSection.FAVORITES)
                .build();
    }

    public static Intent createTweetsIntent(Context context) {
        return new HomeActivityDeepLinkCreator(context)
                .deepLinkTo(BottomNavigationSection.TWEETS)
                .build();
    }

    public static Intent createVenueInfoIntent(Context context) {
        return new HomeActivityDeepLinkCreator(context)
                .deepLinkTo(BottomNavigationSection.VENUE_INFO)
                .build();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        pageFadeDurationMillis = getResources().getInteger(android.R.integer.config_shortAnimTime);

        pageContainer = findViewById(R.id.page_container);
        collectPageViewsInto(pageViews);
        collectLoadablesInto(loadables);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);

        Intent intent = getIntent();
        selectPageFrom(intent, savedInstanceState);

        HomeComponent homeComponent = HomeInjectorKt.homeComponent(this);

        analytics = homeComponent.analytics();

        navigator = homeComponent.navigator();
        subscriptions = new CompositeDisposable();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        selectPageFrom(intent, null);
    }

    private void selectPageFrom(Intent intent, @Nullable Bundle savedState) {
        HomeActivityIntentParser intentParser = new HomeActivityIntentParser(savedState, intent);
        BottomNavigationSection selectedPage = intentParser.getInitialSelectedPage();
        selectInitialPage(selectedPage);
    }

    private void collectPageViewsInto(Map<BottomNavigationSection, View> pageViews) {
        pageViews.put(BottomNavigationSection.SCHEDULE, pageContainer.findViewById(R.id.schedule_content_root));
        pageViews.put(BottomNavigationSection.FAVORITES, pageContainer.findViewById(R.id.favorites_content_root));
        pageViews.put(BottomNavigationSection.TWEETS, pageContainer.findViewById(R.id.tweetsContentRoot));
        pageViews.put(BottomNavigationSection.VENUE_INFO, pageContainer.findViewById(R.id.venueContentRoot));
    }

    private void collectLoadablesInto(List<Loadable> loadables) {
        loadables.add(pageContainer.findViewById(R.id.schedule_content_root));
        loadables.add(pageContainer.findViewById(R.id.favorites_content_root));
        loadables.add(pageContainer.findViewById(R.id.tweetsContentRoot));
        loadables.add(pageContainer.findViewById(R.id.venueContentRoot));
    }

    private void setupBottomNavigation(InterceptingBottomNavigationView bottomNavigationView) {
        BottomNavigationHelper.disableShiftMode(bottomNavigationView);
        bottomNavigationView.setRevealDurationMillis(pageFadeDurationMillis);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {
                    switch (item.getItemId()) {
                        case R.id.action_schedule:
                            selectPage(BottomNavigationSection.SCHEDULE);
                            break;
                        case R.id.action_favorites:
                            selectPage(BottomNavigationSection.FAVORITES);
                            break;
                        case R.id.action_tweets:
                            selectPage(BottomNavigationSection.TWEETS);
                            break;
                        case R.id.action_venue:
                            selectPage(BottomNavigationSection.VENUE_INFO);
                            break;
                        default:
                            throw new IndexOutOfBoundsException("Unsupported navigation item ID: " + item.getItemId());
                    }
                    return true;
                }
        );
    }

    private void selectInitialPage(BottomNavigationSection section) {
        swapPageTo(section);
        bottomNavigationView.cancelTransitions();
        bottomNavigationView.selectItemAt(section.ordinal());

        Resources.Theme theme = getThemeFor(section);
        bottomNavigationView.setBackgroundColor(getColorFromTheme(theme, android.support.design.R.attr.colorPrimary));
        getWindow().setStatusBarColor(getColorFromTheme(theme, android.R.attr.statusBarColor));

        currentSection = section;
    }

    private void selectPage(BottomNavigationSection section) {
        if (section.equals(currentSection)) {
            return;
        }

        Fade transition = new Fade();
        transition.setDuration(pageFadeDurationMillis);
        TransitionManager.beginDelayedTransition(pageContainer, transition);

        swapPageTo(section);

        Resources.Theme theme = getThemeFor(section);
        animateStatusBarColorTo(getColorFromTheme(theme, android.R.attr.statusBarColor));
        bottomNavigationView.setColorProvider(() -> getColorFromTheme(theme, android.support.design.R.attr.colorPrimary));

        currentSection = section;

        trackPageSelection(section);
    }

    private void swapPageTo(BottomNavigationSection section) {
        if (currentSection != null) {
            pageViews.get(currentSection).setVisibility(View.INVISIBLE);
        }
        pageViews.get(section).setVisibility(View.VISIBLE);
    }

    private Resources.Theme getThemeFor(BottomNavigationSection section) {
        Resources.Theme theme = getResources().newTheme();
        theme.setTo(getTheme());
        theme.applyStyle(section.getTheme(), true);
        return theme;
    }

    @ColorInt
    private int getColorFromTheme(Resources.Theme theme, @AttrRes int attributeId) {
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attributeId, typedValue, true);
        return typedValue.data;
    }

    private void animateStatusBarColorTo(@ColorInt int color) {
        Window window = getWindow();
        int currentStatusBarColor = window.getStatusBarColor();

        animateColor(currentStatusBarColor, color, animation -> window.setStatusBarColor((int) animation.getAnimatedValue()));
    }

    private void animateColor(@ColorInt int currentColor, @ColorInt int targetColor, ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator animator = ValueAnimator.ofArgb(currentColor, targetColor).setDuration(pageFadeDurationMillis);
        animator.addUpdateListener(listener);
        animator.start();
    }

    private void trackPageSelection(BottomNavigationSection section) {
        analytics.trackItemSelected(ContentType.NAVIGATION_ITEM, section.name());
        analytics.trackPageView(this, section.name());
    }

    public void requestSignIn() {
        disposeAllSubscriptions();
        navigator.toSignInForResult(REQUEST_SIGN_IN_MAY_GOD_HAVE_MERCY_OF_OUR_SOULS);
    }

    @Override
    protected void onStart() {
        super.onStart();

        selectInitialPage(currentSection);

        for (Loadable loadable : loadables) {
            loadable.startLoading();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        HomeStatePersister statePersister = new HomeStatePersister();
        statePersister.saveCurrentSection(outState, currentSection);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();

        disposeAllSubscriptions();
    }

    private void disposeAllSubscriptions() {
        subscriptions.clear();

        for (Loadable loadable : loadables) {
            loadable.stopLoading();
        }
    }
}
