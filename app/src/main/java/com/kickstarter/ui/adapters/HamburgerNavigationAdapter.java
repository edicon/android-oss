package com.kickstarter.ui.adapters;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;

import com.kickstarter.R;
import com.kickstarter.models.Category;
import com.kickstarter.models.Empty;
import com.kickstarter.models.HamburgerNavigationData;
import com.kickstarter.models.HamburgerNavigationItem;
import com.kickstarter.models.User;
import com.kickstarter.services.DiscoveryParams;
import com.kickstarter.ui.viewholders.EmptyViewHolder;
import com.kickstarter.ui.viewholders.HamburgerNavigationFilterViewHolder;
import com.kickstarter.ui.viewholders.HamburgerNavigationHeaderLoggedInViewHolder;
import com.kickstarter.ui.viewholders.HamburgerNavigationHeaderLoggedOutViewHolder;
import com.kickstarter.ui.viewholders.KSViewHolder;

import java.util.Collections;

public final class HamburgerNavigationAdapter extends KSAdapter {
  private final Delegate delegate;

  public interface Delegate extends HamburgerNavigationFilterViewHolder.Delegate {}

  public HamburgerNavigationAdapter(final @NonNull Delegate delegate) {
    this.delegate = delegate;
  }

  // TODO: Replace object type for filters list
  public void data(final @NonNull HamburgerNavigationData data) {
    data().clear();

    // HEADER
    data().add(Collections.singletonList(data.user() != null ? data.user() : Empty.get()));

    // TOP FILTERS (e.g. staff picks, friends backed, ...)
    data().add(data.topFilters());

    // DIVIDER
    data().add(Collections.singletonList(Empty.get()));

    // CATEGORY FILTERS
    data().add(data.categoryFilters());

    notifyDataSetChanged();
  }

  @Override
  protected int layout(final @NonNull SectionRow sectionRow) {
    final int section = sectionRow.section();
    final Object object = objectFromSectionRow(sectionRow);
    if (section == 0) {
      return (object instanceof User) ?
        R.layout.hamburger_navigation_header_logged_in_view :
        R.layout.hamburger_navigation_header_logged_out_view;
    } else if (object instanceof HamburgerNavigationItem) {
      final HamburgerNavigationItem item = (HamburgerNavigationItem) object;
      final Category category = item.discoveryParams().category();
      if (category != null) {
        return category.isRoot() ?
          R.layout.hamburger_navigation_parent_filter_view :
          R.layout.hamburger_navigation_child_filter_view;
      } else {
        return R.layout.hamburger_navigation_top_filter_view;
      }
    }
    return R.layout.hamburger_divider_view;
  }

  @Override
  protected @NonNull KSViewHolder viewHolder(final @LayoutRes int layout, final @NonNull View view) {
    switch (layout) {
      case R.layout.hamburger_navigation_header_logged_in_view:
        return new HamburgerNavigationHeaderLoggedInViewHolder(view);
      case R.layout.hamburger_navigation_header_logged_out_view:
        return new HamburgerNavigationHeaderLoggedOutViewHolder(view);
      case R.layout.hamburger_navigation_top_filter_view:
      case R.layout.hamburger_navigation_parent_filter_view: // TODO: Change to separate viewholder?
      case R.layout.hamburger_navigation_child_filter_view: // TODO: Change to separate viewholder?
        return new HamburgerNavigationFilterViewHolder(view);
      default:
        return new EmptyViewHolder(view);
    }
  }
}
