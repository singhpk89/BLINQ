package com.blinq.sorters;

import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.InstagramPost;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.TwitterPost;
import com.blinq.utils.Log;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Sort a list of social window posts according to the publish time property . An implementation to the
 * <code>Comparator</code> is done to decide the comparison criteria.
 * The original sorting algorithm is described here: https://quip.com/kYiKAAbl31hR
 *
 * Main entry is sort
 */
public class SocialWindowSorter {

    private static final String TAG = SocialWindowSorter.class.getSimpleName();
    private Date now;
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
    public static final long TWENTY_DAYS = 20L * 24 * 60 * 60 * 1000;
    Date TWENTY_DAYS_AGO;
    Date ONE_DAY_AGO;

    List<SocialWindowPost> facebookPosts;
    List<SocialWindowPost> facebookEvents;
    List<SocialWindowPost> twitterPosts;
    List<SocialWindowPost> instagramPosts;
    List<SocialWindowPost> facebookLikes;
    List<SocialWindowPost> facebookShares;

    public List<SocialWindowPost> sort(List<SocialWindowPost> fbPosts, List<SocialWindowPost> fbEvents,
                                       List<SocialWindowPost> twPosts, List<SocialWindowPost> instPosts) {
        try {
            now = new Date();
            TWENTY_DAYS_AGO = new Date(now.getTime() - TWENTY_DAYS);
            ONE_DAY_AGO = new Date(now.getTime() - ONE_DAY);

            this.facebookPosts = filterOutOldPosts(fbPosts);
            this.facebookEvents = filterOutOldPosts(fbEvents);
            this.twitterPosts = filterOutOldPosts(twPosts);
            this.instagramPosts = filterOutOldPosts(instPosts);
            this.facebookLikes = filter(facebookPosts, PostTypeTag.LIKES_LINK);
            this.facebookShares = filter(facebookPosts, PostTypeTag.SHARED_IMAGE_VIDEO);
            this.facebookPosts.removeAll(facebookLikes);
            this.facebookPosts.removeAll(facebookShares);

            List<SocialWindowPost> result = new ArrayList<SocialWindowPost>();
            SocialWindowPost post = getMostRecent_Face_Ev_Inst_Tw(facebookPosts, facebookEvents, instagramPosts, twitterPosts);
            if (post != null) result.add(post);
            post = getMostPopular_Tw_Inst_Face(facebookPosts, twitterPosts, instagramPosts);
            if (post != null) result.add(post);
            post = getMostPopular_Face_Inst_Tw(facebookPosts, twitterPosts, instagramPosts);
            if (post != null) result.add(post);
            post = getMostRecent_Tw_Ev_Face_Share(twitterPosts, facebookEvents, facebookPosts, facebookShares);
            if (post != null) result.add(post);
            post = getMostRecent_Inst_Face_Share(instagramPosts, facebookPosts, facebookShares);
            if (post != null) result.add(post);
            if (!facebookLikes.isEmpty())
                result.add(getMostRecent(facebookLikes));
            post = getMostRecent_Ev_Face_Share_Tw(facebookEvents, twitterPosts, facebookPosts, facebookShares);
            if (post != null) result.add(post);
            post = getMostRecent_Tw_Share_Face(facebookPosts, twitterPosts, instagramPosts);
            if (post != null) result.add(post);
            if (!facebookLikes.isEmpty())
                result.add(getMostRecent(facebookLikes));
            post = getMostPopular_Inst_Face_Tw(facebookPosts, twitterPosts, instagramPosts);
            if (post != null) result.add(post);

            return result;

        } catch (IllegalArgumentException e) {
            Crashlytics.logException(e);
            Log.e(TAG, "Sorting failure: " + e.getMessage());
        }
        return null;
    }

    private List<SocialWindowPost> filterOutOldPosts(List<SocialWindowPost> list) {
        List<SocialWindowPost> result = new ArrayList<SocialWindowPost>();
        for (SocialWindowPost post : list) {
            if (post.getPublishTime().after(TWENTY_DAYS_AGO)) {
                result.add(post);
            }
        }
        return result;
    }

    private List<SocialWindowPost> filter(List<SocialWindowPost> list, PostTypeTag tag) {
        List<SocialWindowPost> result = new ArrayList<SocialWindowPost>();
        for (SocialWindowPost post : list) {
            if (post.getTag() == tag) {
                result.add(post);
            }
        }
        return result;
    }

    private SocialWindowPost getMostRecent(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByRecentPost());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.sort(list, sortByRecentPost());
        SocialWindowPost result = list.get(0);
        for (List<SocialWindowPost> input : args) {
            if (input.contains(result)) {
                input.remove(result);
                break;
            }
        }
        return result;
    }

    private SocialWindowPost getMostRecent_Face_Ev_Inst_Tw(List<SocialWindowPost>... args) {
        List<SocialWindowPost> recentPosts = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> arg : args) {
            if (arg.isEmpty()) continue;
            Collections.sort(arg, sortByRecentPost());
            recentPosts.add(arg.get(0));
        }
        if (recentPosts.isEmpty()) {
            return null;
        }

        Collections.sort(recentPosts, sortByRecentPost());

        // Prioritize if less then 24 hours
        for (SocialWindowPost post : recentPosts) {
            if (post.getPublishTime().after(ONE_DAY_AGO) && post.getPublishTime().before(now)) {
                removeFromPosts(post);
                return post;
            }
        }

        // If delta is more then 1d - show recent
        SocialWindowPost lastPost = recentPosts.get(0);
        Date lastPostDate = lastPost.getPublishTime();
        for (SocialWindowPost post : recentPosts) {
            if(post.getPublishTime().getTime() - lastPostDate.getTime() > ONE_DAY) {
                lastPostDate = post.getPublishTime();
                continue;
            }
            removeFromPosts(lastPost);
            return lastPost;
        }

        // Prioritize facebook -> facebook events -> instagram -> tweets
        for (SocialWindowPost post : recentPosts) {
            if (facebookPosts.contains(post)) {
                facebookPosts.remove(post);
                return post;
            }
        }
        for (SocialWindowPost post : recentPosts) {
            if (post.getTag() == PostTypeTag.EVENT) {
                facebookEvents.remove(post);
                return post;
            }
        }
        for (SocialWindowPost post : recentPosts) {
            if (post instanceof InstagramPost) {
                instagramPosts.remove(post);
                return post;
            }
        }
        for (SocialWindowPost post : recentPosts) {
            if (post instanceof TwitterPost) {
                twitterPosts.remove(post);
                return post;
            }
        }
        return null;
    }

    /**
     * Try to remove the given posts from any list if exists
     */
    private void removeFromPosts(SocialWindowPost post) {
        facebookPosts.remove(post);
        facebookEvents.remove(post);
        twitterPosts.remove(post);
        instagramPosts.remove(post);
        facebookLikes.remove(post);
        facebookShares.remove(post);
    }

    private SocialWindowPost getMostPopular_Tw_Inst_Face(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByPopular());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : list) {
            if (post instanceof TwitterPost && post.getTag() != PostTypeTag.RETWEET) {
                result = post;
                twitterPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof InstagramPost) {
                result = post;
                instagramPosts.remove(post);
                return result;
            }
        }
        if (facebookPosts.isEmpty()) {
            return null;
        }
        result = facebookPosts.get(0);
        facebookPosts.remove(result);
        return result;
    }

    private SocialWindowPost getMostPopular_Face_Inst_Tw(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByPopular());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result = null;
        for (SocialWindowPost post : list) {
            if (post instanceof FacebookPost) {
                result = post;
                facebookPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof InstagramPost) {
                result = post;
                instagramPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof TwitterPost && post.getTag() != PostTypeTag.RETWEET) {
                result = post;
                twitterPosts.remove(post);
                return result;
            }
        }
        return null;
    }

    private SocialWindowPost getMostRecent_Tw_Ev_Face_Share(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByRecentPost());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : twitterPosts) {
            if (post instanceof TwitterPost) {
                result = post;
                twitterPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post.getTag() == PostTypeTag.EVENT) {
                result = post;
                facebookEvents.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (facebookPosts.contains(post)) {
                result = post;
                facebookPosts.remove(post);
                return result;
            }
        }
        if (facebookShares.isEmpty()) {
            return null;
        }
        result = facebookShares.get(0);
        facebookShares.remove(result);
        return result;
    }

    private SocialWindowPost getMostRecent_Inst_Face_Share(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByRecentPost());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : list) {
            if (post instanceof InstagramPost) {
                result = post;
                instagramPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (facebookPosts.contains(post)) {
                result = post;
                facebookPosts.remove(post);
                return result;
            }
        }
        if (facebookShares.isEmpty()) {
            return null;
        }
        result = facebookShares.get(0);
        facebookShares.remove(result);
        return result;
    }

    private SocialWindowPost getMostRecent_Ev_Face_Share_Tw(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByRecentPost());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : list) {
            if (facebookEvents.contains(post)) {
                result = post;
                facebookEvents.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (facebookPosts.contains(post)) {
                result = post;
                facebookPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (facebookShares.contains(post)) {
                result = post;
                facebookShares.remove(post);
                return result;
            }
        }
        if (twitterPosts.isEmpty()) {
            return null;
        }
        result = twitterPosts.get(0);
        twitterPosts.remove(result);
        return result;
    }

    private SocialWindowPost getMostRecent_Tw_Share_Face(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByRecentPost());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : list) {
            if (post instanceof TwitterPost) {
                result = post;
                twitterPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof FacebookPost) {
                result = post;
                facebookPosts.remove(post);
                return result;
            }
        }
        if (instagramPosts.isEmpty()) {
            return null;
        }
        result = instagramPosts.get(0);
        instagramPosts.remove(result);
        return result;
    }

    private SocialWindowPost getMostPopular_Inst_Face_Tw(List<SocialWindowPost>... args) {
        List<SocialWindowPost> list = new ArrayList<SocialWindowPost>();
        for (List<SocialWindowPost> input : args) {
            if (input.isEmpty()) continue;
            Collections.sort(input, sortByPopular());
            list.add(input.get(0));
        }
        if (list.isEmpty()) {
            return null;
        }
        SocialWindowPost result;
        for (SocialWindowPost post : list) {
            if (post instanceof InstagramPost) {
                result = post;
                instagramPosts.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof FacebookPost) {
                result = post;
                facebookLikes.remove(post);
                return result;
            }
        }
        for (SocialWindowPost post : list) {
            if (post instanceof TwitterPost && post.getTag() != PostTypeTag.RETWEET) {
                result = post;
                twitterPosts.remove(post);
                return result;
            }
        }
        return null;
    }


    /**
     * Sort a list of posts according to the publish time property.
     * Top items should be posts
     */
    private Comparator<SocialWindowPost> sortByRecentPost() {
        return new Comparator<SocialWindowPost>() {

            @Override
            public int compare(SocialWindowPost post1, SocialWindowPost post2) {
                return post2.getPublishTime().compareTo(post1.getPublishTime());
            }
        };
    }

    private int getFacebookPopularity(SocialWindowPost p) {
        FacebookPost post = (FacebookPost) p;
        return post.getLikesCount() + post.getCommentsCount();
    }

    private int getInstagramPopularity(SocialWindowPost p) {
        InstagramPost post = (InstagramPost) p;
        return post.getLikesCount() + post.getCommentsCount();
    }

    private int getTwitterPopularity(SocialWindowPost p) {
        TwitterPost post = (TwitterPost) p;
        return post.getFavoritesCount() + post.getRetweetsCount();
    }

    /**
     * Sort a list of posts according to the most popular post
     * If there is an earlier post that is more then twice then it wins
     */
    private Comparator<SocialWindowPost> sortByPopular() {
        return new Comparator<SocialWindowPost>() {

            @Override
            public int compare(SocialWindowPost post1, SocialWindowPost post2) {

                int post1Popularity = 0;
                int post2Popularity = 0;

                if (post1 instanceof FacebookPost) {
                    post1Popularity = getFacebookPopularity(post1);
                }
                if (post2 instanceof FacebookPost) {
                    post2Popularity = getFacebookPopularity(post2);
                }
                if (post1 instanceof InstagramPost) {
                    post1Popularity = getInstagramPopularity(post1);
                }
                if (post2 instanceof InstagramPost) {
                    post2Popularity = getInstagramPopularity(post2);
                }
                if (post1 instanceof TwitterPost) {
                    post1Popularity = getTwitterPopularity(post1);
                }
                if (post2 instanceof TwitterPost) {
                    post2Popularity = getTwitterPopularity(post2);
                }

                if (post1.getTag() == PostTypeTag.RETWEET && post2.getTag() != PostTypeTag.RETWEET) {
                    return 1;
                }
                if (post2.getTag() == PostTypeTag.RETWEET && post1.getTag() != PostTypeTag.RETWEET) {
                    return -1;
                }

                return Integer.valueOf(post2Popularity).compareTo(post1Popularity);
            }
        };
    }

    public void sortOnlyByRecentLocation(List<SocialWindowPost> list) {
        try {
            Collections.sort(list, sortByRecentLocations());
        }catch(IllegalArgumentException e) {
            Crashlytics.logException(e);
            Log.e(TAG, "Sorting by recent location failure" + e.getMessage());
        }
    }

    private Comparator<SocialWindowPost> sortByRecentLocations() {
        return new Comparator<SocialWindowPost>() {

            @Override
            public int compare(SocialWindowPost post1, SocialWindowPost post2) {
                //Likes are pushed last
                if (post1.getTag() == PostTypeTag.LIKES_A &&
                        post2.getTag() == PostTypeTag.LIKES_A) {
                    return post2.getPublishTime().compareTo(post1.getPublishTime());
                }

                //Likes and retweets are pushed last
                if (post1.getTag() == PostTypeTag.LIKES_A || post1.getTag() == PostTypeTag.RETWEET) {
                    return 1;
                }
                if (post2.getTag() == PostTypeTag.LIKES_A || post2.getTag() == PostTypeTag.RETWEET) {
                    return -1;
                }
                if (post1.getLocation() != null && post2.getLocation() != null) {
                    //Push event from the past before future
                    if ((post1 instanceof FacebookPost &&
                            ((FacebookPost)post1).isEvent() &&
                            ((FacebookPost)post1).isEventInFuture()) &&
                            post2 instanceof FacebookPost &&
                            ((FacebookPost)post2).isEvent() &&
                            ((FacebookPost)post2).isEventInFuture()) {
                        //If both are future events - compare the one that is more closer to now (reverse compare)
                        return post1.getPublishTime().compareTo(post2.getPublishTime());
                    }
                    if ((post1 instanceof FacebookPost &&
                            ((FacebookPost)post1).isEvent() &&
                            ((FacebookPost)post1).isEventInFuture()))
                        return 1;

                    if ((post2 instanceof FacebookPost &&
                            ((FacebookPost)post2).isEvent() &&
                            ((FacebookPost)post2).isEventInFuture()))
                        return -1;

                    //Both location are Sort by -
                    return post2.getPublishTime().compareTo(post1.getPublishTime());
                }
                if (post1.getLocation() == null && post2.getLocation() == null) {
                    return post2.getPublishTime().compareTo(post1.getPublishTime());
                }
                if (post1.getLocation() == null) {
                    return 1;
                }
                if (post2.getLocation() == null) {
                    return -1;
                }
                return post2.getPublishTime().compareTo(post1.getPublishTime());
            }
        };
    }
}


